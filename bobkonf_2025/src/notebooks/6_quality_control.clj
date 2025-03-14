^:kindly/hide-code
(ns notebooks.6-quality-control
  (:require
   [clojure.data :as data]
   [clojure.set :as set]
   [clojure.string :as str]
   [java-time.api :as jt]
   [notebooks.4-explore-and-understand :as explore]
   [notebooks.5-transform :as transform]
   [scicloj.tableplot.v1.plotly :as plotly]
   [scicloj.tcutils.api :as tcu]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]))

;; # Quality control

;; ## Bonus: Data quality analysis and cleaning

;; In any real-world dataset, there are always data quality issues that we should address before proceeding with an analysis. If we don't, we risk drawing incorrect conclusions because the source data may be unreliable or unrealistic. We need to expose any issues like this ahead of time so that we can correctly interpret our results.

;; In this case we have some very helpful advice in the dataset description pointing out several things we should look for, including:
;;
;; 1. Gaps in the data from technical issues (battery failures, defective equipment, transmission issues, etc.)
;; 2. Different station start dates
;; 3. Environmental factors affecting measurement
;; 4. Infrastructure changes affecting traffic (detours, construction, network updates)
;; 5. Mis-measurement due to incorrect bike lane usage (cyclists travelling in the wrong direction)

;; We'll explore and fix these data quality issues so that we can conduct an accurate analysis.

;; ## Missing and inconsistent data

;; First we'll calculate the uptime percentage for each station. To do this we'll
;;
;; 1. Look at the location metadata dataset to determine when a station came online
;; 2. Determine how many measurements it _should_ have, if it had been working 24/7
;; 3. Examine how many measurements we actually have, and compare

;; Then we'll flag suspicious values by checking for ones that appear significantly higher or lower than the mean.

;; Looking at our entire dataset together, we can start by grouping by station ID, since many of quality metrics we care about will be most relevant at a station level. Note that tablecloth knows how to handle a gzipped file, there is no extra processing needed.

(def combined-dataset (tc/dataset transform/combined-dataset-file-name
                                  {:key-fn keyword
                                   :parser-fn {:datetime [:local-date-time "yyyy-MM-dd'T'HH:mm"]}}))

(def grouped-by-station-id (tc/group-by combined-dataset :station-id))

grouped-by-station-id

;; There are 38 groups, but only 35 rows in our location metadata sheet:

(def location-info-ds
  (tc/dataset explore/location-info-file-name {:key-fn keyword
                                               :parser-fn {:installed [:local-date-time "yyyy-MM-dd'T'HH:mm"]}}))

(tc/row-count location-info-ds)

;; First, we should reconcile this. Clojure has some useful built-in helpers. `clojure.data/diff` returns a tuple of [things-only-in-a things-only-in-b things-in-both] which we can use to spot the discrepancy:

(data/diff (set (:name grouped-by-station-id)) (set (:station-id location-info-ds)))

;; Here we can easily spot the problem: it appears that there are two stations that probably got mis-named. `"17-SK-BRE-O"` and `"17-SK-BRE-W"` are in the location-info dataset, but do not appear at all in the timeseries data. However, in the timeseries we do have these five station ids that are not in the location-info dataset:

#{"17-SZ-BRE-W" "02-MI-AL-W" "03-SP-NO-W" "03-SP-NO-O" "17-SZ-BRE-O"}

;; It seems likely that `"17-SZ-BRE-W"` and `"17-SZ-BRE-O"` are meant to be `"17-SK-BRE-W"` and `"17-SK-BRE-O"`. We could be more sure by inspecting the first row of the raw datasets we had, which included the installation date in the same cell as the station id:

(->> explore/raw-datasets
     (map tc/column-names)
     (map (partial filter #(str/includes? % "17"))))

;; Comparing these to the rows from the location-info dataset, we can see the installation dates are the same:

(-> location-info-ds
    (tc/select-rows (comp #{"17-SK-BRE-O" "17-SK-BRE-W"} :station-id)))

;; So it's safe to conclude that these two sets of IDs are actually the same.
;;
;; There are still 3 other station ids in our timeseries data that do not appear in the location info dataset: `"02-MI-AL-W"`, `"03-SP-NO-W"`, and `"03-SP-NO-O"`. We can check if there's anything similar there:

(-> location-info-ds
    (tc/select-rows #(re-find #"MI-AL|SP-NO" (:station-id %))))

;; We can check again how these compare to the raw data to see if we can infer anything about what may be going on here:

(defn- show-column-names-with-substring [datasets val]
  (->> datasets
       (map tc/column-names)
       (map (partial filter #(str/includes? % val)))))

(show-column-names-with-substring explore/raw-datasets "MI-AL")
(show-column-names-with-substring explore/raw-datasets "SP-NO")

;; Again we can see this is most likely a case of station ids changing over time. The original column headers reveal that the stations are most likely the same ones, but have different IDs in different years. We'll  update this in our combined dataset. To review, we have this mapping of station-ids present in the raw data to station-ids in the location info. We will use the location info ones as the canonical ones:

(def station-id-mapping
  {"17-SZ-BRE-W" "17-SK-BRE-W"
   "17-SZ-BRE-O" "17-SK-BRE-O"
   "02-MI-AL-W" "01-MI-AL-W"
   "03-SP-NO-W" "16-SP-NO-W"
   "03-SP-NO-O" "16-SP-NO-O"})

;; We'll apply this mapping to our `station-id` column, then verify that it worked. We can see that after the update we have the exact same collection of unique station-ids in our timeseries data as we do in the location-info metadata we were given.

(defn correct-station-ids [ds]
  (tc/update-columns ds :station-id (partial replace station-id-mapping)))

(def corrected-station-ids
  (correct-station-ids combined-dataset))

(set/difference (-> corrected-station-ids (tc/group-by :station-id) :name set)
                (set (:station-id location-info-ds)))

;; ## Station reliability

;; Now we'll figure out each station's reliability. This is where we can really see the power of tablecloth's magic handling of grouped datasets. We can just use all of the regular tablecloth functions and they will magically work on the grouped datasets.
;;
;; First we get the latest measurement time. Also double check it's the same for every station:

(-> corrected-station-ids
    (tc/group-by [:station-id])
    (tc/aggregate {:latest-date #(apply jt/max (:datetime %))})
    :latest-date
    distinct)

;; Yes, all stations have their last measurement at 2024-12-31T23:00, so this is what we'll use at the expected last measurement value:

(def latest
  (apply jt/max (:datetime corrected-station-ids)))

latest

;; Then get the total number of measurements for each station:
(defn- add-location-info-and-station-count [ds location-info]
  (-> ds
      (tc/group-by [:station-id])
      ;; Count the total rows minus the header row per grouped dataset
      (tc/aggregate {:total-measurement-count tc/row-count})
      ;; Then join this with the location-info dataset
      (tc/inner-join location-info :station-id)))

(defn- add-uptime-col [ds end-time]
  (-> ds
      (tc/map-columns :expected-measurement-count
                      [:installed]
                      (fn [installation-date]
                        (jt/time-between installation-date end-time :hours)))
      (tc/map-columns :uptime
                      [:total-measurement-count :expected-measurement-count]
                      (fn [total expected]
                        (-> total
                            (/ expected)
                            (* 100.0))))))

(-> corrected-station-ids
    (add-location-info-and-station-count location-info-ds)
    (add-uptime-col latest))

;; These look mostly good. We can see 2 have an uptime of more than 100%. The most likely cause, and something we want to check for anyway, is duplicate measurements. In our combined dataset, there should only ever be one count per station/time combination. We can double check this:

;; Count all rows:
(tc/row-count corrected-station-ids)

;; Count unique rows by date/station-id combination:
(-> corrected-station-ids
    (tc/unique-by [:datetime :station-id])
    tc/row-count)

;; So there are some dupes. We'll find them and see what we're dealing with:

(comment
  ;; Note, this is how you could do this using tablecloth's grouping and aggregating functionality, but `tcutils` (which is included in noj) includes a helper for finding duplicates that is more performant, which we'll use here.
  (def duplicate-rows-by-date-station-id
    (-> corrected-station-ids
        (tc/group-by [:datetime :station-id])
        (tc/aggregate {:row-count tc/row-count})
        (tc/select-rows (comp (partial < 1) :row-count)))))

;; Get the combinations of date/station-id that have more than one count:

(def duplicate-rows (tcu/duplicate-rows corrected-station-ids))

;; We can see they're all for a single station, which is great:

(-> duplicate-rows
    :station-id
    distinct)

;; And since these rows are exact duplicates, we can just do a very naive de-dedupe on our data. From here we have a clean dat, then we'll add back our uptime stats and inspect:

(defn dedupe [ds]
  (tc/unique-by ds [:datetime :station-id]))

(def clean-dataset
  (-> corrected-station-ids
      dedupe))

(-> clean-dataset
    (add-location-info-and-station-count location-info-ds)
    (add-uptime-col latest))

;; And now the data looks better. We don't have any uptimes above 100%.
;;
;; We'll do one last check, looking for suspicious values. Like counts that are not whole numbers:

(-> clean-dataset
    (tc/update-columns :count (partial map #(mod % 1)))
    :count
    distinct)

;; Here we're good. With our clean dataset, we can add some visualizations to make it easier to assess our data quality at a glance.

(-> clean-dataset
    (add-location-info-and-station-count location-info-ds)
    (add-uptime-col latest)
    (tc/order-by :uptime)
    (plotly/base {:=width 600
                  :=title "Uptime by station"})
    (plotly/layer-bar {:=x :uptime
                       :=y :station-id})
    plotly/plot
    (assoc-in [:data 0 :orientation] "h"))

;; So we'll store the results of this cleaned up data and use this in our analysis going forward:

(def clean-dataset-file-name "data/prepared/cleaned-dataset.csv.gz")

(let [csv-file-name "data/prepared/cleaned-dataset.csv"]
  (-> corrected-station-ids
      (tc/unique-by [:datetime :station-id])
      (tc/write-csv! csv-file-name))
  (transform/compress-file csv-file-name clean-dataset-file-name))

;; All of this illustrates the importance of defining and using consistent IDs as a data publisher. Failing to do this makes it very hard for downstream consumers of your data to have confidence that they are interpreting it correctly.
