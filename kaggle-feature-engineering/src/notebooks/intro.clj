(ns notebooks.intro
  (:require
   [scicloj.metamorph.ml :as ml]
   [scicloj.metamorph.ml.loss :as loss]
   [scicloj.ml.smile.regression]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]
   [tech.v3.dataset.modelling :as dsmod]))

;; The task for this dataset is to predict a concrete's compressive strength given its formulation.

(def concrete
  (tc/dataset "data/concrete.csv"))

(tc/head concrete)

;; Get a baseline error with the dataset itself
(let [
      prediction-target "CompressiveStrength"
      ds (-> concrete
             (dsmod/set-inference-target prediction-target))
      model (ml/train ds {:model-type :smile.regression/random-forest})
      predictions (ml/predict ds model)]
  (loss/mae (ds prediction-target) (predictions prediction-target)))

;; Add synthetic features and re-check the error
(let [prediction-target "CompressiveStrength"
      with-ratios (-> concrete
                      (tc/map-columns "FCRatio" ["FineAggregate" "CoarseAggregate"] tcc//)
                      (tc/map-columns "AggCmtRatio"
                                      ["CoarseAggregate" "FineAggregate" "Cement"]
                                      (fn [coarse fine cement]
                                        (tcc// (tcc/+ coarse fine) cement)))
                      (tc/map-columns "WtrCmtRatio" ["Water" "Cement"] tcc//)
                      (dsmod/set-inference-target prediction-target))
      model (ml/train with-ratios {:model-type :smile.regression/random-forest})
      predictions (ml/predict with-ratios model)]
  (loss/mae (with-ratios prediction-target) (predictions prediction-target)))

;; See that the error is better with our engineered features included
