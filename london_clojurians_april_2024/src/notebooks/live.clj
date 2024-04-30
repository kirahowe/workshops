(ns notebooks.live
  (:require
   [tablecloth.api :as tc]
   [scicloj.kindly.v4.kind :as kind]))

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
;; ### Outliers
;; ### Processing
;; #### Removing missing values
;; #### Converting to numbers
;; #### Combining features
;; #### Comparing regression models
;; ## Modelling time series
;; ### Basic but unique time series features
;; ### Trends
;; ### Basic forecasting
;; ### Python interop
