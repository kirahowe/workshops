^:kindly/hide-code
(ns notebooks.5-transform
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [notebooks.3-extract :as extract]
   [tablecloth.api :as tc]
   [tech.v3.libs.fastexcel :as xlsx]
   [utils.dates :as dates]))

;; # Transform

;; In the previous section, we explored data extraction and performed some initial transformations on a single dataset. Now, we'll make these transformations more robust and apply them to all of the datasets in our timeseries. We'll see how we can build reusable data processing pipelines in Clojure, which are essential for reliable data analysis workflows.
;;
;; The first thing we'll do is parse dates on ingestion and reload the data, this will be more efficient:

(def datasets
  (let [is-1904-system? (dates/is-1904-system? extract/raw-data-file-name)]
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
      (tc/rename-columns {"Zählstelle        Inbetriebnahme" :datetime})
      (tc/rename-columns (complement #{:datetime})
                         #(->> % (re-matches #"^(.+)\s\d{2}\.\d{2}\.\d{4}") second str/trim))))

(defn make-tidy [ds]
  (tc/pivot->longer ds (complement #{:datetime}) {:target-columns :station-id
                                                  :value-column-name :count}))

;; We can combine all of these operations and apply the same transformations to each of our yearly datasets, and then since they all have the same column names we can simply concatenate them all into one big dataset:

(defn transform-datasets [ds-coll]
  (->> ds-coll
       (map drop-empty-columns)
       (map update-column-names)
       (map make-tidy)
       (apply tc/concat)))

;; A side note about performance -- there are more efficient ways to accomplish this, we can compare them with some basic benchmarks:

(comment
  ;; To run these, you'll need to evaluate this `benchmark` macro in your REPL. By default anything inside a `comment` like this is not evaluated.
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
          :time (/ (- end-time# start-time#) 1000000.0)})))

  ;; The most intuitive way to write our transformation pipeline (IMO) is like this:
  (benchmark
   (->> datasets
        (drop 3)
        (map drop-empty-columns)
        (map update-column-names)
        (map make-tidy)
        (apply tc/concat)))

  ;; But this isn't necessarily always the best option, especially when we're working with relatively large datasets like this, because it applies each transformation separate to the entire collection of datasets. I find this the most straightforward and readable, but it can be inefficient and use more memory as it stores the intermediate results between steps. It's also not the most compute-efficient because each dataset is processed multiple times. Another option is to combine all of the transformations at once using function composition, like this:

  (benchmark
   (->> datasets
        (drop 3)
        (map (comp make-tidy update-column-names drop-empty-columns))
        doall))

  ;; This combines our transformations into a single function that gets applied to each dataset.
  ;; This doesn't really save us anything yet, but the nice thing about writing the transformation
  ;; this way is that it makes it trivially easy to take advantage of one of Clojure's super
  ;; powers, really amazing built-in support for concurrency:

  (benchmark
   (->> datasets
        (drop 3)
        (pmap (comp make-tidy update-column-names drop-empty-columns))
        doall))

  ;; This approach can significantly improve performance for CPU-bound transformations. Another
  ;; option that can optimize memory usage even further is to take advantage of transducers:
  (benchmark
   (let [xform (comp
                (map drop-empty-columns)
                (map update-column-names)
                (map make-tidy))]
     (->> datasets
          (drop 3)
          (transduce xform conj []))))

  ;; The transducer approach can be more memory efficient as it avoids creating intermediate
  ;; collections between each step.
  ;;
  ;; These are some different approaches to consider depending on the characteristics of the
  ;; transformation we're doing.
  )

;; Now we have one big tidy dataset. CSV isn't really the best storage option for datasets, but for the purposes of this workshop we'll use since it's universally known and supported. Better alternative file formats exist like parquet, avro, or nippy, which we would use if we were doing this "for real".
;;
;; At the very least, we can compress our big combined dataset (it's over 70MB!) to keep it in this repo. First we'll write our combined tidy dataset as csv:

(let [combined-dataset (->> datasets
                            (drop 3) ;; The first 3 datasets are reference, not timeseries data
                            transform-datasets)]
  (tc/write! combined-dataset "data/prepared/combined-data.csv"))

;; But we'll immediately compress it for sharing:

(def combined-dataset-file-name "data/prepared/combined-data.csv.gz")

(defn compress-file [input-file output-file]
  (with-open [input (io/input-stream input-file)
              output (java.util.zip.GZIPOutputStream.
                      (io/output-stream output-file))]
    (io/copy input output))
  (io/delete-file input-file))

(compress-file "data/prepared/combined-data.csv"
               combined-dataset-file-name)

;; This completes our transformation pipeline. We've:
;; 1. Loaded multiple Excel datasets with proper date parsing
;; 2. Cleaned and standardized column names
;; 3. Transformed wide data to long/tidy format
;; 4. Written a combined dataset
;; 5. Compressed the final result for efficient storage
;;
;; In the next section, we'll do some QA on this dataset to make sure it's ready for analysis, then we'll explore how to analyze this tidy data to extract meaningful insights.
