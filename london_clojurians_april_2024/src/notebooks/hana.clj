(ns notebooks.hana
  (:require [scicloj.kindly.v4.kind :as kind]
            [scicloj.noj.v1.paths :as paths]
            [scicloj.tempfiles.api :as tempfiles]
            [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [tech.v3.dataset :as ds]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [scicloj.metamorph.ml.toydata :as toydata]
            [tech.v3.datatype.functional :as fun]))


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


(defn xform [context]
  (let [{:keys [hana/stat]} (:args context)
        context1 (if stat
                   (stat context)
                   context)
        {:keys [template args metamorph/data]} context1]
    (-> template
        (hc/xform args)
        (cond-> data
          (assoc :data (prepare-data data)))
        kind/vega-lite)))

(defn layered-xform [{:as context
                      :keys [template args metamorph/data]}]
  (-> context
      (update :template
              safe-update
              :layer
              (partial
               mapv
               (fn [layer]
                 (-> layer
                     ;; merge the toplevel args
                     ;; with the layer's
                     ;; specific args
                     (update :args (partial merge args))
                     (update :metamorph/data #(or % data))
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


(defn layer-point
  ([context]
   (layer-point context {}))
  ([context args]
   (layer context point-layer args)))

(defn layer-line
  ([context]
   (layer-line context {}))
  ([context args]
   (layer context line-layer args)))

(def linreg-stat
  (fn [{:as context
        :keys [template args metamorph/data]}]
    (let [[X Y] (hc/xform [:X :Y] args)
          xs (data X)
          ys (data Y)
          model (fun/linear-regressor xs ys)]
      (assoc context
             :metamorph/data (-> {X [(tcc/reduce-min xs)
                                     (tcc/reduce-max xs)]}
                                 tc/dataset
                                 (tc/map-columns Y [X] model))))))

(defn layer-linreg
  ([context]
   (layer-linreg context {}))
  ([context args]
   (layer context
          line-layer
          (merge {:hana/stat linreg-stat}
                 args))))

(delay
  (-> (toydata/iris-ds)
      (plot point-chart
            {:X :sepal_width
             :Y :sepal_length})))

(delay
  (-> (toydata/iris-ds)
      (plot {:X :sepal_width
             :Y :sepal_length})
      layer-point
      layer-linreg))

(delay
  (-> (toydata/iris-ds)
      (plot {:TITLE "dummy"
             :MCOLOR "green"})
      (layer-point
       {:X :sepal_width
        :Y :sepal_length
        :MSIZE 100})
      (layer-line
       {:X :sepal_width
        :Y :sepal_length
        :MSIZE 4
        :MCOLOR "brown"})))
