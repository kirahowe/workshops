(ns notebooks.getting-started-with-clojure)

;; # Welcome to Clojure!

;; Clojure is a modern lisp that runs on the JVM, designed for simplicity, composability, and interactive development.

;; We'll quickly explore some syntax and key features of Clojure that make it excellent for working with data before we dive in to the purpose-built tools for more sophisticated problems.

;; ## 1. Data-first, data-only paradigm

;; In Clojure, data is a first-class concept. Code is data and data is code, there is no barrier between the two. Clojure's built-in data structures include lists, vectors, sets and maps. They are [immutable and efficient](https://clojure.org/about/functional_programming#_immutable_data_structures).

;; Lists: used for code and sequences

(def languages (list "Clojure" "Python" "Julia" "R"))

(first languages)
(rest languages)
(conj languages "JavaScript")

;; Vectors: ordered, indexable

(def conferences ["ClojureD" "Heart of Clojure" "Clojure/conj" "re:Clojure"])

(get conferences 1) ;; vectors are 0-indexed
(conj conferences "BobKonf")

;; Sets: unique elements, fast lookups

(def cities #{"London" "Berlin" "Brussels" "Alexandria"})

(contains? cities "Berlin")
(conj cities "Durham")

;; Maps: key-value data structure

(def conference
  {:name "BobKonf"
   :city "Berlin"
   :venue "Scandic Hotel Potsdamer Platz"
   :date "2025-03-14"})

;; Maps are functions:

(conference :date)

;; Keywords in Clojure are symbolic identifiers that evaluate to themselves:

:city

;; They are commonly used as keys in maps because they are also functions:

(:city conference)

;; ## 2. Functional data transformation

;; Rather than writing imperative loops, in Clojure we compose functions to transform data. Clojure has built-in threading operators, which allow us to write readable and easily-understandable pipelines for working on our data.

;; The thread-first operator (`->`) passes the output of the previous function as the first argument to the next. This is useful for working on a single value:

(-> conference
    :date
    (clojure.string/split #"-") ;; #"regex-pattern-here" is the syntax for defining a regex in Clojure
    first
    parse-long)

;; The thread-last operator (`->>`) passes the output of the previous function as the _last_ argument to the next. This is useful for working with collections:

(def attendees [{:name "Alice" :languages #{"Clojure" "Java"}}
                {:name "Bob" :languages #{"Python" "Java" "R"}}
                {:name "Charlie" :languages #{"Clojure" "Python"}}
                {:name "Daniel" :languages #{"Clojure" "R"}}])

;; TODO: example

;; Clojure comes with many useful built-in functions for operating on collections, most of which are based on either `map`, `filter`, or `reduce`. Combining these makes complex data transformations very succinct:

(def attendees [{:name "Alice" :languages #{"Clojure" "Java"}}
                {:name "Bob" :languages #{"Python" "Java" "R"}}
                {:name "Charlie" :languages #{"Clojure" "Python"}}
                {:name "Daniel" :languages #{"Clojure" "R"}}])

;; e.g. find all Clojure enthusiasts:
(->> attendees
     (filter #((:languages %) "Clojure"))
     (map :name))

;; TODO: example

;; ## 3. Lazy evaluation

;; Most collection operators like these in Clojure return lazy sequences, meaning the elements of the collection are not evaluated in advance or on the fly, they are only computed once they're needed. This has many benefits:
;; - Efficiency -- avoids unnecessary memory and compute usage
;; - Composability -- allows building complex data pipelines that process elements incrementally, i.e. passing one element at a time through the entire chain of operations without evaluating or reifying unnecessary intermediate collections
;; - Scalability -- allows for processing larger-than-memory datasets because elements are only processed as needed


;; We can write code as if we're working with infinite data, but Clojure will only compute what we actually need.

;; ## 4. Key takeaways

;; - Data is immutable by default, i.e. functions transform data, they do not modify it
;; - Everything is an expression, i.e. every operation returns a value
;; - Functions are composable
;; - Functions are pure by default, making code easier to reason about
;; - Most sequences are lazy, take advantage of them to write efficient, composable data transformations
;; - Clojure has a rich standard library for data manipulation
;; - REPL-driven development enables rapid iteration

;; TODO: examples here? more points?
