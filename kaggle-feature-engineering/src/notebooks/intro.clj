(ns notebooks.intro
  (:require [tablecloth.api :as tc]))

;; The task for this dataset is to predict a concrete's compressive strength given its formulation.


(def concrete
  (tc/dataset "data/concrete.csv"))
