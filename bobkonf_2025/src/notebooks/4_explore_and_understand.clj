(ns notebooks.4-explore-and-understand
  (:require
   [clojure.string :as str]
   [scicloj.tableplot.v1.plotly :as plotly]
   [tablecloth.api :as tc]
   [tablecloth.column.api :as tcc]
   [utils.dates :as dates]
   [utils.utils :as u]))

;; # Explore and understand

;; Once we have access to our raw data file, we'll want to explore it and see what we need to do to clean it up. This requires some initial understanding of what our dataset contains.
;;
;; We're starting with an excel workbook of bicycle traffic data in Berlin. To learn more about where we found this data and how to download it, see the ["Extract"](/bobkonf-2025/notebooks.extract.html) chapter that we skipped over. In the interest of time, we'll start here and learn:

;; - how to load an excel file as a tablecloth dataset
;; - how to explore and work with Excel workbooks that contain multiple sheets
;; - how to handle Excel's date format in Clojure
;; - how to extract metadata from reference sheets
;; - how to clean, transform, and pivot messy data to make [tidy data](https://tidyr.tidyverse.org/articles/tidy-data.html)

;; ## Loading xlsx data in Clojure

;; To load xlsx data we'll need to require fastexcel:

(require '[tech.v3.libs.fastexcel :as xlsx])

;; For consistency, we'll use the same file name as the one specified in the previous notebook, which means we need to require it, too:

(require '[notebooks.3-extract :as extract])

;; Attempting to load the excel file as a dataset directly shows us that there are actually 16 datasets contained in this workbook:
(tc/dataset extract/raw-data-file-name)

;; So instead we'll make a seq of datasets, loading each sheet as its own dataset:

(def raw-datasets
  (xlsx/workbook->datasets extract/raw-data-file-name))

;; We can see what our sheets are named like this:
(map tc/dataset-name raw-datasets)

;; The first three are reference datasets and the rest are a time series. It's pretty common to see Excel workbooks set up like this, with a few sheets that are reference datasets and the rest containing the actual data. We'll extract some metadata out of these first three datasets to use in our analysis. The first dataset just contains a big description, formatted for aesthetics rather than machine-readability, so we'll have to dig in to the one random column containing the text to get the content out.

(def info-ds (first raw-datasets))

;; First we'll drop all the columns where every value is nil, which we can see leaves us with a single column:

(tc/drop-columns info-ds (fn [col] (every? nil? (info-ds col))))

;; Then we'll join all the non-nil values in this column into a single wall of text, and store it as the description for this dataset.

(->> (tc/drop-columns info-ds (fn [col] (every? nil? (info-ds col))))
     tc/rows
     flatten
     (remove nil?)
     (map str/trim)
     (str/join "\n")
     (spit "data/prepared/description-de.txt"))

;; You'll notice this description is in German. As a quick bonus step, we can translate it to English using a free online translation API. To follow this particular example you'll need an API key from [DeepL](https://www.deepl.com/en/pro#developer).

(comment
  (require '[clj-http.client :as http]
           '[clojure.data.json :as json])

  (let [key (u/decrypt-gpg-key "secrets/translate-api-key.txt.gpg")
        response (http/post "https://api-free.deepl.com/v2/translate"
                            {:body (json/write-str {:text [(slurp "data/prepared/description-de.txt")]
                                                    :target_lang "EN"})
                             :headers {"Authorization" (str "DeepL-Auth-Key " key)}
                             :content-type :json})
        english (-> response :body json/read-str (get-in ["translations" 0 "text"]))]
    (spit "data/prepared/description-en.txt"
          (str "NOTE: THIS CONTENT IS A MACHINE TRANSLATION OF THE ORIGINAL IN data/prepared/description-de.txt\n"
               "provided only for convenience with no guarantee of accuracy.\n\n"
               english))))

;; Next we'll look at the "Legende" sheet to see what we can learn about our dataset from it.

(second raw-datasets)

;; This does not appear to be very useful. We'll come back to it if it seems like it might be interesting later on.

;; Next is the "Standortdaten" sheet:

(nth raw-datasets 2)

;; This is useful information about the different tracker locations. It also reveals our first encounter with one very annoying thing about working with Excel files -- dates. Very annoyingly, dates in excel are stored as the number of days since 1900-1-1. We'll fix this and do some other basic transformations to this dataset, then store it as a CSV file.

(def location-info-file-name "data/prepared/location-info.csv")

(-> raw-datasets
    (nth 2)
    ;; It's idiomatic to use keywords as column names in Clojure, so we'll translate these to English and make them keywords
    (tc/rename-columns {"Zählstelle" :station-id
                        "Beschreibung - Fahrtrichtung" :direction
                        "Breitengrad" :lat
                        "Längengrad" :long
                        "Installationsdatum" :installed})
    ;; We'll parse the installation date values as dates based on Excel's idiosyncratic date handling
    (tc/update-columns :installed (partial map dates/parse-excel-cell-as-date))
    ;; Also update the lat/long precision to be realistic (https://xkcd.com/2170/)
    (tc/update-columns [:lat :long] #(-> % (tcc/* 10000.0) tcc/round (tcc// 10000.0)))
    ;; Save this transformed data
    (tc/write-csv! location-info-file-name))

;; We can also quickly plot this to visualize the locations of the stations as our first introduction to [tableplot](https://scicloj.github.io/tableplot/), the visualization library we'll be using throughout this workshop. To overlay the points on a map of Berlin, we'll need a [Mapbox token](https://www.mapbox.com).
;;
;; Here's how we can see the locations of the stations on a map, starting from the cleaned up dataset we just saved:

(let [token (u/decrypt-gpg-key "secrets/mapbox-api-token.txt.gpg")]
  (-> location-info-file-name
      (tc/dataset {:key-fn keyword}) ;; Note this is how we convert the column names to keywords on load
      (plotly/base {:=coordinates :geo
                    :=lat :lat
                    :=lon :long
                    :=width 1000
                    :=height 700})
      (plotly/layer-point {:=name "Bike traffic counting stations"
                           :=mark-color "orange"
                           :=mark-size 10})
      plotly/plot
      (assoc-in [:data 0 :type] "scattermapbox")
      (assoc-in [:layout :mapbox]
                {:style "carto-positron"          ;; Choose a Mapbox style
                 :center {:lat 52.51, :lon 13.37} ;; Center the map on Berlin
                 :zoom 10.5                       ;; Adjust zoom level
                 :accesstoken token}))            ;; Use your Mapbox token
  )

;; Now we get into the interesting work. Inspecting one of the annual data files we can see that this dataset is stored in a very wide format with bad column names

(def data-2012 (nth raw-datasets 3))

data-2012

;; This data is a mess. The one saving grace is that all years of data are messy in the same way. We can see all of the timeseries datasets have the same column names:

(map tc/column-names raw-datasets)

;; We'll figure out how to parse a single dataset first here, then make it more robust and apply the transformations to every year.
;;
;; We can see the data is very sparse, there are columns for every station in every year, even though most stations were not installed in the early years.
;;
;; We'll clean up the data by dropping the empty columns, renaming the columns, and making it tidy (i.e. one variable per column and one observation per row):

(let [;; Get the names of all empty columns that are not empty
      non-empty-columns (-> data-2012
                            tc/info
                            (tc/select-rows (comp pos? :n-valid))
                            :col-name)]
  (-> data-2012
      ;; Select only those
      (tc/select-columns (set non-empty-columns))
      ;; Now fix the column names
      ;; First column is always the date
      (tc/rename-columns {"Zählstelle        Inbetriebnahme" :datetime})
      ;; Now we'll deal with the excel dates
      (tc/update-columns :datetime (partial map dates/parse-excel-cell-as-date))
      ;; Then we'll strip the date part off the end of the rest of the column names. Regex isn't super efficient, but that's fine we're only ever working with a small number of columns. Since these are domain-specific IDs, we'll leave them as-is, rather than converting them to keywords or something else.
      (tc/rename-columns (complement #{:datetime})
                         #(->> % (re-matches #"^(.+)\d{2}\.\d{2}\.\d{4}") second str/trim))

      ;; Now we'll make the data "tidy". It's not super obvious why this is necessary with a small dataset like this, but it will be for the ones that have more columns
      (tc/pivot->longer (complement #{:datetime}) {:target-columns :station-id
                                                   :value-column-name :count})))

;; Now that we've seen what kind of data we're working with, we'll look at how to apply these transformations systematically to the rest of the data.
