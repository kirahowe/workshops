(ns london-clojurians-april-2024.util
  (:require
   [aerial.hanami.templates :as ht]
   [aerial.hanami.common :as hc]
   [scicloj.noj.v1.stats :as stats]
   [scicloj.noj.v1.vis.hanami :as hanami]))

(swap! hc/_defaults
       assoc
       :BACKGROUND "white"
       :WIDTH 700)

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

;; (defn smooth [ds ]
;;   (-> ds
;;       (stats/add-predictions)
;;       (hanami/line-layer)))

(defn out-of-sample [ds model]
  )
