^:kindly/hide-code
(ns notebooks.8-distribute
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [notebooks.3-extract :as extract]))

;; # Distribute

;; To make this more robust there are a few things we'll want to add at each step, like logging, error handling, and some basic validation. We can easily build this on top of the functionality we already have:

;; First, downloading the dataset:

(defn download-dataset [url file-name]
  (try
    (log/info "Downloading dataset from " url)
    (extract/download-dataset url file-name)
    (log/info "Download complete: " file-name)
    {:success? true :file file-name}

    (catch Exception e
      (log/debug e)
      (let [message (ex-message e)]
        (log/error "Error downloading dataset: ")
        {:success? false :error message}))))

(defn check-download [file-name]
  (let [file (io/file file-name)]
    (cond
      (not (.exists file))
      {:valid? false :reason "File does not exist"}

      (zero? (.length file))
      {:valid? false :reason "File is empty"}

      :else
      {:valid? true :file-size (.length file)})))

;; There are many options for distributing the results of your data analysis, depending on your audience and goals. The simplest option is to publish your namespace as a notebook using Clay.

;; See `build.clj` for how to make a quarto book out of multiple namespaces

;; Since we have all of our code in one place, we can also collect it into a pipeline and run it on a schedule or on demand.
