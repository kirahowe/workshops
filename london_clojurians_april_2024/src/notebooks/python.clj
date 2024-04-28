(ns notebooks.python
  (:require
   [aerial.hanami.templates :as ht]
   [clojure.string :as str]
   [libpython-clj2.require :refer [require-python]]
   [scicloj.noj.v1.vis.hanami :as hanami]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]
   [tech.v3.dataset.rolling :as ds-rolling]
   [util :as util]))

;; (->> (re-seq #"[A-Z]?[^A-Z]*" s)
;;        (map str/lower-case)
;;        (remove empty?)
;;        (interpose "-")
;;        (apply str))
;; )

(defn pascal-to-kebab-case [s]
  (->> s
       (re-seq #"[A-Z]?[^A-Z]*")
       (map str/lower-case)
       (remove empty?)
       (str/join "-")))

(def us-retail-sales
  (tc/dataset "data/us-retail-sales.csv"
              {:key-fn (comp keyword pascal-to-kebab-case)}))

;; plot food and beverage sales:

(-> us-retail-sales
    (hanami/combined-plot ht/layer-chart
                          {:X :month
                           :XTYPE :temporal
                           :WIDTH 1200
                           :YSCALE {:zero false}
                           :TITLE "US Food and Beverage Sales"}
                          :LAYER [[ht/point-layer {:Y :food-and-beverage
                                                   :MCOLOR "black"}]
                                  [ht/line-layer {:Y :food-and-beverage
                                                  :MCOLOR "grey"
                                                  :MSIZE 1}]]))

;; add rolling average over each year and plot

(-> us-retail-sales
    (ds-rolling/rolling {:window-size 12
                         :relative-window-position :left}
                        {:average (ds-rolling/mean :food-and-beverage)})
    (hanami/combined-plot ht/layer-chart
                          {:X :month
                           :XTYPE :temporal
                           :WIDTH 1200
                           :YSCALE {:zero false}
                           :TITLE "US Food and Beverage Sales"}
                          :LAYER [[ht/point-layer {:Y :food-and-beverage
                                                   :MCOLOR "black"}]
                                  [ht/line-layer {:Y :food-and-beverage
                                                  :MCOLOR "grey"
                                                  :MSIZE 1}]
                                  [ht/line-layer {:Y :average}]]))


;; generate a dataset of overall average sales

(def store-sales
  (-> "data/store-sales.csv"
      (tc/dataset {:key-fn (comp keyword #(str/replace % "_" "-"))})))

(def average-sales
  (-> store-sales
      (tc/group-by [:date])
      (tc/aggregate {:avg-sales (comp tcc/mean :sales)})))

(-> average-sales
    ;; (tc/drop-rows 0)
    ;; (tc/drop-rows #(< (:avg-sales %) 10))
    (util/rolling-window-plot {:window-opts {:window-size 365
                                             :units :days}
                               :hanami-opts {:Y :avg-sales
                                             :X :date
                                             :XTYPE :temporal
                                             :WIDTH 1200
                                             :YSCALE {:zero false}
                                             :TITLE "Average sales"}}))

;; create a trend features

;; using clojure

(let [y average-sales
      clj-dp (-> average-sales
                 (tc/select-columns [:date])
                 (tc/add-column :trend (->> average-sales tc/row-count inc (range 1)))
                 (tc/map-columns :trend-squared [:trend] (fn [col] (tcc/* col col)))
                 (tc/map-columns :trend-cubed [:trend] (fn [col] (tcc/* col col col))))

      ;; X (py/py. dp in_sample)
      ;; X-forecast (py/py. dp out_of_sample 90)
      last-row (-> clj-dp (tc/select-rows (-> average-sales tc/row-count dec)))
      ;; forecase (-> clj-dp
      ;;              (tc/map-rows))

      ;; last-day (-> tunnel :day last)
      ;; next-100-days (->> (iterate #(.plusDays % 1) last-day) (take 100))
      ;; forecasted-x (-> tunnel
      ;;                  (tc/concat (tc/dataset {:day next-100-days})))
      ]

;; X-forecast
  last-row)


;; using python, we don't have a good equivalent of statsmodels  yet

(require-python '[statsmodels.tsa.deterministic :as statsd])
(require-python '[numpy :as np])
(require-python '[pandas :as pd])

;; (defn tablecloth-to-pandas [tablecloth-dataset]
;;   (let [python-obj (py/py.. pandas
;;                             DataFrame
;;                             (tc/columns tablecloth-dataset)
;;                             (tc/rows tablecloth-dataset))]
;;     python-obj))

(defn- df-to-ds [df]
  (let [index-col (-> df (py/py.- index) py/as-list tcc/column)
        rest-of-ds (-> df
                       py/->jvm
                       (tc/dataset {:key-fn (comp keyword #(str/replace % "_" "-"))})
                       (tc/update-columns :all (partial map second))
                       (tc/order-by :trend))]
    ;; (tc/append index-col rest-of-ds)
    (-> (tc/dataset {:date index-col})
        (tc/append rest-of-ds))))

(let [y average-sales
      clj-dp (-> average-sales
                 (tc/select-columns [:date])
                 (tc/add-column :trend (range (tc/row-count average-sales)))
                 (tc/map-columns :trend-squared [:trend] (fn [col] (tcc/* col col)))
                 (tc/map-columns :trend-cubed [:trend] (fn [col] (tcc/* col col col))))

      ;; this is why objects are terrible
      noidea (pd/to_datetime (->> (tc/convert-types average-sales :date :string)
                                  :date
                                  (apply vector))
                             :format "%Y-%m-%d" :errors "coerce")
      y-index (pd/PeriodIndex noidea :freq "D")
      dp (statsd/DeterministicProcess :index y-index :order 3)
      X (py/py. dp in_sample)
      X-forecast (py/py. dp out_of_sample 90)
      tc-in-sample (df-to-ds X)
      tc-forecast (df-to-ds X-forecast)]
  tc-forecast
  ;; (-> X py/->jvm)
  ;; tc-forecast
  )

;; (py/from-import statsmodels.tsa.deterministic DeterministicProcess)

;; (let [dp (statsd/DeterministicProcess {:index  :order 11})
;;       X (py/py. dp in_sample)]
;;   X)
