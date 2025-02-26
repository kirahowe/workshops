(ns notebooks.analysis-and-visualisation
  (:require
   [java-time.api :as jt]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]))

(def dataset (tc/dataset "data/prepared/cleaned-dataset.csv"
                         {:key-fn keyword
                          :parser-fn {:date [:local-date-time "yyyy-MM-dd'T'HH:mm"]}}))

dataset

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
