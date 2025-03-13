(ns utils.utils
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(defn decrypt-gpg-key [file-name]
  (->> file-name (sh/sh "gpg" "--decrypt") :out str/trim))
