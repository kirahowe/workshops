(ns notebooks.prepared
  (:require
   [clojure.string :as str]
   [libpython-clj2.python :as py]
   [libpython-clj2.require :refer [require-python]]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.metamorph.ml :as ml]
   [scicloj.metamorph.ml.loss :as loss]
   [scicloj.ml.tribuo]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]
   [tech.v3.dataset :as ds]
   [tech.v3.dataset.math :as math]
   [tech.v3.dataset.modelling :as modelling]
   [tech.v3.dataset.rolling :as rolling]
   [tech.v3.datatype.functional :as dfn]
   [utils.hana :as hana]
   [utils.util :as util]))

^:kindly/hide-code
(kind/hiccup [:style "
img {max-width: 100%}
svg {max-width: 100%}"])

;; # Clojure for Data Deep Dive
;; London Clojurians April 2024

;; ## Overview

;; There's a lot of material out there about using specific parts of this stack, and today we'll look at a few examples of problems that combine a lot of different components.
;; - data pre-processing
;;     - computing higher-order features
;;     - test/train splitting
;; - comparing ML models
;; - time series analysis
;;     - searching for trends
;;     - forecasting
;;

;; ## ML Basics in Clojure

;; A walkthrough of a realistic ML workflow using an example dataset about housing prices.

(def housing
  (tc/dataset "data/housing-data.csv"))

(tc/column-names housing)

;; ### Outliers

;; Plot outliers so we can remove them ([as recommended by the author](http://jse.amstat.org/v19n3/decock.pdf) of the original dataset)

(-> housing
    (hana/plot {:X "GrLivArea" :Y "SalePrice"})
    (hana/layer-point))

;; Remove the outliers (very large houses)

(-> housing
    (tc/drop-rows #(> (% "GrLivArea") 4000))
    (hana/plot {:X "GrLivArea" :Y "SalePrice"})
    (hana/layer-point))

(def without-outliers
  (-> housing
      (tc/drop-columns "Id")
      (tc/drop-rows #(> (% "GrLivArea") 4000))))

;; ### Processing

;; #### Removing missing values
;; First find columns that have missing values.

(-> without-outliers
    tc/info
    (tc/select-rows #(< 0 (:n-missing %)))
    :col-name)

;; Replace missing values with sensible alternative ones

(def no-missing-values
  (-> without-outliers
      (tc/replace-missing ["LotFrontage" "MasVnrArea"] 0)
      (tc/replace-missing ["Alley" "MasVnrType"] "None")
      (tc/replace-missing ["BsmtQual" "BsmtCond" "BsmtExposure" "BsmtFinType1" "BsmtFinType2"
                           "Fence" "FireplaceQu" "GarageType" "GarageFinish" "GarageYrBlt"
                           "GarageQual" "GarageCond" "MiscFeature" "PoolQC"] "No")
      (tc/drop-rows #(nil? (% "Electrical")))))

;; #### Converting to numbers
;; Convert string columns to numeric ones so they can be handled by our models. Finding all string columns, and then assigning them numeric values.

(def numeric-values-only
  (let [string-col-names (-> no-missing-values
                             tc/info
                             (tc/select-rows #(= :string (:datatype %)))
                             :col-name)]
    (-> no-missing-values
        (ds/categorical->number string-col-names))))

;; #### Combining features
;; Linear combinations based on semantic understanding of the dataset

(def with-linear-combinations
  (-> numeric-values-only
      (tc/map-columns "OverallGrade" ["OverallCond" "OverallQual"] tcc/*)
      (tc/map-columns "GarageGrade" ["GarageQual" "GarageCond"] tcc/*)
      (tc/map-columns "ExterGrade" ["ExterQual" "ExterCond"] tcc/*)
      (tc/map-columns "KitchenScore" ["KitchenAbvGr" "KitchenQual"] tcc/*)
      (tc/map-columns "FireplaceScore" ["Fireplaces" "FireplaceQu"] tcc/*)
      (tc/map-columns "GarageScore" ["GarageArea" "GarageQual"] tcc/*)
      (tc/map-columns "PoolScore" ["PoolArea" "PoolQC"] tcc/*)
      (tc/map-columns "TotalBath" ["BsmtFullBath" "BsmtHalfBath"
                                   "FullBath" "HalfBath"]
                      (fn [bsmt-full-bath bsmt-half-bath full-bath half-bath]
                        (tcc/+ bsmt-full-bath (tcc/* 0.5 bsmt-half-bath)
                               full-bath (tcc/* 0.5 half-bath))))
      (tc/map-columns "AllSF" ["GrLivArea" "TotalBsmtSF"] tcc/+)
      (tc/map-columns "AllFlrsSF" ["1stFlrSF" "2ndFlrSF"] tcc/+)
      (tc/map-columns "AllPorchSF" ["OpenPorchSF" "EnclosedPorch"
                                    "3SsnPorch" "ScreenPorch"] tcc/+)))

;; See correlations between variables

(def correlations
  (-> with-linear-combinations
      math/correlation-table
      (get "SalePrice")))

(->> correlations
     (sort-by second >)
     (take 40))

;; Polynomial combinations of highly correlated features -- take just the top 10

(def with-polynomial-combinations
  (->> correlations
       (sort-by second >)
       (drop 1)
       (take 10)
       (map first)
       (reduce (fn [ds column-name]
                 (-> ds
                     (tc/map-columns (str column-name "-s2") [column-name] tcc/sq)
                     (tc/map-columns (str column-name "-s3") [column-name] #(tcc/pow % 3))
                     (tc/map-columns (str column-name "-sqrt") [column-name] tcc/sqrt)))
               with-linear-combinations)))

(->> (-> with-polynomial-combinations
         math/correlation-table
         (get "SalePrice"))
     (sort-by second >)
     (take 20))

;; #### Comparing regression models

(let [model-configs [util/tribuo-cart-config
                     {:model-type :smile.regression/random-forest}
                     {:model-type :smile.regression/gradient-tree-boost}]
      trainable-ds (-> with-polynomial-combinations
                       (modelling/set-inference-target "SalePrice"))
      models (map (partial ml/train trainable-ds) model-configs)
      predictions (map (partial ml/predict trainable-ds) models)]
  (map (fn [prediction]
         {:mae (loss/mae (prediction "SalePrice") (trainable-ds "SalePrice"))
          :rmse (loss/rmse (prediction "SalePrice") (trainable-ds "SalePrice"))})
       predictions))

;; ## Modelling time series

(def tunnel
  (tc/dataset "data/tunnel.csv" {:key-fn (comp keyword str/lower-case)}))

(tc/info tunnel)

(tc/head tunnel)

;; ### Basic but unique time series features

;; Two kinds of features unique to time series -- time-step features and lag features. Applying ML models to time series mostly involves deriving usable features from the time index.

;; Most basic time step feature is a time dummy, just a step counter that increments for each row. Time-step features let you model time dependence, to see whether values can be predicted based on the time they occurred.

(def tunnel-with-time-dummy
  (-> tunnel
      (tc/order-by :day :asc)
      (tc/add-column :time (range (tc/row-count tunnel)))))

(let [regressor (dfn/linear-regressor (:time tunnel-with-time-dummy) (:numvehicles tunnel-with-time-dummy))]
  (-> tunnel-with-time-dummy
      (tc/map-columns :predictions [:time] regressor)
      (hana/plot {:X :day
                  :Y :numvehicles
                  :XTYPE :temporal
                  :TITLE "Time plot of tunnel traffic"
                  :YSCALE {:zero false}
                  :WIDTH 1200})
      (hana/layer-point {:MCOLOR "black"
                         :MSIZE 20})
      (hana/layer-line {:Y :predictions})))

;; Another basic feature we can derive is lag. Lag features let you model serial dependence, to see whether an observation can be predicted from previous observations.

(-> tunnel
    (tc/order-by :day :asc)
    (rolling/rolling {:window-type :fixed
                      :window-size 2
                      :relative-window-position :left}
                     {:lag (rolling/first :numvehicles)})
    (hana/plot {:X :lag
                :Y :numvehicles
                :XTYPE :temporal
                :TITLE "Lag plot of tunnel traffic"
                :YSCALE {:zero false}})
    (hana/layer-point {:MCOLOR "black"
                       :MSIZE 20})
    (hana/layer-smooth {:X-predictors [:lag]}))


(-> tunnel-with-time-dummy
    (hana/plot {:X :day
                :Y :numvehicles
                :XTYPE :temporal
                :YSCALE {:zero false}
                :XSCALE {:zero false}
                :TITLE "Time plot of tunnel traffic"})
    (hana/layer-point {:MCOLOR "black"
                       :MSIZE 15})
    (hana/layer-line {:MCOLOR "grey"
                      :MSIZE 1})
    (hana/layer-smooth {:X-predictors [:time]}))

;; ### Trends

;; A trend is slowest moving part of series -- a persistent, long-term change in the mean of the series. Can try to model a trend with a time-step feature once you've identified the shape of it. A moving average can help you detect the shape of a trend.

(require-python '[statsmodels.tsa.deterministic :as statsd])

(def store-sales
  (tc/dataset "data/store-sales.nippy"))

(tc/info store-sales)

(-> store-sales :date distinct)

(def average-sales
  (-> store-sales
      (tc/group-by [:date])
      (tc/aggregate {:avg-sales (comp tcc/mean :sales)})))

(-> average-sales
    (tc/order-by :date :asc)
    (rolling/rolling {:window-size 365
                      :window-type :variable
                      :column-name :date
                      :units :days
                      :relative-window-position :left}
                     {:trend (rolling/mean :avg-sales)})
    (hana/plot {:X :date
                :XTYPE :temporal
                :WIDTH 1200})
    (hana/layer-point {:Y :avg-sales
                       :MCOLOR "grey"
                       :MSIZE 20})
    (hana/layer-line {:Y :trend}))

;; ### Basic forecasting

(let [last-day (-> tunnel :day last)
      future-size 100
      ;; this is not really how it's done
      future-days (->> (iterate #(.plusDays % 1) last-day) (take future-size))]
  (-> tunnel-with-time-dummy
      (tc/concat (tc/dataset {:day future-days
                              :numvehicles (repeat future-size nil)}))
      (tc/add-column :trend #(-> % tc/row-count range))
      (tc/map-columns :relative-time [:numvehicles] #(if % "Past" "Future"))
      (hana/plot {:X :day
                  :XTYPE :temporal
                  :Y :numvehicles
                  :YSCALE {:zero false}
                  :WIDTH 1200
                  :TITLE "Tunnel traffic forecasting"})
      (hana/layer-point {:Y :numvehicles
                         :MCOLOR "grey"})
      (hana/layer-smooth {:X-predictors [:trend]
                          :COLOR {:field :relative-time}})))

;; Higher-order forecast

(let [last-day (-> tunnel :day last)
      future-size 100
      future-days (->> (iterate #(.plusDays % 1) last-day) (take future-size))]
  (-> tunnel-with-time-dummy
      (tc/concat (tc/dataset {:day future-days
                              :numvehicles (repeat future-size nil)}))
      (tc/add-column :trend #(-> % tc/row-count range))
      (tc/map-columns :trend-squared [:trend] tcc/sq)
      (tc/map-columns :trend-cubed [:trend] #(tcc/pow % 3))
      (tc/map-columns :relative-time [:numvehicles] #(if % "Past" "Future"))
      (hana/plot {:X :day
                  :XTYPE :temporal
                  :Y :numvehicles
                  :YSCALE {:zero false}
                  :WIDTH 1200
                  :TITLE "Tunnel traffic"})
      (hana/layer-point {:Y :numvehicles
                         :MCOLOR "grey"})
      (hana/layer-smooth {:X-predictors [:trend-cubed]
                          :COLOR {:field :relative-time}})))

;; ### Python interop

(require-python '[statsmodels.tsa.deterministic :as statsd])
(require-python '[pandas :as pd])

(let [time-index (pd/to_datetime (->> (tc/convert-types average-sales :date :string)
                                      :date
                                      (apply vector))
                                 :format "%Y-%m-%d")
      period-index (pd/PeriodIndex time-index :freq "D")
      dp (statsd/DeterministicProcess :index period-index :order 3)
      ;; dp.in_sample()
      X (py/py. dp in_sample)
      ;; dp.out_of_sample(90)
      X-forecast (py/py. dp out_of_sample 180)
      ds-in-sample (-> X
                       util/df-to-ds
                       (tc/convert-types :date :string)
                       (tc/full-join (-> average-sales (tc/convert-types :date :string)) :date))
      model (-> ds-in-sample
                (modelling/set-inference-target :avg-sales)
                (tc/select-columns [:trend :avg-sales])
                (ml/train util/tribuo-linear-sgd-config))
      ds-with-forecast (-> X-forecast
                           util/df-to-ds
                           (tc/concat ds-in-sample)
                           (tc/order-by :trend :asc))
      predictions (-> ds-with-forecast
                      (modelling/set-inference-target :avg-sales)
                      (ml/predict model)
                      (tc/rename-columns {:avg-sales :avg-sales-prediction}))
      with-predictions (-> (tc/append ds-with-forecast predictions))]
  (-> with-predictions
      (hana/plot {:X :date
                  :XTYPE :temporal
                  :YSCALE {:zero false}
                  :WIDTH 1200
                  :TITLE "Average sales trend"})
      (hana/layer-point {:Y :avg-sales
                         :MCOLOR "grey"
                         :MSIZE 15})
      (hana/layer-line {:Y :avg-sales-prediction})))

;; Are you interested in designing some of these APIs? Thinking about how to design APIs for statisticians? Join us!
