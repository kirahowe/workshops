# Reproducible data science with Clojure

This is the source code from the Reproducible Data Science with Clojure workshop, presented live in Berlin on March 14, 2025.

## Abstract

Despite sophisticated tooling, data scientists still battle fundamental challenges day to day, like reproducibility, maintainability, and sharing their work. While traditional notebooks offer interactivity and quick feedback, they’re plagued with hidden state dependencies, version control complexity, and production deployment hurdles. Converting notebook-based analyses into production-ready code often requires extensive refactoring, untangling implicit dependencies, debugging hidden state issues, and deciphering sparse documentation. And that’s before tackling today’s reality of excessively large, unstructured data dumps typically lacking any metadata or explanation, making it difficult to find useful data in the first place.

Clojure’s data science ecosystem has been maturing rapidly in recent years. With it’s stable toolkit, immutable data structures, and functional paradigm, Clojure offers a compelling alternative to traditional data science workflows. Imagine knowing exactly which version of your code produced which dataset. Or seamlessly deploying the same code you used in an exploratory analysis to production. And imagine that code also ran in a state of the art literate programming environment, but also simultaneously in your own, familiar IDE.

This hands-on workshop will introduce a new way of thinking about working with data, demonstrating how Clojure’s libraries and tooling solve many pain points in current data science workflows.

## Overview

This workshop is a high-level overview of Clojure's data processing capabilities, demonstrating an end-to-end process for analysing a public dataset. At the time of writing the tools and libraries demonstrated here represent the most stable and recommended setup for working with data in Clojure.

## Setup

If you're a workshop participant or want to follow along, there is an installation guide available in the [`1_installation_guide.clj`](./src/notebooks/1_installation_guide.clj) notebook, which is also [published online here](https://kira.quarto.pub/bobkonf-2025/notebooks.installation_guide.html).

## Developing the book

For developers of the workshop itself, the notebooks are namespaces in the [`src/notebooks`](/src/notebooks/) directory.

To build and deploy the book you will need the [quarto cli](https://quarto.org/docs/get-started/) and [babashka](https://github.com/babashka/babashka#installation) installed on your system.

There is a build script to convert the Clojure namespaces into a quarto book in `bin/build.clj`, which you can run via the configured babashka tasks like so:

```
bb build
```

There are also other bb tasks available, which you can see by running `bb tasks`:

```
The following tasks are available:

clean   Deletes the `book` directory
build   Builds the quarto publishable-book
deploy  Deploys the book to the quarto pub location specified in `bin/_publish.yml`
preview Preview the already built book, url will be printed once it's ready
release Clean, build, and deploy all at once
```

You can update the bb tasks to publish your own version of the book using `quarto publish`, to any of the many available free options it supports. If you want to use quarto-pub with your own account, update the details in `bin/_publish.yml`.
