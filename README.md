# Workshops

This is a collection of workshops I've put on in recent years. Each folder is a self-contained project containing instructions, data, and notebooks for following along.

## Contents

### [Clojure for Data Deep Dive](./london_clojurians_april_2024/README.md)

This was first presented at the London Clojurians online meetup in April, 2024. It's a whirlwind tour of Clojure's data science ecosystem, with examples of how to accomplish some non-trivial tasks using Clojure's data science toolkit.

At the time of the recording the libraries shown were in various states of maturity. The underlying data type and dataset libraries ([dtype.next](https://github.com/cnuernber/dtype-next/tree/master) and [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset)) are very mature and stable. The ML and statistics libraries are functional and stable, but not necessarily in their final forms. The visualization libraries are still under very active development and will certainly evolve more in the coming months. This presentation represents the best of Clojure's current set of building blocks for working with data.

### [Reproducible Data Science with Clojure](./bobkonf_2025/README.md)

This was first presented live at the Bobkonf conference in Berlin on March 14, 2025. It's a comprehensive walk-through of how to collect, clean up, prepare, analyze, and distribute data, using the example of a public dataset about bicycle traffic from Berlin.

The workshop features [noj](https://github.com/scicloj/noj) at a mature beta release stage, Clojure's emerging all-in-one data science toolkit, similar to the [tidyverse](https://www.tidyverse.org). The data processing and wrangling libraries (mostly [tablecloth](https://github.com/scicloj/tablecloth)) are very mature and stable and consequently the workshop is heavy on these initial steps of data analysis. The visualization libraries remain under active development. [Tableplot](https://github.com/scicloj/tableplot) is featured here, bundled with noj. At the time of this presentation, this represents the recommended and most mature, well-supported combination of libraries for carrying out an end-to-end data project in Clojure.
