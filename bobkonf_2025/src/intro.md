## Welcome to Clojure for data science

This book is a published, more curated version of the workshop first presented live at Bobkonf 2025 in Berlin on March 14, 2025.

## Abstract

Despite sophisticated tooling, data scientists still battle fundamental challenges day to day, like reproducibility, maintainability, and sharing their work. While traditional notebooks offer interactivity and quick feedback, they’re plagued with hidden state dependencies, version control complexity, and production deployment hurdles. Converting notebook-based analyses into production-ready code often requires extensive refactoring, untangling implicit dependencies, debugging hidden state issues, and deciphering sparse documentation. And that’s before tackling today’s reality of excessively large, unstructured data dumps typically lacking any metadata or explanation, making it difficult to find useful data in the first place.

Clojure’s data science ecosystem has been maturing rapidly in recent years. With it’s stable toolkit, immutable data structures, and functional paradigm, Clojure offers a compelling alternative to traditional data science workflows. Imagine knowing exactly which version of your code produced which dataset. Or seamlessly deploying the same code you used in an exploratory analysis to production. And imagine that code also ran in a state of the art literate programming environment, but also simultaneously in your own, familiar IDE.

This hands-on workshop will introduce a new way of thinking about working with data, demonstrating how Clojure’s libraries and tooling solve many pain points in current data science workflows.

## About the workshop

The world is full of useful information, but it is often only available in chaotic and unorganized forms. The central aim of this workshop is to learn how to transform wild, raw data into useful insights.

That process looks something like this:

- extract
- explore
- understand
- basic cleanup -- data formats, column names, drop empty columns, make tidy data
combine into a single, coherent dataset
- quality control -- check for accuracy, validity, consistency, remove duplicates, flag suspicious values
- analysis and visualization -- only makes sense to do once we have a clean, reliable dataset
- distribution

The problem with the way this process is typically executed is that the one-time, one-off steps are intermingled with the steps that need to be repeated, which makes these recurring steps difficult to automate and monitor. We'll explore what a robust data pipeline looks like and see how Clojure's powerful data processing toolkit can make all of this simpler.
Today we'll explore how bicycle usage in Berlin has evolved over time. By the end of the workshop we'll be able to answer questions like:

- What are the most travelled bicycle routes in the city?
- How did the pandemic affect bicycle usage in Berlin?
- How is bicycle traffic affected by time of day, day of the week, or time of year?

There is _way_ more than 90 minutes worth of material to cover, so in this workshop we'll move quickly through a few sections. In depth explorations of all of the skipped-over steps are available in this book for you to explore at your leisure. For now we'll focus our limited time on the following topics:

### Overview

1. **[Setup](/bobkonf-2025/notebooks.installation_guide.html) and [intro](/bobkonf-2025/notebooks.getting_started_with_clojure.html) (15 mins):**
  Hopefully you have already [installed Clojure](/bobkonf-2025/notebooks.installation_guide.html) and one way or another have a working Clojure environment. We'll make sure everyone is set up to follow along and take a very quick tour of Clojure's as a language in the [getting started section](/bobkonf-2025/notebooks.getting_started_with_clojure.html) before diving in to the specific libraries we'll use for a more realistic data analysis.

2. **[Data loading and exploration](/bobkonf-2025/notebooks.explore_and_understand.html) (20 mins):**
  We'll learn how to load data from the internet into a tablecloth dataset, how to parse excel files, and how to explore it so we can see what we're working with.

3. **Data cleaning and transformation (20 mins):**
  Once we have a sense of what our data looks like, we'll learn how to clean it up, transforming into an "analysis-ready" dataset.

4. **Analysis, visualization, and reporting (20 mins):**
  After we have a tidy dataset usable for analysis, we'll get to work answering some of our original questions.

5. **Integration and distribution (15 mins):**
  This is where the reproducibility parts comes in. As we go, we'll architect our data wrangling and analysis code in a way that makes it easy to re-run. We'll see how easy it is to deploy a dashboard based on our analysis.
