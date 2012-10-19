# Define paths to the executables you want to use on your system, or just
# leave them without a path name if the commands you want are already in
# your command path.

# Try to make the bash scripts for compiling and running work on all
# of these:

# * Linux
# * Mac OS X
# * Windows + Cygwin, started from within a bash session, not from a
#   Windows command prompt

# SEP=SEParator.  String to use in separating components of a file's
# path name.  '/' for Linux/Unix, '\' for Windows.

# PSEP=Path SEParator.  String to use in separating paths in a list of
# paths.  ':' for Linux/Unix, ';' for Windows.

# On Windows+Cygwin, it appears that these characters are only needed
# in classpath arguments to the Java run time.

OS=`uname -o 2>/dev/null`
if [ $? -ne 0 ]
then
    # Option -o does not exist in Mac OS X default version of uname
    # command.
    OS=`uname -s 2>/dev/null`
fi

# Functions to help in converting multiple allowed ways of specifying
# a Clojure version on the command line, into one standard version
# string CLJ_VERSION_STR for internal use in the rest of the scripts.

ALL_CLOJURE_VERSIONS="clj-1.2 clj-1.2.1 clj-1.3-alpha5 clj-1.3-alpha6 clj-1.3-alpha7 clj-1.3-alpha8 clj-1.3-beta1 clj-1.3-beta2 clj-1.3-beta3 clj-1.3 clj-1.4-alpha1 clj-1.4-alpha2 clj-1.4-alpha3 clj-1.4-alpha4 clj-1.4-alpha5 clj-1.4-beta1 clj-1.4-beta2 clj-1.4-beta3 clj-1.4-beta4 clj-1.4-beta5 clj-1.4-beta6 clj-1.4-beta7 clj-1.4 clj-1.5-alpha1 clj-1.5-alpha2 clj-1.5-alpha3 clj-1.5-alpha4 clj-1.5-alpha5 clj-1.5-alpha6 clj-1.5-alpha7"
ALL_MAJOR_CLOJURE_VERSIONS="clj-1.2.1 clj-1.3 clj-1.4 clj-1.5-alpha7"

show_known_clojure_versions()
{
    1>&2 echo -n "1.2 1.2.1 1.3-alpha[5-8] 1.3-beta[1-3] 1.3 1.4-alpha[1-5] 1.4-beta[1-7] 1.4.0 1.5-alpha[1-7]"
}

internal_check_clojure_version_spec()
{
    local spec="$1"
    local ret_val=0
    #echo "iccvs spec=:${spec}:"
    case "${spec}" in
    1.2 | 1.2.0)
	CLJ_VERSION_STR="1.2.0"
	;;
    1.2.1)
        CLJ_VERSION_STR="${spec}"
        ;;

    1.3-alpha[5-8])
        CLJ_VERSION_STR="1.3.0${spec/1.3/}"
        ;;
    1.3.0-alpha[5-8])
        CLJ_VERSION_STR="${spec}"
        ;;
    1.3-beta[1-3])
        CLJ_VERSION_STR="1.3.0${spec/1.3/}"
        ;;
    1.3.0-beta[1-3])
        CLJ_VERSION_STR="${spec}"
        ;;
    1.3 | 1.3.0)
        CLJ_VERSION_STR="1.3.0"
        ;;

    1.4-alpha[1-5])
        CLJ_VERSION_STR="1.4.0${spec/1.4/}"
        ;;
    1.4.0-alpha[1-5])
        CLJ_VERSION_STR="${spec}"
        ;;
    1.4-beta[1-7])
        CLJ_VERSION_STR="1.4.0${spec/1.4/}"
        ;;
    1.4.0-beta[1-7])
        CLJ_VERSION_STR="${spec}"
        ;;
    1.4 | 1.4.0)
        CLJ_VERSION_STR="1.4.0"
        ;;

    1.5-alpha[1-7])
        CLJ_VERSION_STR="1.5.0${spec/1.5/}"
        ;;
    1.5.0-alpha[1-7])
        CLJ_VERSION_STR="${spec}"
        ;;

    *)
        # Unknown Clojure version
	ret_val=1
	;;
    esac
    return ${ret_val}
}

check_clojure_version_spec()
{
    local spec="$1"
    internal_check_clojure_version_spec "${spec}"
    local exit_status=$?
    if [ ${exit_status} == 0 ]
    then
	return 0
    fi
    internal_check_clojure_version_spec "${spec/clj-/}"
    exit_status=$?
    if [ ${exit_status} == 0 ]
    then
	return 0
    fi
    internal_check_clojure_version_spec "${spec/clojure-/}"
    exit_status=$?
    if [ ${exit_status} == 0 ]
    then
	return 0
    fi
    return 1
}

#echo "Debug OS=:${OS}:"
if [ "$OS" == "Cygwin" ]
then
    SEP='\'
    PSEP=';'

    ######################################################################
    # Windows+Cygwin Java
    ######################################################################
    # Example paths I used successfully on Windows+Cygwin for some JVMs.

    # The JVM_TYPE is intended to be used as an argument to
    # measureproc if you want to get GC statistics, e.g.
    # measureproc --jvm-gc-stats "${JVM_TYPE}" ...
    JVM_TYPE="jrockit"
    JAVA_BIN="/cygdrive/c/Program Files/Java/jrmc-4.0.1-1.6.0/bin"
    #JVM_TYPE="hotspot"
    #JAVA_BIN="/cygdrive/c/Program Files/Java/jdk1.6.0_21/bin"
    JAVAC="${JAVA_BIN}/javac"
    JAVA="${JAVA_BIN}/java"

    ######################################################################
    # Windows+Cygwin Clojure
    ######################################################################

    HOME_DIR=`cygpath -w "$HOME"`
    check_clojure_version_spec "${CLJ_VERSION:=clojure-1.3.0}"
    if [ $? != 0 ]
    then
        1>&2 echo -n "$0: CLJ_VERSION='${CLJ_VERSION}' must be one of: "
	show_known_clojure_versions
	1>&2 echo ""
        exit 1
    fi
    CLOJURE_JAR_DIR=`cygpath -w "${HOME_DIR}/lein/clojure-${CLJ_VERSION_STR}/lib"`
    CLOJURE_CLASSPATH=`cygpath -w "${CLOJURE_JAR_DIR}/clojure-${CLJ_VERSION_STR}.jar"`
    # Platform-specific form of Clojure object file directory
    PS_CLJ_OBJ_DIR=".${SEP}obj${SEP}clojure-${CLJ_VERSION_STR}"
    PS_FULL_CLJ_CLASSPATH="${CLOJURE_CLASSPATH}${PSEP}${PS_CLJ_OBJ_DIR}"
    # Unix form, still useful in some places on Cygwin
    CLJ_OBJ_DIR="./obj/clojure-${CLJ_VERSION_STR}"

    ######################################################################
    # Windows+Cygwin SBCL
    ######################################################################
    # The mandelbrot benchmark requires SBCL built with threading enabled.
    # TBD: Is threading implemented in SBCL for Windows yet?  Test it.
    #
    # These lines worked for me on Windows+Cygwin with SBCL 1.0.37
    # installed from the binary available here:
    # http://www.sbcl.org/platform-table.html
    SBCL="/cygdrive/c/Program Files/Steel Bank Common Lisp/1.0.37/sbcl"
    export SBCL_HOME="C:\Program Files\Steel Bank Common Lisp\1.0.37"

    ######################################################################
    # Windows+Cygwin GHC
    ######################################################################
    # Glasgow Haskell Compiler location
    # There is no ghc package available in Cygwin as of Sep 2010.
    GHC="ghc"

    ######################################################################
    # Linux/MacOS Perl
    ######################################################################
    PERL="perl"

elif [ "$OS" == "GNU/Linux" -o "$OS" == "Darwin" ]
then
    SEP='/'
    PSEP=':'

    ######################################################################
    # Linux/MacOS Java
    ######################################################################
    # The JVM_TYPE is intended to be used as an argument to
    # measureproc if you want to get GC statistics, e.g.
    # measureproc --jvm-gc-stats "${JVM_TYPE}" ...
    JVM_TYPE="hotspot"
    JAVAC="javac"
    JAVA="java"

    ######################################################################
    # Linux/MacOS Clojure
    ######################################################################

    check_clojure_version_spec "${CLJ_VERSION:=clojure-1.3.0}"
    if [ $? != 0 ]
    then
        1>&2 echo -n "$0: CLJ_VERSION='${CLJ_VERSION}' must be one of: "
	show_known_clojure_versions
	1>&2 echo ""
        exit 1
    fi
    CLOJURE_JAR_DIR="${HOME}/lein/clojure-${CLJ_VERSION_STR}/lib"
    CLOJURE_CLASSPATH="${CLOJURE_JAR_DIR}/clojure-${CLJ_VERSION_STR}.jar"
    # Platform-specific form of Clojure object file directory
    PS_CLJ_OBJ_DIR=".${SEP}obj${SEP}clojure-${CLJ_VERSION_STR}"
    PS_FULL_CLJ_CLASSPATH="${CLOJURE_CLASSPATH}${PSEP}${PS_CLJ_OBJ_DIR}"
    CLJ_OBJ_DIR="./obj/clojure-${CLJ_VERSION_STR}"

    ######################################################################
    # Linux/MacOS SBCL
    ######################################################################
    # The mandelbrot benchmark requires SBCL built with threading enabled.
    # On a Mac, one way to get this is to install MacPorts, and then this
    # to get the threaded version of SBCL:
    #
    # sudo port install sbcl@+threads
    #
    # I've used these lines with success on my Mac with sbcl installed via
    # MacPorts.
    SBCL="sbcl"

    ######################################################################
    # Linux/MacOS GHC
    ######################################################################
    # Glasgow Haskell Compiler location
    GHC="ghc"

    ######################################################################
    # Linux/MacOS Perl
    ######################################################################
    PERL="perl"

    ######################################################################
    # Linux/MacOS Scala
    ######################################################################
    SCALAC="$HOME/sw/scala-2.8.1.final/bin/scalac"
    PS_SCALA_OBJ_DIR=".${SEP}obj${SEP}scala"
    SCALA_JAR_DIR="$HOME/sw/scala-2.8.1.final/lib"
    SCALA_CLASSPATH="${SCALA_JAR_DIR}/scala-library.jar"
    PS_FULL_SCALA_CLASSPATH="${SCALA_CLASSPATH}${PSEP}${PS_SCALA_OBJ_DIR}"

    ######################################################################
    # Linux/MacOS Perl
    ######################################################################
    JRUBY="$HOME/sw/jruby-1.5.6/bin/jruby"

else
    2>&1 echo "In script env.sh: Unknown output from 'uname -o' command:" $OS
    2>&1 echo "Aborting."
    exit 1
fi


######################################################################
# Common arguments for all measureproc runs:

# Using the arguments below for MP_COMMON_ARGS will cause all
# measurement results to be appended to an XML file $HOME/results.xml,
# creating it if it does not exist.  This file can be converted to a
# CSV file, easy to import into spreadsheet programs, by running this
# command:

# bin/xmltable2csv $HOME/results.xml > results.csv

#MP_COMMON_ARGS="--xml --log-file $HOME/results.xml"
MP_COMMON_ARGS=

######################################################################
# Common arguments for all measureproc runs that measure JVM runs:

# Using the longest list of arguments below for MP_ARGS_FOR_JVM_RUN
# will cause as many measurement details to be printed about the JVM,
# the OS, and garbage collection as measureproc is able to record.
# You might not want all of that information printed for interactive
# use.

#MP_ARGS_FOR_JVM_RUN="--jvm-info server --jvm-gc-stats ${JVM_TYPE}"
#MP_ARGS_FOR_JVM_RUN="--jvm-gc-stats ${JVM_TYPE}"
MP_ARGS_FOR_JVM_RUN="--jvm-info server"
#MP_ARGS_FOR_JVM_RUN=


JAVA_OBJ_DIR="./obj/java"
GHC_OBJ_DIR="./obj/ghc"
SBCL_OBJ_DIR="./obj/sbcl"
SCALA_OBJ_DIR="./obj/scala"
GCC_OBJ_DIR="./obj/gcc"

# Choose your style of Java profiling, if any.
#JAVA_PROFILING="-Xprof"
#JAVA_PROFILING="-Xrunhprof"
HPROF_OPTS="cpu=samples,depth=20,thread=y"
#JAVA_PROFILING="-agentlib:hprof=${HPROF_OPTS}"
JAVA_PROFILING=


######################################################################
# Miscellaneous scripting commands
######################################################################

CP="/bin/cp"
RM="/bin/rm"
DIFF="diff -c"

# On Windows, some programs produce \r\n for line endings, whereas
# others produces \n only.  Ignore trailing carriage returns on input.
CMP="diff --strip-trailing-cr --brief"
#CMP="cmp"

cmp_and_rm_2nd_if_correct()
{
	local F1="$1"
	local F2="$2"
	${CMP} "${F1}" "${F2}"
	local cmp_exit_status=$?
	if [ $cmp_exit_status == 0 ]
	then
		${RM} "${F2}"
	fi
}
