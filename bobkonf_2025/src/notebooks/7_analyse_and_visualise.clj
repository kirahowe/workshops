^:kindly/hide-code
(ns notebooks.7-analyse-and-visualise
  (:require
   [java-time.api :as jt]
   [notebooks.6-quality-control :as qa]
   [scicloj.tableplot.v1.plotly :as plotly]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]))

;; # Analysis and visualisation

;; ## Time series/time-based analysis

(def dataset (tc/dataset qa/clean-dataset-file-name
                         {:key-fn keyword
                          :parser-fn {:datetime [:local-date-time "yyyy-MM-dd'T'HH:mm"]}}))

;; We'll start by breaking our date column into year/month/day/hour so we can start to examine some trends:

(def get-hour (memfn getHour))

(def with-temporal-components
  (-> dataset
      (tc/map-columns :year :datetime (comp jt/value jt/year))
      (tc/map-columns :month :datetime (comp jt/value jt/month))
      (tc/map-columns :day-of-week :datetime jt/day-of-week)
      (tc/map-columns :hour :datetime get-hour)
      (tc/order-by :datetime)))

;; Yearly trends
(defn- calculate-yearly-trends [ds]
  (-> ds
      (tc/group-by [:year])
      (tc/aggregate {:total-count (comp int tcc/sum :count)
                     :avg-count (comp int tcc/mean :count)
                     :stations (comp count distinct :station-id)})
      (tc/order-by :year)))

(-> (calculate-yearly-trends with-temporal-components)
    (plotly/base {:=width 1000
                  :=title "Yearly bicycle traffic trend"})
    (plotly/layer-line {:=x :year
                        :=y :avg-count}))

;; Monthly trends
(defn calculate-monthly-average [ds]
  (-> ds
      (tc/group-by [:year :month])
      (tc/aggregate {:avg-count (comp int tcc/mean :count)})
      (tc/map-columns :datetime [:year :month] (fn [year month]
                                             (jt/local-date year month)))
      (tc/order-by :datetime)))

(-> (calculate-monthly-average with-temporal-components)
    (plotly/base {:=width 1000
                  :=title "Monthly bicycle traffic trend"})
    (plotly/layer-line {:=x :datetime
                        :=y :avg-count}))

;; How has bike traffic changed from season to season over time?
(-> with-temporal-components
    (tc/group-by [:year :month])
    (tc/aggregate {:monthly-total (comp int tcc/sum :count)
                   :daily-avg (comp int tcc/mean :count)})
    (tc/map-columns :season :month {12 "Winter" 1 "Winter" 2 "Winter"
                                    3 "Spring" 4 "Spring" 5 "Spring"
                                    6 "Summer" 7 "Summer" 8 "Summer"
                                    9 "Fall" 10 "Fall" 11 "Fall"})
    (plotly/base {:=width 1000
                  :=title "Seasonal daily average counts"})
    (plotly/layer-bar {:=x :year
                       :=x-title "Year"
                       :=y :daily-avg
                       :=y-title "Daily average count across all stations"
                       :=color :season
                       :=mark-opacity 0.8}))


;; How does weekend traffic compare to weekday traffic?
(-> with-temporal-components
    (tc/map-columns :is-weekend :datetime jt/weekend?)
    (tc/group-by [:year :is-weekend])
    (tc/aggregate {:avg-count (comp int tcc/mean :count)})
    (plotly/base {:=width 1000})
    (plotly/layer-bar {:=x :year
                       :=y :avg-count
                       :=color :is-weekend}))

;; What is the busiest day of the week?
(-> with-temporal-components
    (tc/group-by [:day-of-week])
    (tc/aggregate {:avg-count (comp int tcc/mean :count)})
    (tc/order-by :day-of-week)
    (plotly/base {:=title "Average bicycle traffic by day of week"
                  :=width 1000})
    (plotly/layer-bar {:=x :day-of-week
                       :=y :avg-count}))

;; What is the busiest time of day?
(-> with-temporal-components
    (tc/group-by [:hour])
    (tc/aggregate {:avg-count (comp int tcc/mean :count)})
    (plotly/base {:=width 1000
                  :=title "Hourly traffic average"})
    (plotly/layer-bar {:=x :hour
                       :=x-title "Hour of day"
                       :=y :avg-count
                       :=y-title "Average count across all stations"}))


;; How did the pandemic impact bicycle traffic?
(def pandemic-periods
  (let [march-2020 (jt/local-date-time 2020 03 01)
        may-2023 (jt/local-date-time 2023 05 01)
        pre-pandemic (tc/select-rows with-temporal-components  #(jt/before? (:datetime %) march-2020))
        pandemic (tc/select-rows with-temporal-components #(jt/<= march-2020 (:datetime %) may-2023))
        post-pandemic (tc/select-rows with-temporal-components #(jt/after? (:datetime %) may-2023))]
    {:pre-pandemic pre-pandemic
     :pandemic pandemic
     :post-pandemic post-pandemic}))

(-> (calculate-monthly-average (:pre-pandemic pandemic-periods))
    (plotly/base {:=width 1000
                  :=title "Impact of the pandemic on bicycle traffic"})
    (plotly/layer-line {:=x :datetime
                        :=y :avg-count})
    (plotly/layer-line {:=dataset (calculate-monthly-average (:pandemic pandemic-periods))
                        :=x :datetime
                        :=y :avg-count})
    (plotly/layer-line {:=dataset (calculate-monthly-average (:post-pandemic pandemic-periods))
                        :=x :datetime
                        :=y :avg-count}))

(-> (calculate-monthly-average (:pre-pandemic pandemic-periods))
    (plotly/base {:=width 1000
                  :=title "Impact of the pandemic on bicycle traffic"})
    (plotly/layer-bar {:=x :datetime
                       :=y :avg-count})
    (plotly/layer-bar {:=dataset (calculate-monthly-average (:pandemic pandemic-periods))
                       :=x :datetime
                       :=y :avg-count})
    (plotly/layer-bar {:=dataset (calculate-monthly-average (:post-pandemic pandemic-periods))
                       :=x :datetime
                       :=y :avg-count}))

;; What are the busiest times for cyclists in Berlin?
(-> with-temporal-components
    (tc/group-by [:day-of-week :hour])
    (tc/aggregate {:avg-count (comp int tcc/mean :count)})
    (tc/order-by :day-of-week)
    (plotly/layer-heatmap {:=x :hour
                           :=y :day-of-week
                           :=z :avg-count}))

;; Find anomalous days (very high or low traffic) across all stations
(-> with-temporal-components
    (tc/group-by [:datetime])
    (tc/aggregate {:daily-total (comp int tcc/sum :count)
                   :daily-avg (comp int tcc/mean :count)
                   :std-dev (comp int tcc/standard-deviation :count)})
    (tc/drop-rows (comp zero? :std-dev))
    (tc/map-columns :z-score [:daily-total :daily-avg :std-dev]
                    (fn [total avg stdev]
                      (/ (- total avg) stdev)))
    (plotly/base {:=width 1200 :=title "Days with unusual bicycle traffic"})
    (plotly/layer-point {:=x :datetime
                         :=xtype :temporal
                         :=mark-size 4
                         :=y :z-score}))


;; ## Spatial analysis

;; For this we'll make use of our other dataset that includes metadata about the stations.

;; How does bicycle traffic vary across different locations in Berlin?
;; Station comparison - traffic volume
(-> with-temporal-components
    (tc/group-by [:station-id])
    (tc/aggregate {:avg-count (comp int tcc/mean :count)})
    (tc/inner-join qa/location-info-ds :station-id)
    (tc/order-by [:avg-count :desc])
    (plotly/base {:=width 1000 :=title "Station traffic comparison"})
    (plotly/layer-bar {:=x :direction :=y :avg-count}))

;; Station growth over time (year-over-year)
(-> with-temporal-components
    (tc/group-by [:year :station-id])
    (tc/aggregate {:yearly-avg (comp int tcc/mean :count)})
    (tc/inner-join qa/location-info-ds :station-id)
    (plotly/base {:=width 1000 :=title "Station growth over time"})
    (plotly/layer-line {:=x :year :=y :yearly-avg :=color :direction}))
