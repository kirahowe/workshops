^:kindly/hide-code
(ns notebooks.2-getting-started-with-clojure)

;; # Getting started with Clojure

;; ## Welcome!

;; Clojure is a modern lisp that runs on the JVM, designed for simplicity, composability, and interactive development. Its functional paradigm and immutable data structures make it particularly well-suited for data processing workflows.
;;
;; This is a practical guide on working with data in Clojure to introduce you to some key language concepts and syntax that will become very familiar as you work through the rest of this workshop. By the end of this tutorial, you'll understand:
;;
;; - Clojure's core data structures and their properties
;; - How to approach data transformation in a functional way
;; - The power of lazy evaluation for efficient data processing
;; - Practical patterns for data manipulation using only the standard library
;; - How these fundamentals apply to data science workflows

;; We'll quickly explore some syntax and key features of Clojure that make it excellent for working with data before we dive in to the purpose-built tools for more sophisticated problems. The essentials of the standard library will always be useful, even as you learn to use more powerful libraries for advanced data processing tasks.

;; ## 1. Data-first, data-only paradigm

;; In Clojure, data is a first-class concept. Code is data and data is code, with no barrier between the two. Clojure's built-in data structures include lists, vectors, sets and maps. They are [immutable and efficient](https://clojure.org/about/functional_programming#_immutable_data_structures).

;; ### Lists: used for code and sequences

(def languages (list "Clojure" "Python" "Julia" "R"))

(first languages)
(rest languages)
(conj languages "JavaScript")

;; Note that data structures are immutable. Adding something to a list creates a new list. The old one is never modified. This is true of all Clojure data structures. This still returns the original list:

languages

;; ### Vectors: ordered, indexable

(def conferences ["ClojureD" "Heart of Clojure" "Clojure/conj" "re:Clojure"])

;; Vectors are 0-indexed:
(get conferences 1)
(conj conferences "BobKonf")

;; ### Sets: unique elements, fast lookups

(def cities #{"London" "Berlin" "Brussels" "Alexandria"})

(contains? cities "Berlin")
(conj cities "Durham")

;; ### Maps: key-value data structure

(def conference
  {:name "BobKonf"
   :city "Berlin"
   :venue "Scandic Hotel Potsdamer Platz"
   :date "2025-03-14"})

(get conference :name)

;; Clojure's standard library includes a rich set of functions for manipulating maps:

(-> conference
    (assoc :attendees 10) ;; Add a key/value
    (dissoc :city) ;; Delete a key/value
    (update :attendees (partial + 10)) ;; Update a value
    )

;; ### Nested data structures

;; In real-world applications, you'll often see nested data structures that combine many of these together.

(def conference-data
  {:name "BobKonf"
   :location {:city "Berlin"
              :country "Germany"
              :venue "Scandic Hotel"}
   :tracks [{:name "Functional Programming"
             :talks 12}
            {:name "Data Science"
             :talks 8}]
   :attendees 100})

;; Accessing nested data is simple:
(get-in conference-data [:location :city])
(get-in conference-data [:tracks 0 :talks])

;; So is updating nested data:

(assoc-in conference-data [:location :landmark] "Potsdamer Platz")
(update-in conference-data [:tracks 1 :talks] + 2)

;; ::: {.callout-tip title="A note on persistence and immutability"}
;; Clojure's data structures are persistent, meaning they preserve previous versions when modified. This is achieved through structural sharing - instead of mutating existing data, new versions share most of their structure with the original, and only the changed parts are created anew, minimizing memory usage and computation. This is how operations can be completed in logarithmic or even constant time despite Clojure's immutability.
;; :::

;; ### File naming conventions in Clojure

;; - Clojure source files use the `.clj` extension
;; - Filenames use lowercase with underscores (snake_case): `data_processing.clj`
;; - Namespaces use dots as separators and match directory structure: `(ns my.project.data-processing)`

;; ::: {.callout-tip title="More than conventions"}
;; "Convention" might be a little too generous to describe these file naming requirements -- if a file name and namespace don't align, the Clojure runtime won't be able to require it properly. I.e. it expects a namespace foo.bar.baz to be defined in the file src/foo/bar/baz.clj.
;; :::

;; - Within namespaces, hyphens are used instead of underscores: `data-processing` not `data_processing`
;; - Test files typically end with `_test.clj` and are placed in a test directory wiht a structure that mirrors the `src` directory

;; ## 2. Functional data transformation

;; ### Data structures as functions

;; Most data structures in Clojure are also first-class functions.

;; Vectors are functions that look up by index, meaning this:
(conferences 2)

;; is equivalent to this:
(get conferences 2)

;; Sets are functions that act as membership test functions:
(cities "London")
(cities "Paris")

;; Maps are functions:
(conference :date)

;; And they're smart functions -- they can accept a second argument to use instead of `nil` for a missing value:
(conference :topic)
(conference :topic "Programming")

;; Keywords in Clojure are symbolic identifiers that evaluate to themselves:
:city

;; They are commonly used as keys in maps because they are also functions:
(:city conference)

;; This is equivalent to:
(get conference :city)

;; ### Data transformation with threading macros

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

;; e.g. find all Clojure enthusiasts:
(->> attendees
     (filter #((:languages %) "Clojure"))
     (map :name))


;; ### Data transformation with function composition

;; Clojure also has advanced built-in support for composing functions. This is especially useful for reduction transformations:

;; As an example, we'll compute total revenue in EUR:
(def revenue
  {"Ticket Sales" [{:amount 1200 :currency :EUR}
                   {:amount 800 :currency :CAD}
                   {:amount 1000 :currency :GBP}]
   "Merchandise" [{:amount 300 :currency :EUR}
                  {:amount 200 :currency :CAD}
                  {:amount 900 :currency :GBP}]
   "Concessions" [{:amount 500 :currency :EUR}
                  {:amount 750 :currency :GBP}]
   "Sponsors" [{:amount 1500 :currency :EUR}
               {:amount 1200 :currency :CAD}
               {:amount 600 :currency :GBP}]})

(def exchange-rates {:CAD 1.5 :GBP 0.85})

;; Can be done with threading (ok):

(->> revenue
     (mapcat second)
     (map #(* (:amount %) (exchange-rates (:currency %) 1)))
     (reduce + 0))

;; Or function composition (better):

(def transformer (comp (partial map #(* (:amount %) (exchange-rates (:currency %) 1)))
                       (partial mapcat second)))

(reduce + (transformer revenue))

;; Or as a single operation with a transducer (best):

(def transducer (comp (mapcat second)
                      (map #(* (:amount %) (exchange-rates (:currency %) 1)))))

(transduce transducer + 0 revenue)

;; Transducers are great because they eliminate intermediate collections and unnecessary processing. In the threading example, each step creates a new collection in memory, whereas the transducer version processes each element completely through all of the transformations before moving on to the next, reducing memory usage and compute time.

;; ### Higher-order functions

;; Clojure's standard library includes many useful higher-order functions, i.e. ones that take other functions as arguments.
;;
;; `juxt` creates a function that returns a vector of results from applying the functions it's given to the same argument(s)

(def describe (juxt count first last))
(describe [0 1 2 3 4 5 6 7])

;; `comp` creates a function that composes other functions:

(def neg-sum (comp - +))
(neg-sum 1 2 3 4)

;; `partial` creates a function with some arguments pre-applied
(def add-5 (partial + 5))
(add-5 10)

;; ### Common data transformations

;; Clojure comes with many useful built-in functions for operating on collections, most of which are based on either `map`, `filter`, or `reduce`. Combining these makes complex data transformations very succinct.

;; Count the total number of attendees per language:
(->> attendees
     (mapcat :languages)
     frequencies)

;; Group attendees by language:
(reduce (fn [acc {:keys [name languages]}]
          (merge-with into acc (zipmap languages (repeat #{name}))))
        {}
        attendees)

;; ### Destructuring

;; Clojure also supports destructuring and it is commonly used to when working with complex data structures:

;; Map destructuring
(let [{:keys [name city]} conference]
  (str name " in " city))

;; Nested destructuring
(let [{{:keys [city country]} :location} conference-data]
  (str city ", " country))

;; Vector destructuring
(let [[first-lang second-lang] languages]
  (str "Primary language: " first-lang ", Secondary: " second-lang))

;; Destructuring in function parameters
(defn format-conference [{:keys [name date]}]
  (str name " on " date))

(format-conference conference)

;; ## 3. Lazy evaluation

;; Most collection operators return lazy sequences, meaning the elements of the collection are not evaluated in advance or on the fly, they are only computed once they're needed. This has many benefits:
;; - Efficiency -- avoids unnecessary memory and compute usage
;; - Composability -- allows building complex data pipelines that process elements incrementally, i.e. passing one element at a time through the entire chain of operations without evaluating or reifying unnecessary intermediate collections
;; - Scalability -- allows for processing larger-than-memory datasets because elements are only processed as needed

;; We can write code as if we're working with infinite data, but Clojure will only compute what we actually need.

;; Create an infinite sequence of numbers:
(def infinite-numbers (iterate inc 0))

;; Take only what we need:
(->> infinite-numbers
     (drop 30)
     (take 10))

;; Fibonacci sequence (infinite)
(def fibonacci-seq
  ((fn fib [a b]
     (lazy-seq (cons a (fib b (+ a b)))))
   0 1))

;; Take first 10 Fibonacci numbers
(take 10 fibonacci-seq)

;; Find the first fibonacci number greater than 1000:
(first (drop-while #(<= % 1000) fibonacci-seq))

;; Print the 40th fibonacci number without computing the entire sequence:
(nth fibonacci-seq 40)

;; ### Processing large data efficiently

;; Lazy evaluation allows processing of large collections without loading everything into memory:
;;
;; Simulate a large collection
(def large-collection (range 1000000))

;; This doesn't evaluate the entire collection at once. Each element flows through the pipeline one at a time:
(->> large-collection
     (filter even?)
     (map #(* % %))
     (take 5))

;; ### Forcing evaluation

;; Sometimes you need to force evaluation of a lazy sequence. Clojure provides helpers for these scenarios:
;; - [`doall`](https://clojuredocs.org/clojure.core/doall) – Forces realization of the sequence.
;; - [`dorun`](https://clojuredocs.org/clojure.core/dorun) – Forces realization but discards the results.
;; - [`doseq`](https://clojuredocs.org/clojure.core/doseq) – Iterates over a sequence for side effects.

;; A common pitfall with lazy sequences is expecting things to be evaluated that aren't, leading you to not see expected output or side effects, and the solution is to use one of the above functions.

;; ### Memory considerations

;; Another common pitfall with lazy sequences is "holding onto the head", preventing garbage collection and leading to excessive memory consumption.

;; **Bad:** Holding onto the head of an infinite sequence keeps all realized elements in memory. This will accumulate indefinitely and eventually consume all memory:
(def bad-seq (map inc (range)))

;; Accessing part of `bad-seq` may seem fine:
(take 5 bad-seq)  ;; => (1 2 3 4 5)

;; But since `bad-seq` is still referenced, the reference to its head prevents garbage collection of the sequence as it gets consumed and the entire sequence persists in memory, eventually consuming all of it.

;; **Good:** Process elements incrementally without retaining the sequence head:
(doseq [n (take 5 (map inc (range)))]
  (println n))

;; This is useful when you need to execute a side effect on each item in a collection. This will print each number but doesn't retain the sequence in memory.

;; **Better:** Use `run!` for side-effectful operations without retaining elements:
(run! println (take 5 (map inc (range))))

;; This is another way to execute a side effectful function on each item in a collection without retaining a reference to the head of the sequence.

;; **Best:** Use chunked processing to control memory usage:
(doseq [batch (partition-all 5 (take 100 (range)))]
  (println "Processing batch..."))

;; Here `partition-all` breaks the sequence into smaller chunks that get processed independently. The head of the original sequence is not retained , allowing garbage collection to reclaim memory.

;; ## 4. Key takeaways

;; - Data is immutable by default, i.e. functions transform data, they do not modify it
;; - Everything is an expression, i.e. every operation returns a value
;; - Functions are composable, complex operations are composed of simple ones
;; - Functions are pure by default, making code easier to reason about
;; - Most sequences are lazy, take advantage of them to write efficient, composable data transformations
;; - Clojure has a rich standard library for data manipulation
;; - REPL-driven development enables rapid iteration
;; - Data oriented design leads to a focus on the shape of transformation of data

;; ## Next steps

;; Once you're comfortable with these basics, we'll look at Clojure's data science toolkit, [noj](https://scicloj.github.io/noj/), and learn how to use it to carry out a realistic data analysis using libraries like tablecloth, tech.ml.dataset, fastmath, metamorph.ml, tableplot, and more.
