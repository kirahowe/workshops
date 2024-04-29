(ns notebooks.prepared
  (:require
   [notebooks.hana :as hana]
   [tablecloth.api :as tc]))

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
  (tc/dataset "data/iowa-data.csv"))

(tc/column-names housing)

;; Plot outliers so we can remove them ([as recommended by the author](http://jse.amstat.org/v19n3/decock.pdf) of the original dataset)

(-> housing
    (hana/plot {:X "GrLivArea" :Y "SalePrice"})
    (hana/layer-point))

;; Remove the outliers (very large houses)

(-> housing
    (tc/drop-rows #(> (% "GrLivArea") 4000))
    (hana/plot {:X "GrLivArea" :Y "SalePrice"})
    (hana/layer-point))

;; Initial processing

(-> housing
    )

;; (-> housing
;;     (tc/drop-rows #(> (% "GrLivArea") 4000))

;;     )
