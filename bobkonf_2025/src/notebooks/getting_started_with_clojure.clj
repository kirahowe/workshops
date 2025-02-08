(ns notebooks.getting-started-with-clojure)

;; # Welcome to Clojure!

;; We'll quickly explore some syntax and key features of Clojure that make it excellent for working with data before we dive in to the purpose-built tools for more sophisticated problems.

;; ## 1. Data-first, data-only paradigm

;; In Clojure, data is a first-class concept. Code is data and data is code, there is no barrier between the two. Clojure's built-in data structures include lists, vectors, sets and maps. They are [immutable and efficient](https://clojure.org/about/functional_programming#_immutable_data_structures).

;; Lists:
(def cities (list "Berlin" "Köln" "München" "Freiburg"))

;; Vectors:
(def neighbourhoods ["Prenzlauer Berg"
                     "Kreuzberg"
                     "Mitte"
                     "Charlottenburg"
                     "Friedrichshain"
                     "Neukölln"
                     "Wedding"])

;; Sets:
(def states #{"Baden-Württemberg"
              "Bayern"
              "Berlin"
              "Brandenburg"
              "Bremen"
              "Hamburg"
              "Hessen"
              "Mecklenburg-Vorpommern"
              "Niedersachsen"
              "Nordrhein-Westfalen"
              "Rheinland-Pfalz"
              "Saarland"
              "Sachsen"
              "Sachsen-Anhalt"
              "Schleswig-Holstein"
              "Thüringen"})

;; Maps:
(def lines
  {:u1 {:label "U1" :colour "green" :direction :east-west :route ["Uhlandstraße" "Warschauer Straße"]}
   :u2 {:label "U2" :colour "red" :direction :east-west :route ["Pankow" "Ruhleben"]}
   :u3 {:label "U3" :colour "turquoise" :route ["Krumme Lanke" "Warschauer Straße"]}
   :u4 {:label "U4" :colour "yellow" :route ["Nollendorfplatz" "Innsbrucker Platz"]}
   :u5 {:label "U5" :colour "brown" :direction :east-west :route ["Hauptbahnhof" "Hönow"]}
   :u6 {:label "U6" :colour "purple" :direction :north-south :route ["Alt-Tegel" "Alt-Mariendorf"]}
   :u7 {:label "U7" :colour "blue" :direction :east-west :route ["Rathaus Spandau" "Rudow"]}
   :u8 {:label "U8" :colour "dark-blue" :direction :north-south :route ["Wittenau" "Hermannstraße"]}
   :u9 {:label "U9" :colour "orange" :direction :north-south :route ["Rathaus Steglitz" "Osloer Straße"]}})


;; ## 2. Functional data transformation

;; Rather than writing loops, we compose functions to transform data. Clojure has threading operators built-in, which allows us to write readable and easily-understandable pipelines for working on our data.

;; The thread-first operator (`->`) passes the output of the previous function as the first argument to the next. This is useful for working on a single value:

;; TODO: example

;; The thread-last operator (`->>`) passes the output of the previous function as the _last_ argument to the next. This is useful for working with collections:

;; TODO: example

;; Clojure comes with many useful built-in functions for operating on collections, most of which are based on either `map`, `filter`, or `reduce`.

;; ## 3. Key takeaways

;; - data is immutable by default, i.e. functions transform data, they do not modify it
;; - everything is an expression, i.e. every operation returns a value
;; - everything is composable

;; TODO: examples here? more points?
