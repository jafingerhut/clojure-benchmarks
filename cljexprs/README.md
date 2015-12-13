# cljexprs

Use criterium to benchmark the run time of some Clojure expressions,
and present the results as graphs.


## Usage

First, update the following bash scripts in the directory above this
one.

File `env.sh`:

* Add all desired Clojure versions to be tested, and perhaps some that
  you do not want to test, if you want the list of released versions
  to be complete, to the variable `ALL_CLOJURE_VERSIONS`.

* Add any versions that are in `ALL_CLOJURE_VERSIONS`, but that you do
  not want to benchmark, to the list of versions to leave out in the
  expression that is assigned to `ALL_BENCHMARK_CLOJURE_VERSIONS`.

* Add new major versions to `ALL_MAJOR_CLOJURE_VERSIONS`.

* Update the cases in function
  `internal_check_clojure_version_spec()`.

File `lein-init.sh`:

* Copy and edit new `for` loops to retrieve the desired versions of
  Clojure.

Install Leiningen (any 2.x version should do).  Run `./lein-init.sh`
from the directory containing that script to get all of the JAR files
for every desired version of Clojure.

Look at `cljexprs/scripts/runseveral.sh` to see if it has the OS/JDK
combinations that you want to run on a particular machine, and the
right `source` commands to set up for a particular JDK version you
want to test.  Edit as desired.

When ready, make sure nothing unnecessary is running on the test
machine, and that will be true for as long as the benchmarks will run.
Then do:

```bash
% cd cljexprs
% ./scripts/runseveral.sh
```

When the benchmark results are available in files in a directory tree
structure within the `results` directory, run these commands to create
graphs of them:

```base
% cd cljexprs/results
% ../scripts/makegraphs.sh
```

How long does it take to run a full set of Clojure expression
benchmarks?

As a rough estimate, running the current 49 expression benchmarks for
a single OS/JDK/Clojure version combination takes about 42 minutes.

If you have V versions of Clojure, each running 3 trials, on a single
OS/JDK, that will take about 126*V minutes.

Multiply that by the number of OS/JDK combinations you want to
benchmark, if they will be run sequentially on the same machine.

Plan for next update to the benchmarks is V=54 (from 1.3.0 betas up
through 1.8.0-RC2).  OS/JDK combos are planned to be:

* Mac OS X 10.6.8 + JDK 1.6.0_65
* Ubuntu 14.04.3 LTS plus:
 * 32-bit and 64-bit versions of:
  * Oracle JDK 1.6.0_<something>
  * Oracle JDK 1.7.0_<something>
  * Oracle JDK 1.8.0_<something>
  * Oracle JDK 1.9.0_<something>

That is 9 versions.

(V Clojure versions) * (3 trials) * (42 mins) = 6,804 mins = 4.7 days

For 9 versions, that is 42.5 days.


## License

Copyright © 2012-2015 Andy Fingerhut

Distributed under the Eclipse Public License, the same as Clojure.
