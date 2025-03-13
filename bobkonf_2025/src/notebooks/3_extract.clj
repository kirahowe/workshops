(ns notebooks.3-extract
  (:require
   [clj-http.client :as http]
   [clojure.java.io :as io]))

;; # Acquiring the data

;; The first step of a data analysis is always to get a hold of the data. For the purposes of this workshop, the cleaned data is already available in `data/prepared/final-dataset.gzip` and we'll start from there. For an in-depth exploration of how to download a dataset from the internet and load an excel spreadsheet as a dataset, the details of how that data was collected are described below:

;; ## Loading data from the internet

;; First we'll download the dataset from the source.

;; ::: {.callout-note}
;; **Note:** If you haven't already done this and the internet is slow or the download is not working for some reason, the file is also included in this repo already as `data/prepared/raw-data.xlsx`
;; :::

;; Our dataset is described on the [Berlin open data portal](https://daten.berlin.de/datensaetze/radzahldaten-in-berlin).

;; ::: {.callout-tip}
;; We should always check first that a given dataset is appropriately licensed for our use case. This dataset is licensed under the permissive [DL-DE->Zero-2.0 license](https://www.govdata.de/dl-de/zero-2-0), so we are free to use it without restrictions or conditions.
;; :::

;; The dataset is available using this link:
(def dataset-source-url "https://www.berlin.de/sen/uvk/_assets/verkehr/verkehrsplanung/radverkehr/weitere-radinfrastruktur/zaehlstellen-und-fahrradbarometer/gesamtdatei-stundenwerte.xlsx")

(def raw-data-file-name "data/prepared/raw-data.xlsx")

;; To download the dataset file, we'll use `clj-http` to stream the file contents to our local disk. The quickest way is to just stream the data directly to a file with a hard-coded name. This helper will download a file to the given destination:

(defn download-dataset
  "Streams a file from the given URL to the given destination."
  [url file-name]
  (let [response (http/get url {:as :byte-array})]
    (io/make-parents file-name)
    (with-open [output (io/output-stream file-name)]
      (io/copy (:body response) output))))

;; ::: {.callout-tip}
;; Executing `download-dataset` will actually download the dataset, which is 16.1MB. If you are on a slow or limited-bandwidth connection, you may prefer to skip evaluating it and use the data that is already downloaded to this repo, in `data/raw-data.xlsx`
;; :::

(comment
  (download-dataset dataset-source-url raw-data-file-name))

;; Now we have our raw data downloaded. For now we'll go ahead with the next steps, ands see how to make this more robust at the end. The next step would be to parse the Excel file and transform it into a format that's easier to work with for our analysis. We'll cover that in the next section.
