^:kindly/hide-code
(ns notebooks.installation-guide
  (:require [scicloj.clay.v2.api :as clay]))

;; # Welcome to Reproducible Data Science with Clojure!

;; This guide will walk you through setting up your environment for the BobKonf 2025 workshop on reproducible data science with Clojure. By the end of this setup process, you'll have a working environment where you can run all the examples and start exploring data science with Clojure's powerful toolkit.

;; ## What We're Setting Up

;; Before we dive into installation steps, here's an overview of the tools we'll be using:

;; - **Clojure**: A functional programming language that prioritizes immutability and simplicity
;; - **Java Development Kit (JDK)**: Since Clojure runs on the Java Virtual Machine (JVM)
;; - **Clojure CLI**: For dependency management and running our code
;; - **IDE with Clojure support**: For interactive development (VS Code + Calva recommended for beginners)
;; - **Clay**: A notebook-like environment for creating interactive, reproducible data science workflows
;; - **Tablecloth**: Data manipulation library similar to pandas in Python
;; - **Noj**: Opinionated collection of libraries for working with data in Clojure, like the tidyverse

;; ## Installation Steps

;; There is an excellent and more detailed [installation guide available here](https://clojure.org/guides/install_clojure) if you would like more details. A short/quick version is summarized below.

;; ### 1. Java Development Kit (JDK)

;; Clojure runs on the JVM, so we need Java installed first:

;; **macOS:**
;; ```bash
;; brew install --cask temurin@21
;; ```

;; **Linux:**
;; ```bash
;; # Ubuntu/Debian
;; sudo apt update
;; sudo apt install temurin-21-jdk
;;
;; # Fedora
;; sudo dnf install temurin-21-jdk
;; ```

;; **Windows:**
;; - Download and install Temurin JDK 21 from https://adoptium.net/
;; - Add Java to your PATH environment variable

;; **Verify your Java installation by opening a terminal and running:**
;; ```bash
;; java -version
;; ```

;; ### 2. Clojure CLI Tools

;; The Clojure CLI tools will help us manage dependencies and run Clojure code:

;; **macOS:**
;; ```bash
;; brew install clojure/tools/clojure
;; ```

;; **Linux:**
;; ```bash
;; curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
;; chmod +x linux-install.sh
;; sudo ./linux-install.sh
;; ```

;; **Windows:**

;; Either install [WSL](https://learn.microsoft.com/en-us/windows/wsl/install) and proceed with Linux installation instructions, or use [clj-msi](https://github.com/casselc/clj-msi). More details are available in the [Clojure installation guide for Windows](https://clojure.org/guides/install_clojure#_windows)

;; **Verify installation:**
;; ```bash
;; clj --version
;; ```
;; ### 3. Editor Setup

;; While Clojure works with many editors (Emacs, IntelliJ+Cursive, Vim), we recommend VS Code with Calva for beginners:

;; #### Option A: Visual Studio Code with Calva (Recommended for beginners)

;; 1. Install Visual Studio Code from https://code.visualstudio.com/
;; 2. Install the Calva extension:
;;    - Open VS Code
;;    - Press Ctrl+P (Cmd+P on macOS)
;;    - Type: `ext install betterthantomorrow.calva`
;;    - Press Enter

;; There are excellent instructions outlining many popular and free alternatives on the [Clojure editors guide](https://clojure.org/guides/editors)

;; ## Getting the Workshop Code

;; Clone the workshop repository:

;; ```bash
;; git clone https://github.com/kirahowe/workshops
;; cd bobkonf_2025
;; ```

;; ## Starting a REPL and Connecting Your Editor

;; ### VS Code + Calva:
;; 1. Open the `workshops/bobkonf_2025` project folder in VS Code
;; 2. Open the command palette (Ctrl+Shift+P / Cmd+Shift+P)
;; 3. Type "Calva: Start a Project REPL and Connect"
;; 4. Select "deps.edn" when prompted for project type
;; 5. Select the default alias when prompted

;; ### Testing Your Setup

;; Let's test whether everything is working:

(defn hello-workshop []
  (println "Welcome to Reproducible Data Science with Clojure at BobKonf 2025!"))

;; Load the current namespace into the repl (open the command palette and select "Calva: Load/Evaluate Current File and its Requires/Dependencies")

;; Try evaluating the function by placing your cursor after the closing parenthesis
;; and pressing Alt+Enter (Option+Enter on macOS)

(hello-workshop)

;; ## Workshop Structure

;; This workshop consists of several notebooks that build on each other:

;; 1. **getting_started_with_clojure.clj** - Introduction to Clojure's syntax and concepts
;; 2. **intro.clj** - Overview of the data science problem we'll solve
;; 3. **prepared.clj** - Data loading, cleaning, and preparation
;; 4. **analysis_and_visualisation.clj** - Analyzing and visualizing our data

;; Each notebook contains explanatory comments and code that you can evaluate
;; interactively as you work through the material.

;; ## Using Clay for Notebook-Style Development

;; This workshop uses Clay, which provides a notebook-like experience while keeping all the benefits of working with plain Clojure files. With Clay in your classpath, Calva will discover Custom REPL Commands. To invoke a custom REPL command, press **Ctrl+Alt+Space** followed by **n** to invoke Clay make Namespace as HTML. Pressing **Ctrl+Alt+Space** followed by **Space** opens a quick-pick list of custom REPL commands to invoke.

;; ## Key Libraries We'll Use

;; During the workshop, we'll work with these libraries:

;; - **tablecloth**: Data manipulation library (like pandas for Python)
;; - **scicloj/clay**: For notebook rendering
;; - **scicloj/noj**: For numerical computing and visualization
;; - **fastexcel-reader**: For reading Excel files
;; - **clojure.java-time**: For working with dates and times

;; ## Workshop Data

;; The workshop includes sample datasets in the `data/prepared/` directory.
;; We'll use these to demonstrate data loading, cleaning, and analysis techniques.

;; ## Getting Help

;; If you encounter any issues during setup:
;; - Reach out to the workshop facilitators
;; - Join the Clojurians Slack: https://clojurians.slack.com/ (get an invite at http://clojurians.net/)
;; - Check Clojure documentation: https://clojure.org/guides/getting_started

;; Ready to start? Let's move on to `notebooks.getting-started-with-clojure` to begin our journey!

;; Happy coding!
