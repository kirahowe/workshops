(ns build
  "Build script for the Bobkonf 2025 workshop

  Build the quarto publishable book:
  clojure -X build/book"

  (:require
   [scicloj.clay.v2.api :as clay]))

(def output-dir "book")

(defn book [_]
  (clay/make! {:format [:quarto :html]
               :book {:title "Reproducible Data Science with Clojure"}
               :show false
               :run-quarto false
               :base-source-path "src"
               :base-target-path output-dir
               :subdirs-to-sync ["src" "data"]
               :source-path ["index.clj"
                             "notebooks/1_installation_guide.clj"
                             "notebooks/2_getting_started_with_clojure.clj"] #_(->> (io/file "src/notebooks")
                                                                          file-seq
                                                                          (filter #(str/ends-with? % "clj"))
                                                                          sort)
               :clean-up-target-dir true})
  (System/exit 0))
