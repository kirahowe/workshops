(ns utils.util
  (:require
   [aerial.hanami.common :as hc]
   [clojure.string :as str]
   [libpython-clj2.python :as py]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]))

(swap! hc/_defaults
       assoc
       :BACKGROUND "white"
       :WIDTH 500)

(def tribuo-cart-config
  {:model-type :scicloj.ml.tribuo/regression
   :tribuo-components [{:name "trainer"
                        :type "org.tribuo.regression.rtree.CARTRegressionTrainer"}]
   :tribuo-trainer-name "trainer"})

(def tribuo-linear-sgd-config
  {:model-type :scicloj.ml.tribuo/regression
   :tribuo-components [{:name "squared"
                        :type "org.tribuo.regression.sgd.objectives.SquaredLoss"}
                       {:name "trainer"
                        :type "org.tribuo.regression.sgd.linear.LinearSGDTrainer"
                        :properties  {:epochs "100"
                                      :minibatchSize "1"
                                      :objective "squared"}}]
   :tribuo-trainer-name "trainer"})

(defn pascal-to-kebab-case [s]
  (->> s
       (re-seq #"[A-Z]?[^A-Z]*")
       (map str/lower-case)
       (remove empty?)
       (str/join "-")))

(defn df-to-ds [df]
  (let [index-col (-> df (py/py.- index) py/as-list tcc/column)
        rest-of-ds (-> df
                       py/->jvm
                       (tc/dataset {:key-fn (comp keyword #(str/replace % "_" "-"))})
                       (tc/update-columns :all (partial map second))
                       (tc/order-by :trend))]
    ;; (tc/append index-col rest-of-ds)
    (-> (tc/dataset {:date index-col})
        (tc/append rest-of-ds))))
