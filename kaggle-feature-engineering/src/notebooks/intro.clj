(ns notebooks.intro
  (:require [tablecloth.api :as tc]))

;; The task for this dataset is to predict a concrete's compressive strength given its formulation.

(def concrete
  (tc/dataset "data/concrete.csv"))

(tc/head concrete)

(def X (tc/drop-columns concrete ["CompressiveStrength"]))

(def y (tc/select-columns concrete ["CompressiveStrength"]))

;; (def baseline = )

;; baseline = RandomForestRegressor(criterion="absolute_error", random_state=0)
;; baseline_score = cross_val_score(
;;     baseline, X, y, cv=5, scoring="neg_mean_absolute_error"
;; )
;; baseline_score = -1 * baseline_score.mean()

;; print(f"MAE Baseline Score: {baseline_score:.4}")
