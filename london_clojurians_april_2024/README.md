# London Clojurians April 2024

This is the source code from the London Clojurians deep dive on Clojure's data ecosystem, presented live on April 30, 2024.

## Overview



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



<!-- The notebook for this workshop has some dependencies that do not work on Apple chips. If you are following along using an Apple system with an M1/M2 chip, you will need to install a x64 JVM and set `JAVA_HOME` to its home path. I recommend using `jabba` to manage multiple JVM installations. -->
