# London Clojurians April 2024

This is the source code from the London Clojurians deep dive on Clojure's data ecosystem, presented live on April 30, 2024.

## JVM notes

The notebook for this workshop has some dependencies that do not work on Apple chips. If you are following along using an Apple system with an M1/M2 chip, you will need to install a x64 JVM and set `JAVA_HOME` to its home path. I recommend using `jabba` to manage multiple JVM installations.

## Recommended installation

Some of the code in [notebook.clj](./notebook.clj) uses `libpython-clj` to access python libraries. This requires having a functioning python environment, which is not as straightforward to set up as it sounds. I recommend using conda to manage python environments, following the steps below:

1. Install conda according to the recommended installation for your system

2. Install the python dependencies in a new conda environment:

```bash
conda env create -n libpython-clj -f london_clojurians_april_2024/python-env.yml
```

3. Activate that environment

```bash
conda activate libpython-clj
```

Now libpython-clj should work. You can add more or different python dependencies by editing the `env.yml` file.


conda create --name libpython-clj
conda activate libpython-clj
<!-- conda install matplotlib -->
conda install --file python-env.txt

<!-- conda install --yes --file python-env.txt -->
<!-- conda config --add channels conda-forge -->
