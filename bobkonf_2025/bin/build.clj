(ns build
  "Build script for the Bobkonf 2025 workshop

  Build the quarto publishable book:
  clojure -X build/book"

  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [scicloj.clay.v2.api :as clay]))

(def output-dir "book")

(defn book [_]
  (clay/make! {:format [:quarto :html]
               :book {:title "Reproducible Data Science with Clojure"
                      :page-navigation true}
               :show false
               :run-quarto false
               :base-source-path "src"
               :base-target-path output-dir
               :subdirs-to-sync ["src" "data"]
               :source-path (->> (io/file "src/notebooks")
                                 file-seq
                                 (filter #(str/ends-with? % "clj"))
                                 sort
                                 (map str)
                                 (map #(str/replace % #"src/" ""))
                                 (cons "index.clj"))
               :clean-up-target-dir true})
  (System/exit 0))
