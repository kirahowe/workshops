(ns london-clojurians-april-2024.notebook
  (:require
   [scicloj.clay.v2.api]
   [aerial.hanami.templates :as ht]
   [tech.v3.dataset.modelling :as dsmod]
   [tech.v3.datatype.functional :as dfn]
   [scicloj.metamorph.ml :as ml]
   [clojure.string :as str]
   [fastmath.signal :as signal]
   [scicloj.noj.v1.stats :as stats]
   [scicloj.noj.v1.vis.hanami :as hanami]
   [tablecloth.api :as tc]
   [tech.v3.dataset.rolling :as ds-rolling]
   [tech.v3.datatype.rolling :as dtype-rolling]
   [london-clojurians-april-2024.util :as util]))

(def book-sales
  (tc/dataset "data/book_sales.csv" {:key-fn (comp keyword str/lower-case)}))

(tc/info book-sales)

(tc/head book-sales)

(-> book-sales
    ;; they're already sorted, but just to be sure
    (tc/order-by :date :asc)
    (tc/add-column :time (range (tc/row-count book-sales))))

;; Time

(-> book-sales
    ;; they're already sorted, but just to be sure
    (tc/order-by :date :asc)
    (tc/add-column :time (range (tc/row-count book-sales)))
    (stats/add-predictions :hardcover [:time]
                           {:model-type :smile.regression/ordinary-least-square})
    ;; (tc/map-columns :target [:])
    (hanami/combined-plot ht/layer-chart
                          {:X :time
                           :YSCALE {:zero false}
                           :TITLE "Time plot of book sales"}
                          :LAYER [[ht/point-layer {:Y :hardcover
                                                   :MCOLOR "black"}]
                                  [ht/line-layer {:Y :hardcover
                                                  :MCOLOR "grey"
                                                  :MSIZE 1}]
                                  [ht/line-layer {:Y :hardcover-prediction}]]))

;; Lag

(-> book-sales
    (ds-rolling/rolling {:window-type :fixed
                         :window-size (inc 1)
                         :relative-window-position :left}
                        {:lag (ds-rolling/first :hardcover)})
    ;; (util/regplot {:x :lag
    ;;                :y :hardcover
    ;;                : false})
    (stats/add-predictions :hardcover [:lag]
                           {:model-type :smile.regression/ordinary-least-square})
    (hanami/combined-plot ht/layer-chart
                          {:X :lag
                           :YSCALE {:zero false}
                           :XSCALE {:zero false}
                           :WIDTH 400
                           :TITLE "Lag plot of book sales"}
                          :LAYER [[ht/point-layer {:Y :hardcover
                                                   :MCOLOR "black"}]
                                  ;; [ht/line-layer {:Y :hardcover
                                  ;;                 :MCOLOR "grey"
                                  ;;                 :MSIZE 1}]
                                  [ht/line-layer {:Y :hardcover-prediction}]]))


;; There are two kinds of features unique to time series: time-step features and lag features.

;; Time-step features are features we can derive directly from the time index. The most basic time-step feature is the time dummy, which counts off time steps in the series from beginning to end.

;; A time series has serial dependence when an observation can be predicted from previous observations. In Hardcover Sales, we can predict that high sales on one day usually mean high sales the next day.

;; Adapting machine learning algorithms to time series problems is largely about feature engineering with the time index and lags. For most of the course, we use linear regression for its simplicity, but these features will be useful whichever algorithm you choose for your forecasting task.

;; (defn kebab [word]
;;   (str/split word #"[A-Z]"))



(def tunnel
  (tc/dataset "data/tunnel.csv" {:key-fn (comp keyword str/lower-case)}))

(tc/info tunnel)

(tc/head tunnel)

(tc/column-names tunnel)

;; ## Tunnel example

(-> tunnel
    ;; they're already sorted, but just to be sure
    (tc/order-by :day :asc)
    (tc/add-column :time (range (tc/row-count tunnel)))
    (stats/add-predictions :numvehicles [:time]
                           {:model-type :smile.regression/ordinary-least-square})
    (hanami/combined-plot ht/layer-chart
                          {:X :day
                           :XTYPE :temporal
                           :YSCALE {:zero false}
                           :XSCALE {:zero false}
                           :TITLE "Time plot of tunnel traffic"}
                          :LAYER [[ht/point-layer {:Y :numvehicles
                                                   :MCOLOR "black"
                                                   :MSIZE 15}]
                                  [ht/line-layer {:Y :numvehicles
                                                  :MCOLOR "grey"
                                                  :MSIZE 1}]
                                  [ht/line-layer {:Y :numvehicles-prediction}]]))

(-> tunnel
    (tc/order-by :day :asc)
    (ds-rolling/rolling {:window-type :fixed
                         :window-size (inc 1)
                         :relative-window-position :left}
                        {:lag (ds-rolling/first :numvehicles)})
    ;; (stats/add-predictions :numvehicles [:lag]
    ;;                        {:model-type :smile.regression/ordinary-least-square})
    ;; (hanami/plot (-> ht/layer-chart
    ;;                  (assoc :LAYER [ht/line-layer
    ;;                                 ht/point-layer]))
    ;;              )
    ;; (hanami/combined-plot ht/layer-chart
    ;;                       {:X :lag
    ;;                        :YSCALE {:zero false}
    ;;                        :XSCALE {:zero false}
    ;;                        :WIDTH 300
    ;;                        :TITLE "Lag plot of tunnel traffic"}
    ;;                       :LAYER [[ht/point-layer {:Y :numvehicles
    ;;                                                :MCOLOR "black"
    ;;                                                :MSIZE 15}]
    ;;                               [ht/line-layer {:Y :numvehicles-prediction}]])
    )

;; ## Trend

;; - slowest moving part of series, a persistent, long-term change in the mean of the series
;; - can try to model a trend with a time-step feature once you've identified the shape of it

;; moving average plot

;; (def rolling-average
;;   (tc/dataset [["Rolling average"
;;                 (-> co2-over-time
;;                     (get "adjusted CO2")
;;                     (rolling/fixed-rolling-window 12
;;                                                   fun/mean
;;                                                   {:relative-window-position :left}))]]))

(-> tunnel
    (ds-rolling/rolling {:window-size 365
                         :relative-window-position :left}
                        {:average (ds-rolling/mean :numvehicles)})
    ;; (stats/add-predictions :numvehicles [:lag]
    ;;                        {:model-type :smile.regression/ordinary-least-square})
    (hanami/combined-plot ht/layer-chart
                          {:X :day
                           :XTYPE :temporal
                           :YSCALE {:zero false}
                           ;; :XSCALE {:zero false}
                           ;; :WIDTH 300
                           :TITLE "Tunnel traffic - 365 day moving average"}
                          :LAYER [[ht/point-layer {:Y :numvehicles
                                                   :MCOLOR "black"
                                                   :MSIZE 15}]
                                  [ht/line-layer {:Y :average}]]))

;; (let [signal [-1 2 4 99 4 2 -1]
;;       mov-avg (moving-average-filter 3)]
;;   (mov-avg signal))


;; (-> tunnel
;;     (tc/add-column :average (-> tunnel :numvehicles ((signal/moving-average-filter 365))))
;;     (hanami/combined-plot ht/layer-chart
;;                           {:X :day
;;                            :XTYPE :temporal
;;                            :YSCALE {:zero false}
;;                            :TITLE "Tunnel traffic - 365 day moving average"}
;;                           :LAYER [[ht/point-layer {:Y :numvehicles
;;                                                    :MCOLOR "black"
;;                                                    :MSIZE 15}]
;;                                   [ht/line-layer {:Y :average}]]))

(-> tunnel
    (tc/add-column :time (range (tc/row-count tunnel)))
    (stats/add-predictions :numvehicles [:time]
                           {:model-type :smile.regression/ordinary-least-square})
    (util/regplot :time :numvehicles ;; {:x :time
                  ;;  :y :numvehicles
                  ;;  :join-lines false
                  ;;  }
                  )
    (hanami/combined-plot ht/layer-chart
                          {:X :day
                           :XTYPE :temporal
                           :YSCALE {:zero false}
                           :TITLE "Tunnel traffic - 365 day moving average"}
                          :LAYER [[ht/point-layer {:Y :numvehicles
                                                   :MCOLOR "black"
                                                   :MSIZE 15}]
                                  [ht/line-layer {:Y :numvehicles-prediction}]])
    )

(-> tunnel
    (tc/add-column :time (range (tc/row-count tunnel)))
    (ds-rolling/rolling {:window-size 365
                         ;; this is important -- have to position the window left so you're only relying on past values to model future ones, this defaults to centre and that would not be useful for forecasting
                         :relative-window-position :left}
                        {:average (ds-rolling/mean :numvehicles)})
    (stats/add-predictions :numvehicles [:time]
                           {:model-type :smile.regression/ordinary-least-square})
    ;; (util/regplot :time :numvehicles ;; {:x :time
    ;;               ;;  :y :numvehicles
    ;;               ;;  :join-lines false
    ;;               ;;  }
    ;;               )
    (hanami/combined-plot ht/layer-chart
                          {:X :day
                           :XTYPE :temporal
                           :YSCALE {:zero false}
                           :HEIGHT 500
                           :WIDTH 1200
                           :TITLE "Tunnel traffic - 365 day moving average"}
                          :LAYER [[ht/point-layer {:Y :numvehicles
                                                   :MCOLOR "black"
                                                   :MSIZE 15}]
                                  [ht/line-layer {:Y :numvehicles-prediction
                                                  :MCOLOR "orange"}]
                                  [ht/line-layer {:Y :average}]]))

(-> book-sales
    (tc/add-column :time (range (tc/row-count book-sales)))
    ;; (util/regplot :time :hardcover {:WIDTH 400})
    )


;; tech.v3.datatype.functional> (def regressor (linear-regressor [1 2 3] [4 5 6]))
;; #'tech.v3.datatype.functional/regressor
;; tech.v3.datatype.functional> (regressor 1)
;; 4.0
;; tech.v3.datatype.functional> (regressor 2)
;; 5.0
;; tech.v3.datatype.functional> (meta regressor)
;; {:regressor
;;  #object[org.apache.commons.math3.stat.regression.SimpleRegression 0x52091e82 "org.apache.commons.math3.stat.regression.SimpleRegression@52091e82"],
;;  :intercept 3.0,
;;  :slope 1.0,
;;  :mean-squared-error 0.0}
(let [with-time-dummy (-> tunnel
                          (tc/add-column :time (range (tc/row-count tunnel))))
      regressor (dfn/linear-regressor (:time with-time-dummy) (:numvehicles with-time-dummy))]
  (-> with-time-dummy
      (tc/map-columns :numvehicles-prediction [:time] regressor)
      (hanami/combined-plot ht/layer-chart
                            {:X :day
                             :XTYPE :temporal
                             :YSCALE {:zero false}
                             :WIDTH 1000
                             :TITLE "Tunnel traffic - dtype next regressor"}
                            :LAYER [[ht/point-layer {:Y :numvehicles
                                                     :MCOLOR "black"
                                                     :MSIZE 15}]
                                    [ht/line-layer {:Y :numvehicles-prediction}]])))


;; out of sample

(let [ds-with-prediction (-> tunnel
                             (tc/add-column :time (range (tc/row-count tunnel)))
                             (stats/add-predictions :numvehicles [:time]
                                                    {:model-type :smile.regression/ordinary-least-square}))
      model (-> ds-with-prediction :numvehicles-prediction meta :model)
      last-day (-> tunnel :day last)
      next-100-days (->> (iterate #(.plusDays % 1) last-day) (take 100))
      forecasted-x (-> tunnel
                       (tc/concat (tc/dataset {:day next-100-days})))
      forecasted-predictions  (-> forecasted-x
                                  (tc/add-column :time (range (tc/row-count forecasted-x)))
                                  (dsmod/set-inference-target :numvehicles)
                                  (ml/predict model)
                                  (tc/rename-columns {:numvehicles :numvehicles-prediction}))
      with-predictions (-> forecasted-x
                           (tc/append forecasted-predictions)
                           (tc/map-columns :relative-time [:numvehicles]
                                           #(if % "Past" "Future")))]
  (hanami/combined-plot with-predictions ht/layer-chart
                        {:X :day
                         :XTYPE :temporal
                         :YSCALE {:zero false}
                         :WIDTH 1000
                         :TITLE "Tunnel traffic - 365 day moving average"}
                        :LAYER [[ht/point-layer {:Y :numvehicles
                                                 :MCOLOR "black"
                                                 :MSIZE 15}]
                                [ht/line-layer {:Y :numvehicles-prediction
                                                :COLOR {:field :relative-time}}]]))




;; Date dt = new Date ();
;; DateTime dtOrg = new DateTime (dt);
;; DateTime dtPlusOne = dtOrg.plusDays (1);




;; (let [train-ds (-> tunnel
;;                    (tc/select-columns [:numvehicles :time]))

;;       model (ml/train train-ds {:model-type :smile.regression/random-forest})
;;       predictions (ml/predict (clean-ds test) model)])


;; -- are you interested in designing some of these APIs? thinking about how to design APIs for statisticians? join us!
