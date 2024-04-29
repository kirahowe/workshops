(ns util
  (:require
   [aerial.hanami.common :as hc]
   [aerial.hanami.templates :as ht]
   [scicloj.noj.v1.stats :as stats]
   [scicloj.noj.v1.vis.hanami :as hanami]
   [tablecloth.api :as tc]
   [tech.v3.dataset.rolling :as ds-rolling]))

(swap! hc/_defaults
       assoc
       :BACKGROUND "white"
       :WIDTH 500)

;; (def connected-point-plot
;;   {:transform :TRANSFORM
;;    :usermeta :USERDATA
;;    :config {:bar :CFGBAR :view :CGFVIEW :axis :CFGAXIS :range :CFGRANGE}
;;    :width :WIDTH
;;    :background :BACKGROUND
;;    :title :TITLE
;;    :layer [{:mark
;;             {:type "circle"
;;              :point :POINT
;;              :size :MSIZE
;;              :color :MCOLOR
;;              :stroke :MSTROKE
;;              :strokeDash :MSDASH
;;              :tooltip :MTOOLTIP
;;              :filled :MFILLED}
;;             :selection :SELECTION
;;             :transform :TRANSFORM
;;             :encoding :ENCODING}
;;            {:mark
;;             {:type "line"
;;              :point :POINT
;;              :size :MSIZE
;;              :color :MCOLOR
;;              :stroke :MSTROKE
;;              :strokeDash :MSDASH
;;              :tooltip :MTOOLTIP
;;              :filled :MFILLED}
;;             :selection :SELECTION
;;             :transform :TRANSFORM
;;             :encoding :ENCODING}]
;;    :resolve :RESOLVE
;;    :height :HEIGHT
;;    :data
;;    {:values :VALDATA :url :UDATA :sequence :SDATA :name :NDATA :format :DFMT}})

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
