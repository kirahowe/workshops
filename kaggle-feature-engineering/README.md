# Feature Engineering

This folder is a translation into Clojure of Kaggle's feature enginering course.

## Recommended installation

Conda is recommended for managing python environments.

## Install Python dependencies

```
conda create --name feature-engineering --file requirements.txt
conda activate feature-engineering
```

## Configure Python environment

The [`python.edn`](./python.edn) file needs to be updated to use the paths in your conda environment. You can find these values by running the following commands:

- `:python-executable` is found by running `which python3`
- `:library-path` is found by running `python3 -c 'import sysconfig; print(sysconfig.get_paths()["stdlib"])'`

libpython-clj should work after these values are set.
