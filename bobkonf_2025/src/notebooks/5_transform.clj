(ns notebooks.transform
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [notebooks.3-extract :as ex]
   [tablecloth.api :as tc]
   [tech.v3.libs.fastexcel :as xlsx]
   [utils.dates :as dates]))

;; # Transform

;; TODO: Write brief blurb here about making the transformations we did on a single dataset in the last section more reliable here.

;; The first thing we'll do is parse dates on ingestion and reload the data, this will be much more efficient:

(def datasets
  (let [is-1904-system? (dates/is-1904-system? ex/raw-data-file-name)]
    (xlsx/workbook->datasets "data/prepared/raw-data.xlsx"
                             {:parser-fn {"Zählstelle        Inbetriebnahme"
                                          [:packed-local-date-time
                                           (partial dates/parse-excel-cell-as-date is-1904-system?)]}})))

;; Great. Now we need to process the rest of the data. We can write simple functions to execute the steps we took:

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

;; Side not about performance:

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
        doall))

  ;; TODO: as a transducer, compare performance
  )

;; Now we have a set of tidy datasets all with the same column names. To facilitate our analysis, we'll combine all of the years into a single dataset:

;; TODO: don't write a 100MB CSV here, discuss better storage options
(let [ds (->> (io/file "data/prepared")
              file-seq
              (filter #(str/includes? % "data-"))
              sort
              (map tc/dataset)
              (apply tc/concat))]
  (tc/write! ds "data/prepared/combined-data.csv"))


;; And compress it
