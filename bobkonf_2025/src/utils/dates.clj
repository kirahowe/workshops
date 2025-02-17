(ns utils.dates
  (:require
   [tech.v3.datatype.datetime :as datetime]
   [clojure.java.io :as io])
  (:import (org.dhatim.fastexcel.reader ReadableWorkbook)))

(defn is-1904-system? [file]
  (with-open [workbook (ReadableWorkbook. (io/file file))]
    (.isDate1904 workbook)))

(defn parse-excel-cell-as-date
  ([val]
   (parse-excel-cell-as-date false val))

  ([is-1904-system? val]
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
     (java.time.LocalDateTime/of local-date local-time))))
