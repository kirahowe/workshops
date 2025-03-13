(ns notebooks.quality-control
  (:require
   [clojure.data :as data]
   [clojure.set :as set]
   [clojure.string :as str]
   [java-time.api :as jt]
   [tablecloth.api :as tc]
   [tech.v3.libs.fastexcel :as xlsx]
   [utils.dates :as dates]))

;; # Quality control

;; ## Bonus: Data quality analysis and cleaning

(def file-name "data/prepared/raw-data.xlsx")
(def datasets
  (let [is-1904-system? (dates/is-1904-system? file-name)]
    (xlsx/workbook->datasets "data/prepared/raw-data.xlsx"
                             {:parser-fn {"Zählstelle        Inbetriebnahme"
                                          [:packed-local-date-time
                                           (partial dates/parse-excel-cell-as-date is-1904-system?)]}})))

;; In any real-world dataset, there are always data quality issues that we should address before proceeding with an analysis. If we don't, we risk drawing incorrect conclusions because the source data may be unreliable or unrealistic. We need to expose any issues like this ahead of time so that we can correctly interpret our results.

;; In this case we have some very helpful advice in the dataset description pointing out several things we should look for, including:
;; 1. Gaps in the data from technical issues (battery failures, defective equipment, transmission issues, etc.)
;; 2. Different station start dates
;; 3. Environmental factors affecting measurement
;; 4. Infrastructure changes affecting traffic (detours, construction, network updates)
;; 5. Mis-measurement due to incorrect bike lane usage (cyclists travelling in the wrong direction)

;; We'll build up a dashboard to quantify and visualize these data quality issues so that we can conduct an accurate analysis.

;; First we'll calculate the uptime percentage for each station. To do this we'll
;; 1. Look at the location metadata dataset to determine when a station came online
;; 2. Determine how many measurements it _should_ have, if it had been working 24/7
;; 3. Examine how many measurements we actually have, and compare

;; Then we'll flag suspicious values by checking for ones that appear significantly higher or lower than the mean.

;; Looking at our entire dataset together, we can start by grouping by station ID, since many of quality metrics we care about will be most relevant at a station level.
(def combined-dataset (tc/dataset "data/prepared/combined-data.csv" {:key-fn keyword
                                                                     :parser-fn {:date [:local-date-time "yyyy-MM-dd'T'HH:mm"]}}))

(def grouped-by-station-id (tc/group-by combined-dataset :station-id))

grouped-by-station-id

;; There are 38 groups, but only 35 rows in our location metadata sheet:

(def location-info-ds
  (tc/dataset "data/prepared/location-info.csv" {:key-fn keyword
                                                 :parser-fn {:installed [:local-date-time "yyyy-MM-dd'T'HH:mm"]}}))

;; First, we should reconcile this. Clojure has some useful built-in helpers. `clojure.data/diff` returns a tuple of [things-only-in-a things-only-in-b things-in-both]

(data/diff (set (:name grouped-by-station-id)) (set (:station-id location-info-ds)))

;; Here we can easily spot the problem it appears that there are two stations that probably got mis-named. `"17-SK-BRE-O"` and `"17-SK-BRE-W"` are in the location-info dataset, but do not appear at all in the timeseries data. However, in the timeseries we do have these five station ids that are not in the location-info dataset:

#{"17-SZ-BRE-W" "02-MI-AL-W" "03-SP-NO-W" "03-SP-NO-O" "17-SZ-BRE-O"}

;; It seems likely that `"17-SZ-BRE-W"` and `"17-SZ-BRE-O"` are meant to be `"17-SK-BRE-W"` and `"17-SK-BRE-O"`. We could be more sure by inspecting the first row of the raw datasets we had, which included the installation date in the same cell as the station id:

(->> datasets
     (map tc/column-names)
     (map (partial filter #(str/includes? % "17"))))

;; Comparing these to the rows from the location-info dataset, we can see the installation dates are the same:

(-> location-info-ds
    (tc/select-rows (comp #{"17-SK-BRE-O" "17-SK-BRE-W"} :station-id)))

;; So we can conclude that these two sets of IDs are actually the same.

;; There are still 3 other station ids in our timeseries data that do not appear in the location info dataset: `"02-MI-AL-W"`, `"03-SP-NO-W"`, and `"03-SP-NO-O"`. We can check if there's anything similar:

(-> location-info-ds
    (tc/select-rows #(re-find #"MI-AL|SP-NO" (:station-id %))))

;; We can check again how these compare to the raw data to see if we can infer anything about what may be going on here:

(defn- show-column-names-with-substring [datasets val]
  (->> datasets
       (map tc/column-names)
       (map (partial filter #(str/includes? % val)))))

(show-column-names-with-substring datasets "MI-AL")
(show-column-names-with-substring datasets "SP-NO")

;; Again we can see this is most likely a case of station ids changing over time. The original column headers reveal that the stations are most likely the same ones, but have different IDs in different years. We'll  update this in our combined dataset. To review, we have this mapping of station-ids present in the raw data to station-ids in the location info. We will use the location info ones as the canonical ones:

(def station-id-mapping
  {"17-SZ-BRE-W" "17-SK-BRE-W"
   "17-SZ-BRE-O" "17-SK-BRE-O"
   "02-MI-AL-W" "01-MI-AL-W"
   "03-SP-NO-W" "16-SP-NO-W"
   "03-SP-NO-O" "16-SP-NO-O"})

;; We'll apply this mapping to our `station-id` column, then verify that it worked. We can see that after the update we have the exact same collection of unique station-ids in our timeseries data as we do in the location-info metadata we were given.

(def corrected-station-ids
  (tc/update-columns combined-dataset :station-id (partial replace station-id-mapping)))

(set/difference (-> corrected-station-ids (tc/group-by :station-id) :name set)
                (set (:station-id location-info-ds)))

;; Now we'll figure out each station's reliability. This is where we can really see the power of tablecloth's magic handling of grouped datasets. We can just use all of the regular tablecloth functions and they will magically work on the grouped datasets.

;; First we get the latest measurement time. Also double check it's the same for every station:

(-> corrected-station-ids
    (tc/group-by [:station-id])
    (tc/aggregate {:latest-date #(apply jt/max (:date %))})
    :latest-date
    distinct)

;; Yes, all stations have their last measurement at 2024-12-31T23:00

(def latest
  (apply jt/max (:date corrected-station-ids)))

latest

;; Then get the total number of measurements for each station:
(defn- add-location-info-and-station-count [ds location-info]
  (-> ds
      (tc/group-by [:station-id])
      ;; Count the total rows minus the header row per grouped dataset
      (tc/aggregate {:total-measurement-count #(- (tc/row-count %) 1)})
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
                        (/ total expected)))))

(-> corrected-station-ids
    (add-location-info-and-station-count location-info-ds)
    (add-uptime-col latest))

;; These look mostly good. We can see 2 have uptimes more than 100%. The most likely cause, and something we want to check for anyway, is duplicate measurements. In our combined dataset, there should only ever be one count per station/time combination. We can double check this:

;; Count all rows:
(tc/row-count corrected-station-ids)

;; Count unique rows by date/station-id combination:
(-> corrected-station-ids
    (tc/unique-by [:date :station-id])
    tc/row-count)

;; So there are some dupes. We'll find them and see what we're dealing with:

(def unique-measurement-counts
  (-> corrected-station-ids
      (tc/group-by [:date :station-id])
      (tc/aggregate {:row-count tc/row-count})))

;; Get the combinations of date/station-id that have more than one count:
(-> unique-measurement-counts
    (tc/select-rows (comp (partial < 1) :row-count)))

;; Can see they're all for a single station, which is great:
(-> unique-measurement-counts
    (tc/select-rows (comp (partial < 1) :row-count))
    :station-id
    distinct)

;; So we'll investigate what's going on with these rows:
(let [duplicated-dates (-> unique-measurement-counts
                           (tc/select-rows (comp (partial < 1) :row-count))
                           :date
                           distinct
                           set)]
  (-> corrected-station-ids
      (tc/select-rows #(and (= "12-PA-SCH" (:station-id %))
                            (duplicated-dates (:date %))))
      (tc/order-by :date)))

;; It seems like they're mostly identical, which is great. They're just straight up duplicates, not discrepancies we have to reconcile. We can verify this:

(let [duplicated-dates (-> unique-measurement-counts
                           (tc/select-rows (comp (partial < 1) :row-count))
                           :date
                           distinct
                           set)]
  (-> corrected-station-ids
      (tc/select-rows #(and (= "12-PA-SCH" (:station-id %))
                            (duplicated-dates (:date %))))
      (tc/group-by [:date :station-id])
      (tc/aggregate {:same-counts? = })
      :same-counts?
      distinct))

;; They're all identical, so we will do a very naive de-dedupe on our data:

(-> corrected-station-ids
    (tc/unique-by [:date :station-id])
    (add-location-info-and-station-count location-info-ds)
    (add-uptime-col latest))

;; And now the data looks better. We don't have any uptimes above 100%.

;; One last check -- Look for suspicious values. Like counts that are not whole numbers:

(-> corrected-station-ids
    (tc/update-columns :count (partial map #(mod % 1)))
    :count
    distinct)


;; TODO: Collect all of these checks/clean ups systematically

;; Lastly, we'll save the results of this

(-> corrected-station-ids
    (tc/unique-by [:date :station-id])
    (tc/write-csv! "data/prepared/cleaned-dataset.csv"))

;; All of this illustrates the importance of defining and using consistent IDs as a data publisher. Failing to do this makes it very hard for downstream consumers of your data to have confidence that they are interpreting it correctly.
