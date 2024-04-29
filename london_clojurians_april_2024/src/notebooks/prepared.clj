(ns notebooks.prepared
  (:require
   [notebooks.hana :as hana]
   [tablecloth.api :as tc]
   [tech.v3.dataset :as ds]
   [tech.v3.dataset.modelling :as modelling]))

;; # Clojure for Data Deep Dive
;; London Clojurians April 2024

;; ## Overview

;; There's a lot of material out there about using specific parts of this stack, and today we'll look at a few examples of problems that combine a lot of different components.
;; - data pre-processing
;;   - computing higher-order features
;;   - test/train splitting
;; - comparing ML models
;; - time series analysis
;;   - searching for trends
;;   - forecasting
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
      (tc/drop-rows #(> (% "GrLivArea") 4000))))

;; ## Processing

;; ### Removing missing values
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
                           "GarageQual" "GarageCond" "MiscFeature" "PoolQC"] "No")))

;; Convert string columns to numeric ones so they can be handled by our models. Finding all string columns, and then assigning them numeric values.

(let [string-col-names (-> no-missing-values
                           tc/info
                           (tc/select-rows #(= :string (:datatype %)))
                           :col-name)]
  (-> no-missing-values
      (ds/categorical->number string-col-names)))

(without-outliers "MoSold")
