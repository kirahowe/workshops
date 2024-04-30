(ns notebooks.prepared
  (:require
   [clojure.string :as str]
   [libpython-clj2.require :refer [require-python]]
   [notebooks.hana :as hana]
   [scicloj.metamorph.ml :as ml]
   [scicloj.metamorph.ml.loss :as loss]
   [scicloj.ml.tribuo]
   [scicloj.noj.v1.stats :as stats]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]
   [tech.v3.dataset :as ds]
   [tech.v3.dataset.math :as math]
   [tech.v3.dataset.modelling :as modelling]
   [tech.v3.dataset.rolling :as rolling]
   [tech.v3.datatype.functional :as dfn]
   [util :as util]))

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

(def book-sales
  (tc/dataset "data/book-sales.csv" {:key-fn (comp keyword str/lower-case)}))

(tc/info book-sales)

(tc/head book-sales)

;; Two kinds of features unique to time series -- time-step features and lag features. Applying ML models to time series mostly involves deriving usable features from the time index.

;; Most basic time step feature is a time dummy, just a step counter that increments for each row. Time-step features let you model time dependence, to see whether values can be predicted based on the time they occurred.

(def book-sales-with-time-dummy
  (-> book-sales
    ;; they're already sorted, but just to be sure
      (tc/order-by :date :asc)
      (tc/add-column :time (range (tc/row-count book-sales)))))

(let [regressor (dfn/linear-regressor (:time book-sales-with-time-dummy)
                                      (:hardcover book-sales-with-time-dummy))]
  (-> book-sales-with-time-dummy
      (tc/map-columns :predictions [:time] regressor)
      (hana/plot {:X :date
                  :Y :hardcover
                  :XTYPE :temporal
                  :TITLE "Time plot of book sales"
                  :YSCALE {:zero false}
                  })
      (hana/layer-point {:MCOLOR "black"
                         :MSIZE 20})
      (hana/layer-line {:Y :predictions})))

;; Another basic feature we can derive is lag. Lag features let you model serial dependence, to see whether an observation can be predicted from previous observations.

(-> book-sales
    (tc/order-by :date :asc)
    (rolling/rolling {:window-type :fixed
                      :window-size 2
                      :relative-window-position :left}
                     {:lag (rolling/first :hardcover)})
    (hana/plot {:X :lag
                :Y :hardcover
                :XTYPE :temporal
                :TITLE "Lag plot of book sales"
                :YSCALE {:zero false}})
    (hana/layer-point {:MCOLOR "black"
                       :MSIZE 20})
    (hana/layer-smooth {:X-predictors [:lag]})
      ;; (hana/layer-line {:Y :numvehicles-prediction})
    )


;; (let [with-lag
;;       ]
;;   (-> with-lag
;;       ;; (tc/map-columns :predictions [:lag] regressor)
;;       (hana/plot {:X :lag
;;                   :Y :numvehicles
;;                   :XTYPE :temporal
;;                   :TITLE "Time plot of tunnel traffic"
;;                   :YSCALE {:zero false}
;;                   :WIDTH 1200})
;;       (hana/layer-point {:MCOLOR "black"
;;                          :MSIZE 20})
;;       (hana/layer-smooth {:X-predictors [:lag]})
;;       ;; (hana/layer-line {:Y :numvehicles-prediction})
;;       ))

(def with-time-dummy
  (-> book-sales
      (tc/order-by :date :asc)
      (tc/add-column :time (range (tc/row-count book-sales)))))

(def tunnel
  (tc/dataset "data/tunnel.csv" {:key-fn (comp keyword str/lower-case)}))

(tc/info tunnel)

(tc/head tunnel)


(def with-time-dummy
  (-> tunnel
      (tc/order-by :day :asc)
      (tc/add-column :time (range (tc/row-count tunnel)))))

(let [regressor (dfn/linear-regressor (:time with-time-dummy) (:numvehicles with-time-dummy))]
  (-> with-time-dummy
      (tc/map-columns :predictions [:time] regressor)
      (hana/plot {:X :day
                  :Y :numvehicles
                  :XTYPE :temporal
                  :TITLE "Time plot of tunnel traffic"
                  :YSCALE {:zero false}
                  :WIDTH 1200})
      (hana/layer-point {:MCOLOR "black"
                         :MSIZE 20})
      ;; (hana/layer-smooth {:X-predictors [:time]})
      (hana/layer-line {:Y :predictions})
      ))


(let [with-lag (-> tunnel
                   (tc/order-by :day :asc)
                   (rolling/rolling {:window-type :fixed
                                     :window-size 2
                                     :relative-window-position :left}
                                    {:lag (rolling/first :numvehicles)})
                   (modelling/set-inference-target :numvehicles))
      ;; model (ml/train with-lag {:model-type :smile.regression/random-forest})
      ;; predictions (ml/predict )
      ]
  (-> with-lag
      ;; (tc/map-columns :predictions [:lag] regressor)
      (stats/add-predictions :numvehicles [:lag] {:model-type :smile.regression/gradient-tree-boost})
      (hana/plot {:X :day
                  :Y :numvehicles
                  :XTYPE :temporal
                  :TITLE "Time plot of tunnel traffic"
                  :YSCALE {:zero false}
                  :WIDTH 1200})
      (hana/layer-point {:MCOLOR "black"
                         :MSIZE 20})
      ;; (hana/layer-smooth {:X-predictors [:time]})
      (hana/layer-line {:Y :numvehicles-prediction})
      ))





(require-python '[statsmodels.tsa.deterministic :as statsd])

(-> with-time-dummy
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
