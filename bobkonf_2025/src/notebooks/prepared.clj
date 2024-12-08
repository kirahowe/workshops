(ns notebooks.prepared
  (:require
   [tablecloth.api :as tc]))

;; Getting insights from data. The whole point is to try to answer a question.

;; Today we'll answer the question: How did the pandemic affect bike usage in Berlin?

;; ## Loading xlxs data

;; Need to require fastexcel to read .xlsx

(require '[tech.v3.libs.fastexcel :as xlsx])

(def file-name "data/radzaehlung_correct.xlsx")

;; The file has many sheets:
(tc/dataset file-name)

;; So instead we can make a seq of datasets from each sheet:
(defonce datasets
  (xlsx/workbook->datasets file-name))

(map tc/dataset-name datasets)

;; ## Data cleaning

;; For now let's just work with a single year, the most recent one:

(def ds
  (last datasets))

;; We can immediately see that the dates are broken. Something went wrong as we were parsing them and we need to fix it.

(tc/info ds)

;; They got parsed as floats, which are nonsensical and totally broken. We need to parse them as dates on ingestion. Unfortunately this requires reverse engineering the way Excel stores dates.

(require '[tech.v3.datatype.datetime :as datetime])

(import '[org.dhatim.fastexcel.reader ReadableWorkbook])
(require '[clojure.java.io :as io])

(defn is-1904-system? [file]
  (with-open [workbook (ReadableWorkbook. (io/file file))]
    (.isDate1904 workbook)))

(defn parse-excel-cell-as-date [is-1904-system? val]
  ;; TODO: deal with date parsing in excel -- translated Clojure from this Java fn:
  ;; https://github.com/dhatim/fastexcel/blob/04d3525dc6a5dc5476009e335bca0ecdf01350d1/fastexcel-reader/src/main/java/org/dhatim/fastexcel/reader/Cell.java#L99
  (let [whole-days (int (Math/floor val))
        ms-in-day (-> val
                      (- whole-days)
                      (* datetime/milliseconds-in-day)
                      (+ 0.5)
                      long)
        ;; sometimes the rounding for .9999999 returns the whole number of ms a day
        [adjusted-days adjusted-ms] (if (= ms-in-day datetime/milliseconds-in-day)
                                      [(inc whole-days) 0]
                                      [whole-days ms-in-day])
        start-year (if is-1904-system? 1904 1900)
        day-adjust (cond
                     ;; 1904 system starts at 1/2/1904
                     is-1904-system? 1

                     ;; Date is prior to 3/1/1900, so adjust because Excel thinks 2/29/1900 exists
                     ;; i.e. if Excel date == 2/29/1900, will become 3/1/1900
                     (< whole-days 61) 0

                     :else -1)
        local-date (.. (java.time.LocalDate/of start-year 1 1 )
                       (plusDays (+ adjusted-days day-adjust -1)))
        local-time (java.time.LocalTime/ofNanoOfDay (* adjusted-ms 1000000))]
    (java.time.LocalDateTime/of local-date local-time)))

;; The relevant column is called "DateTime", we can parse it on ingestion:

(def datasets
  (let [is-1904-system? (is-1904-system? file-name)]
    (xlsx/workbook->datasets "data/radzaehlung_correct.xlsx"
                             {:parser-fn {"DateTime"
                                          [:packed-local-date-time
                                           (partial parse-excel-cell-as-date is-1904-system?)]}})))







;; (tc/update-columns [:datetime]
;;                    (fn [dates-as-floats]
;;                      (-> dates-as-floats
;;                          (fun/* datetime/milliseconds-in-day)
;;                          fun/round
;;                          (fun/- (-> 70
;;                                     (* 365)
;;                                     (+ 19)
;;                                     (* datetime/milliseconds-in-day)))
;;                          (->> (datetime/milliseconds->datetime :local-date-time)))))
