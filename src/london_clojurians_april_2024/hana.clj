(ns london-clojurians-april-2024.hana
  (:require [scicloj.kindly.v4.kind :as kind]
            [scicloj.noj.v1.paths :as paths]
            [scicloj.tempfiles.api :as tempfiles]
            [tablecloth.api :as tc]
            [tech.v3.dataset :as ds]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [scicloj.metamorph.ml.toydata :as toydata]))


(defn build [[f arg]]
  (f arg))

(defn prepare-data [dataset]
  (when dataset
    (let [{:keys [path _]}
          (tempfiles/tempfile! ".csv")]
      (-> dataset
          (ds/write! path))
      {:values (slurp path)
       :format {:type "csv"}})))

(defn safe-update [m k f]
  (if (m k)
    (update m k f)
    m))

(defn xform [{:as context
              :keys [template args]}]
  (let [dataset (:metamorph/data context)]
    (-> template
        (hc/xform args)
        (cond-> dataset
          (assoc :data (prepare-data dataset)))
        kind/vega-lite)))

(defn layered-xform [{:as context
                      :keys [template args]}]
  (-> context
      (update :template
              safe-update
              :layer
              (partial
               mapv
               (fn [layer]
                 (-> layer
                     (update :args
                             ;; merge the toplevel args
                             ;; with the layer's
                             ;; specific args
                             (partial merge args))
                     xform))))
      xform))

(defn svg-rendered [vega-lite-template]
  (assoc vega-lite-template
         :usermeta
         {:embedOptions {:renderer :svg}}))

(def view-base (svg-rendered ht/view-base))
(def point-chart (svg-rendered ht/point-chart))
(def line-chart (svg-rendered ht/line-chart))
(def point-layer ht/point-layer)
(def line-layer ht/line-layer)


(defn plot
  ([dataset args]
   (plot dataset
         view-base
         args))
  ([dataset template args]
   (kind/fn [layered-xform
             {:metamorph/data dataset
              :template template
              :args args}])))

(defn layer
  ([context template args]
   (if (tc/dataset? context)
     (layer (plot context {})
            template
            args)
     (-> context
         (update
          1
          update
          :template
          update
          :layer
          (comp vec conj)
          {:template template
           :args args})))))


(delay
  (-> (toydata/iris-ds)
      (plot point-chart
            {:X :sepal_width
             :Y :sepal_length})))

(delay
  (-> (toydata/iris-ds)
      (plot {:TITLE "dummy"
             :MCOLOR "green"})
      (layer point-layer
             {:X :sepal_width
              :Y :sepal_length
              :MSIZE 100})
      (layer line-layer
             {:X :sepal_width
              :Y :sepal_length
              :MSIZE 4
              :MCOLOR "brown"})))
