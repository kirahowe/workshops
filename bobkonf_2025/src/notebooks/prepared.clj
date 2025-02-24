(ns notebooks.prepared
  (:require
   [clj-http.client :as http]
   [scicloj.tableplot.v1.plotly :as plotly]
   [clojure.data :as data]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [java-time.api :as jt]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]
   [scicloj.tcutils.api :as tcu]
   [utils.dates :as dates]
   [clojure.set :as set]))

;; # Exploring bicycle traffic in Berlin
;; _BobKonf 2025, Berlin

;; Getting insights from data. The whole point is to try to answer a question.

;; In this workshop we'll explore Clojure's data science toolkit, using it to walk through
;; exploring and analyzing a [dataset about bicycle traffic in Berlin](https://daten.berlin.de/datensaetze/radzahldaten-in-berlin).

;; Today we'll explore how bicycle usage in Berlin has evolved over time. By the end of the workshop we'll be able to answer questions like:
;; - What are the most travelled bicycle routes in the city?
;; - How did the pandemic affect bicycle usage in Berlin?
;; - How is bicycle traffic affect by time of day, day of the week, or time of year?

;; ## Overview
;; 1. Setup and intro (15 mins)
;;   Hopefully you have already installed Clojure and one way or another have a working Clojure environment. We'll make sure everyone is set up to follow along and take a very quick tour of Clojure's as a language before diving in to the specific libraries we'll use for a more realistic data analysis.
;;
;; 2. Data loading and exploration (20 mins)
;;   We'll learn how to load data from the internet into a tablecloth dataset, how to parse excel files, and how to explore it so we can see what we're working with.

;; 3. Data cleaning and transformation (20 mins)
;;   Once we have a sense of what our data looks like, we'll learn how to clean it up, transforming into an "alaysis-ready" dataset.

;; 4. Analysis, visualization and reporting
;;   After we have a tidy dataset usable for analysis, we'll get to work answering some of our original questions.

;; 5. Integration and production-readiness
;;   This is where the reproducibility parts comes in. As we go, we'll architect our data wrangling and analysis code in a way that makes it easy to re-run. We'll see how easy it is to deploy a dashboard based on our analysis.

;; ## 2. Data loading and exploration

;; ## 2.1 Loading data from the internet

;; First we'll download the dataset from the source.

;; ::: {.callout-note}
;; **Note:** If you haven't already done this and the internet is slow or the download is not working for some reason, the file is also included in this repo already as `data/2025-11-13/raw-data.xlsx`
;; :::

;; Our dataset is described on the [Berlin open data portal](https://daten.berlin.de/datensaetze/radzahldaten-in-berlin).

;; ::: {.callout-tip}
;; We should always check first that a given dataset is appropriately licensed for our use case. This dataset is licensed under the permissive [DL-DE->Zero-2.0 license](https://www.govdata.de/dl-de/zero-2-0), so we are free to use it without restrictions or conditions.
;; :::

;; The dataset is available using this link:
(def dataset-source-url "https://www.berlin.de/sen/uvk/_assets/verkehr/verkehrsplanung/radverkehr/weitere-radinfrastruktur/zaehlstellen-und-fahrradbarometer/gesamtdatei-stundenwerte.xlsx?ts=1737968619")

;; To download the dataset file, we'll use `clj-http` to stream the file contents to our local disk. The quickest way is to just stream the data directly to a file with a hard-coded name:

;; ::: {.callout-tip}
;; This will download the dataset, which is 16.1MB. If you are on a slow or limited-bandwidth connection, you may prefer to skip evaluating it and use the data that is already downloaded to this repo, in `data/raw-data.xlsx`
;; :::

(comment
  (let [response (http/get dataset-source-url {:as :byte-array})
        file-name "data/prepared/raw-data.xlsx"]
    (io/make-parents file-name)
    (with-open [output (io/output-stream file-name)]
      (io/copy (:body response) output))))

;; This works for now. Later in the workshop we'll see how we can make it more robust and reliable for deploying to production.

;; 2.2 Loading xlxs data in Clojure

;; To load xlsx data we'll need to require fastexcel:

(require '[tech.v3.libs.fastexcel :as xlsx])

(def file-name "data/prepared/raw-data.xlsx")

;; Attempting to load the excel file as a dataset directly shows us that there are actually 16 datasets contained in this workbook:
(tc/dataset file-name)

;; So instead we'll make a seq of datasets, loading each sheet as its own dataset:

;; ::: {.callout-tip}
;; TODO: explain `defonce`? Or maybe just don't use it..
;; :::

(def datasets
  (xlsx/workbook->datasets file-name))

;; We can see what our sheets are named like this:
(map tc/dataset-name datasets)

;; The first three are reference datasets and the rest are a time series. This is pretty common. We'll extract some metadata out of these first three datasets to use in our final report. The first dataset just contains a big description, formatted for aesthetics rather than machine-readability, so we'll have to dig in to a random cell in the middle to get the content out.

;; First we'll drop all the columns where every value is nil, which we can see leaves us with a single column:

(def info-ds (first datasets))

(tc/drop-columns info-ds (fn [col] (every? nil? (info-ds col))))

;; Then we'll join all the non-nil values in this column into a single wall of text, and store it as the description for this dataset.
(->> (tc/drop-columns info-ds (fn [col] (every? nil? (info-ds col))))
     tc/rows
     flatten
     (remove nil?)
     (map str/trim)
     (str/join "\n")
     (spit "data/prepared/description-de.txt"))

;; You'll notice this description is in German. As a quick bonus step, we can translate it to German using a free online translation API. To follow this particular example you'll need an API key from [DeepL](https://www.deepl.com/en/pro#developer).

(comment
  (require '[clojure.java.shell :as sh])

  (let [key (->> "secrets/translate-api-key.txt.gpg" (sh/sh "gpg" "--decrypt") :out str/trim)
        response (http/post "https://api-free.deepl.com/v2/translate"
                            {:body (json/write-str {:text [(slurp "data/prepared/description-de.txt")]
                                                    :target_lang "EN"})
                             :headers {"Authorization" (str "DeepL-Auth-Key " key)}
                             :content-type :json})
        english (-> response :body json/read-str (get-in ["translations" 0 "text"]))]
    (spit "data/prepared/description-en.txt"
          (str "NOTE: THIS CONTENT IS A MACHINE TRANSLATION OF THE ORIGINAL IN data/prepared/description-de.txt\n"
               "provided only for convenience with no guarantee of accuracy.\n\n"
               english))))

;; Next we'll look at the "Legende" sheet to see what we can learn about our dataset from it.

(second datasets)

;; This does not appear to be very useful. We'll come back to it if it seems like it might be interesting later on.

;; Next is the "Standortdaten" sheet:

(nth datasets 2)

;; This is useful information about the different tracker locations. It also reveals our first encounter with one very annoying thing about working with Excel files -- dates. Very annoyingly, dates in excel are stored as number of days since 1900-1-1. We'll do some basic transformations to this dataset, and store it as a CSV file.

(-> datasets
    (nth 2)
    ;; It's idiomatic to use keywords as column names in Clojure, so we'll translate these to English and make them keywords
    (tc/rename-columns {"Zählstelle" :station-id
                        "Beschreibung - Fahrtrichtung" :direction
                        "Breitengrad" :lat
                        "Längengrad" :long
                        "Installationsdatum" :installed})
    ;; We'll parse the installation date values as dates based on Excel's idiosyncratic date handling
    (tc/update-columns :installed (partial map dates/parse-excel-cell-as-date))
    ;; Save this transformed data
    (tc/write-csv! "data/prepared/location-info.csv"))

;; Now we get into the interesting work. Inspecting one of the annual data files we can see that this dataset is stored in a very wide format with bad column names

;; This data is a mess. The one saving grace is that all years of data are messy in the same way.

(def data-2012 (nth datasets 3))

data-2012

(tc/column-names data-2012)

(map tc/column-names datasets)

;; Parse dates on ingestion and reload the data, will be much more efficient:

(def datasets
  (let [is-1904-system? (dates/is-1904-system? file-name)]
    (xlsx/workbook->datasets "data/prepared/raw-data.xlsx"
                             {:parser-fn {"Zählstelle        Inbetriebnahme"
                                          [:packed-local-date-time
                                           (partial dates/parse-excel-cell-as-date is-1904-system?)]}})))

;; Figure out how to parse a single dataset first

(def data-2012 (nth datasets 3))

;; Inspect a single one:
(tc/info data-2012)

;; Data is very sparse. We'll make it tidy and assume a missing value later implies `nil`, rather than explicitly including blank cells in our cleaned datasets.

(let [;; Get the names of all empty columns that are not empty
      non-empty-columns (-> data-2012
                            tc/info
                            (tc/select-rows (comp pos? :n-valid))
                            :col-name)]
  ;; Select only those
  (-> data-2012
      (tc/select-columns (set non-empty-columns))
      ;; Now fix the column names
      ;; First column is always the date
      (tc/rename-columns {"Zählstelle        Inbetriebnahme" :date})
      ;; Then we'll strip the date part off the end of the rest of the column names. Regex isn't super efficient, but that's fine we're only ever working with a small number of columns. Since these are domain-specific IDs, we'll leave them as-is, rather than converting them to keywords or something else.
      (tc/rename-columns (complement #{:date}) #(->> % (re-matches #"^(.+)\d{2}\.\d{2}\.\d{4}") second str/trim))
      ;; Now we'll make the data "tidy". It's not super obvious why this is necessary with a small dataset like this, but it will be for the ones that have more columns
      (tc/pivot->longer (complement #{:date}) {:target-columns :station-id
                                               :value-column-name :count})
      ;; Then save this for later
      ;; (tc/write! "data/prepared/data-2012.csv")
      ))

;; Great. Now we need to process the rest of the data.

(defn drop-empty-columns [ds]
  (let [non-empty-columns (-> ds tc/info (tc/select-rows (comp pos? :n-valid)) :col-name)]
    (tc/select-columns ds (set non-empty-columns))))

(defn update-column-names [ds]
  (-> ds
      (tc/rename-columns {"Zählstelle        Inbetriebnahme" :date})
      (tc/rename-columns (complement #{:date}) #(->> % (re-matches #"^(.+)\s\d{2}\.\d{2}\.\d{4}") second str/trim))))

(defn make-tidy [ds]
  (tc/pivot->longer ds (complement #{:date}) {:target-columns :station-id
                                              :value-column-name :count}))

(defn write-csv! [ds]
  (let [year (re-find #"\d{4}" (tc/dataset-name ds))]
    (tc/write! ds (str "data/prepared/data-" year ".csv"))))

(->> datasets
     (drop 4) ;; We've already handled the first 4
     ;; (map tc/dataset-name)
     ;; (map tc/column-names)
     (map drop-empty-columns)
     ;; (map tc/column-names)
     (map update-column-names)
     ;; (map tc/column-names)
     (map make-tidy)
     ;; (map tc/column-names)
     (map write-csv!))


(comment
  (defmacro benchmark
    "Benchmarks memory usage of the given body forms.
   Returns a map containing:
   - :memory-used - Memory used in MB
   - :return-value - The actual return value of the body
   - :time - Time spent in garbage collection (ms)"
    [& body]
    `(do
       (System/gc)
       (let [runtime# (Runtime/getRuntime)
             start-mem# (.totalMemory runtime#)
             start-free# (.freeMemory runtime#)
             start-time# (System/nanoTime)
             result# (do ~@body)
             end-time# (System/nanoTime)
             end-mem# (.totalMemory runtime#)
             end-free# (.freeMemory runtime#)
             used-memory# (/ (- (- end-mem# end-free#)
                                (- start-mem# start-free#))
                             1024.0
                             1024.0)]
         {:memory-used (double used-memory#)
          :return-value result#
          :time (/ (- end-time# start-time#) 1000000.0)})))

  (benchmark
   (->> datasets
        (drop 4)
        (map drop-empty-columns)
        (map update-column-names)
        (map make-tidy)
        (map write-csv!)
        doall))

  (benchmark
   (->> datasets
        (drop 4)
        (map (comp write-csv! make-tidy update-column-names drop-empty-columns))
        doall))

  (benchmark
   (->> datasets
        (drop 4)
        (pmap (comp write-csv! make-tidy update-column-names drop-empty-columns))
        doall)))

;; Now we have a set of tidy datasets all with the same column names. To facilitate our analysis, we'll combine all of the years into a single dataset:

(let [ds (->> (io/file "data/prepared")
              file-seq
              (filter #(str/includes? % "data-"))
              sort
              (map tc/dataset)
              (apply tc/concat))]
  (tc/write! ds "data/prepared/combined-data.csv"))

;; ## Bonus: Data quality analysis and cleaning

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


;; ## Analysis and visualisation

(def dataset (tc/dataset "data/prepared/cleaned-dataset.csv" {:key-fn keyword
                                                              :parser-fn {:date [:local-date-time "yyyy-MM-dd'T'HH:mm"]}}))

;; We'll start by breaking our date column into year/month/day/hour so we can start to examine some trends:

(def get-hour (memfn getHour))

(def with-temporal-components
  (-> dataset
      (tc/map-columns :year :date jt/year)
      (tc/map-columns :month :date #(jt/as % :month-of-year))
      (tc/map-columns :day-of-week :date jt/day-of-week)
      (tc/map-columns :hour :date get-hour)))

;; Yearly trends
(defn- calculate-yearly-trends [ds]
  (-> ds
      (tc/group-by [:year])
      (tc/aggregate {:total-count (comp int tcc/sum :count)
                     :avg-count (comp int tcc/mean :count)
                     :stations (comp count distinct :station-id)})
      (tc/order-by :year)))

(calculate-yearly-trends with-temporal-components)

;; Seasonal trends
(-> with-temporal-components
    (tc/group-by [:year :month])
    (tc/aggregate {:monthly-total (comp int tcc/sum :count)
                   :daily-avg (comp int tcc/mean :count)})
    (tc/map-columns :season :month {12 "Winter" 1 "Winter" 2 "Winter"
                                    3 "Spring" 4 "Spring" 5 "Spring"
                                    6 "Summer" 7 "Summer" 8 "Summer"
                                    9 "Fall" 10 "Fall" 11 "Fall"}))

;; Weekly
(-> with-temporal-components
    (tc/map-columns :is-weekend :date jt/weekend?)
    (tc/group-by [:year :is-weekend])
    (tc/aggregate {:total-count (comp int tcc/sum :count)
                   :avg-count (comp int tcc/mean :count)}))


;; Daily
(-> with-temporal-components
    (tc/group-by [:hour])
    (tc/aggregate {:avg-count (comp int tcc/mean :count)})
    (tc/order-by :hour))

;; Pandemic impact
(def pandemic-periods
  (let [march-2020 (jt/local-date-time 2020 03 01)
        may-2023 (jt/local-date-time 2023 05 01)
        pre-pandemic (tc/select-rows with-temporal-components  #(jt/before? (:date %) march-2020))
        pandemic (tc/select-rows with-temporal-components #(jt/<= march-2020 (:date %) may-2023))
        post-pandemic (tc/select-rows with-temporal-components #(jt/after? (:date %) may-2023))]
    {:pre-pandemic pre-pandemic
     :pandemic pandemic
     :post-pandemic post-pandemic}))

(calculate-yearly-trends (:pre-pandemic pandemic-periods))
(calculate-yearly-trends (:pandemic pandemic-periods))
(calculate-yearly-trends (:post-pandemic pandemic-periods))

(defn overall-average [ds]
  (-> ds calculate-yearly-trends :avg-count tcc/mean))

(let [pre-pandemic-average (-> pandemic-periods :pre-pandemic overall-average)
      during-average (-> pandemic-periods :pandemic overall-average)]
  (* 100 (/ (- during-average pre-pandemic-average) pre-pandemic-average)))

(-> with-temporal-components
    (plotly/layer-point {:=x :date
                         :=y :count
                         :=name "Daily counts"})
    ;; (plotly/layer-smooth {:=x :date
    ;;                       :=y :count
    ;;                       :=name "Trend"
    ;;                       :=mark-color "red"})
    )


;; (-> )
#_(-> (rdatasets/datasets-iris)
    (tc/random 10 {:seed 1})
    (plotly/layer-point
     {:=x :sepal-width
      :=y :sepal-length
      :=color :species
      :=mark-size 20
      :=mark-opacity 0.6}))

;; ## Making this more robust for production

;; ### Downloading the data. To make this more robust, we can build out some helper functions to make it easier to update the data in a systematic way. We'll write some scaffolding to handle this.


(defn download-dataset
  "Streams a file from the given URL to the `data/current-date` directory as `raw-data.xlsx`.
  Also stores response headers in a `headers.edn` file."
  [url target-dir]
  (let [response (http/get url {:as :stream})
        input-stream (:body response)
        output-stream (io/output-stream (str target-dir "/raw-data.xlsx"))]
    (spit (io/file (str target-dir "/headers.edn")) (:headers response))
    (io/copy input-stream output-stream)))

(defn make-dataset-dir!
  "Generates a directory name at `data/yyyy-mm-dd`"
  []
  (let [dir-name (io/file (str "data/" (jt/local-date)))]
    (io/make-parents dir-name)
    dir-name
    ;; (if (and (.exists dir-name) (.isDirectory dir-name))
    ;;   dir-name
    ;;   (doto dir-name .mkdir))
    ))

(comment
  (def dataset-path
    (let [dataset-dir (make-dataset-dir!)]
      (download-dataset dataset-source-url dataset-dir))))
