# Define paths to the executables you want to use on your system, or just
# leave them without a path name if the commands you want are already in
# your command path.

# Path to Clojure JAR file
CLOJURE_JAR_DIR=/Users/andy/.clojure
CLOJURE_CLASSPATH=$CLOJURE_JAR_DIR/clojure-1.1.0-alpha-SNAPSHOT.jar:$CLOJURE_JAR_DIR/clojure-contrib.jar

JAVAC=javac
JAVA=java

# Choose your style of Java profiling, if any.
#JAVA_PROFILING=-Xprof
#JAVA_PROFILING=-Xrunhprof
JAVA_PROFILING=

# Glasgow Haskell Compiler location
GHC=ghc

PERL=perl

# The mandelbrot benchmark requires SBCL built with threading enabled.
# On a Mac, one way to get this is to install MacPorts, and then this
# to get the threaded version of SBCL:
#
# sudo port install sbcl@+threads
SBCL=/opt/local/bin/sbcl
unset SBCL_HOME

CP="cp -p"
RM="/bin/rm"
DIFF="diff -c"
CMP="cmp"
