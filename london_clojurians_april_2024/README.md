# London Clojurians April 2024

This is the source code from the London Clojurians deep dive on Clojure's data ecosystem, presented live on April 30, 2024.

## Overview

This workshop is a whirlwind tour of some of Clojure's data processing and analysis capabilities. It represents the current best practices for accomplishing basic data science tasks in Clojure. Many of the tools demonstrated here at the time of this talk are works-in-progress. In the video I discuss the work that is ongoing in the community to polish these tools and improve their usability. Everything is possible, but we are working on hard making many of these tools more ergonomic for people who are more familiar with statistics and data science than they are with software engineering.

The talk covers
- data pre-processing using tablecloth
- comparing ML models
- basic time-series analysis
- python interop

Among other things.

## Dependencies

This notebook works on java version 21.0.2 2024-01-16 LTS and requires some python dependencies as well.

### Recommended installation

Some of the code presented in the workshop uses `libpython-clj` to access python libraries. This requires having a functioning python environment, which can at times be difficult to set up. I recommend using conda to manage python environments, following the steps below:

1. Install conda according to the recommended installation for your system

2. Install the python dependencies in a new conda environment:

```bash
conda env create -n libpython-clj -f london_clojurians_april_2024/python-env.yml
```

3. Activate that environment

```bash
conda activate libpython-clj
```

4. Update the [`python.edn`](./python.edn) file to use the paths to your conda environment. You can find these values from the command line:

- `:python-executable` is found by running `which python3`
- `:library-path` is found by running `python3 -c 'import sysconfig; print(sysconfig.get_paths())'`

Once these are set properly, libpython-clj should work.
