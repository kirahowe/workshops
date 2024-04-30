(ns utils.util
  (:require
   [aerial.hanami.common :as hc]
   [aerial.hanami.templates :as ht]
   [clojure.string :as str]
   [libpython-clj2.python :as py]
   [scicloj.noj.v1.stats :as stats]
   [scicloj.noj.v1.vis.hanami :as hanami]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]
   [tech.v3.dataset.rolling :as ds-rolling]))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def smile-random-forest-config
  {:model-type :smile.regression/random-forest})

(defn regplot [ds x y]
  ;; (let [{:keys [X Y ]}])
  (-> ds
      (stats/add-predictions y [x]
                             {:model-type :smile.regression/ordinary-least-square})
      (hanami/combined-plot ht/layer-chart

                            {:X x
                             :XSCALE {:zero false}
                             :YSCALE {:zero false}}
                            :LAYER [[ht/point-layer {:Y y
                                                     :MCOLOR "black"}]
                                    ;; [ht/line-layer {:Y y
                                    ;;                 :MCOLOR "grey"
                                    ;;                 :MSIZE 1}]
                                    [ht/line-layer {:Y (-> y name (str "-prediction") keyword)}]])))

(defn rolling-window-plot [ds {:keys [window-opts hanami-opts]}]
  (let [{:keys [Y X]} hanami-opts]
    (-> ds
        (tc/order-by X)
        (ds-rolling/rolling (merge {:window-type :variable
                                    :column-name X
                                    :relative-window-position :left}
                                   window-opts)

                            {:average (ds-rolling/mean Y)})
        (hanami/combined-plot ht/layer-chart
                              hanami-opts
                              :LAYER [[ht/point-layer {:Y Y
                                                       :MCOLOR "black"}]
                                      [ht/line-layer {:Y Y
                                                      :MCOLOR "grey"
                                                      :MSIZE 1}]
                                      [ht/line-layer {:Y :average}]]))))

;; (defn smooth [ds ]
;;   (-> ds
;;       (stats/add-predictions)
;;       (hanami/line-layer)))

(defn out-of-sample [ds model]
  )
