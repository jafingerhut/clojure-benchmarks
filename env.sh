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

# Note: There is a clj-1.4-alpha5 in the Maven repo, but according
# to the Clojure git log, it is identical to clj-1.4-beta1.  I kept
# only clj-1.4-beta1.

# See also below the definition of ALL_BENCHMARK_CLOJURE_VERSIONS,
# which is a subset of ALL_CLOJURE_VERSIONS

ALL_CLOJURE_VERSIONS="clj-1.2 clj-1.2.1 clj-1.3-alpha5 clj-1.3-alpha6 clj-1.3-alpha7 clj-1.3-alpha8 clj-1.3-beta1 clj-1.3-beta2 clj-1.3-beta3 clj-1.3 clj-1.4-alpha1 clj-1.4-alpha2 clj-1.4-alpha3 clj-1.4-alpha4 clj-1.4-beta1 clj-1.4-beta2 clj-1.4-beta3 clj-1.4-beta4 clj-1.4-beta5 clj-1.4-beta6 clj-1.4-beta7 clj-1.4 clj-1.5-alpha1 clj-1.5-alpha2 clj-1.5-alpha3 clj-1.5-alpha4 clj-1.5-alpha5 clj-1.5-alpha6 clj-1.5-alpha7 clj-1.5-beta1 clj-1.5-beta2 clj-1.5-RC1 clj-1.5-RC2 clj-1.5-RC3"

ALL_MAJOR_CLOJURE_VERSIONS="clj-1.2.1 clj-1.3 clj-1.4 clj-1.5-RC3"

show_known_clojure_versions()
{
    1>&2 echo -n "1.2 1.2.1 1.3-alpha[5-8] 1.3-beta[1-3] 1.3 1.4-alpha[1-5] 1.4-beta[1-7] 1.4.0 1.5-alpha[1-7] clj-1.5-beta[1-2] clj-1.5-RC[1-3]"
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

    1.5-beta[1-2])
        CLJ_VERSION_STR="1.5.0${spec/1.5/}"
        ;;
    1.5.0-beta[1-2])
        CLJ_VERSION_STR="${spec}"
        ;;

    1.5-RC[1-3])
        CLJ_VERSION_STR="1.5.0${spec/1.5/}"
        ;;
    1.5.0-RC[1-3])
        CLJ_VERSION_STR="${spec}"
        ;;

    *)
        # Unknown Clojure version
	ret_val=1
	;;
    esac
    SHORT_CLJ_VERSION_STR="${CLJ_VERSION_STR/.0/}"
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

make_clojure_version_set()
{
    # str_list is a single string containing 'words' separated by
    # white space.  This function and
    # check_exact_string_in_string_set() only work for lists/sets of
    # strings where each string contains no white space, and no colon
    # characters.
    local version_set=":"
    for v in $*
    do
        #1>&2 echo "andy-debug $v"
        check_clojure_version_spec "$v"
        if [ $? != 0 ]
        then
            1>&2 echo "Bad Clojure version string found in version_list=${version_list}"
            1>&2 echo -n "$0: v='${v}' must be one of: "
	    show_known_clojure_versions
	    1>&2 echo ""
            exit 1
        fi
        version_set="${version_set}${CLJ_VERSION_STR}:"
    done
    echo "${version_set}"
}

check_exact_string_in_string_set()
{
    local str="$1"
    # Note: The 'string set' is assumed to be a sequence of strings
    # that do not contain colon (:) characters, separated by colon
    # characters, and with a colon at the beginning and end.  For
    # example, the set containing strings "a", "foo", and "gah" would
    # be represented as ":a:foo:gah:"
    local str_set="$2"
    local t="${str_set/:${str}:/}"
    if [ "${str_set}" == "${t}" ]
    then
	# str not in str_set, so return "bad" non-0 exit status
	return 1
    fi
    # str in str_set, so return "good" 0 exit status
    return 0
}

all_clojure_versions_except()
{
    local clojure_versions_to_exclude_set=`make_clojure_version_set $*`
    local tmp
    local ret_val=""
    for v in ${ALL_CLOJURE_VERSIONS}
    do
        check_clojure_version_spec "${v}"
        tmp="${CLJ_VERSION_STR}"
        check_exact_string_in_string_set "${tmp}" "${clojure_versions_to_exclude_set}"
        if [ $? != 0 ]
        then
            # The version is not in the list of versions to exclude,
            # so add it to return value.
	    ret_val="${ret_val} ${v}"
        fi
    done
    echo "${ret_val}"
}

# ALL_BENCHMARK_CLOJURE_VERSIONS is same as ALL_CLOJURE_VERSIONS,
# except the following versions have been removed so that benchmarking
# runs go faster:

# 1.5-RC2 is identical to 1.5-RC1, except for the changes.md file.

# 1.5-RC1 is only 1 commit older than 1.5-RC3.  Keep 1.5-RC3 with its
# 1 extra comit in preference to 1.5-RC1.

# 1.5-alpha7 is only 1 commit newer than 1.5-alpha6, and it is the
# addition of 5 new macros that won't be used in any benchmarks
# spanning many versions.

# 1.5-alpha6 is only 1 commit newer than 1.5-alpha5, and it is a minor
# bug fix in column metadata.

# 1.4-beta7 is only 2 commits older than 1.4.0, and they are edits to
# doc strings.

# 1.4-beta5 is only 2 commits newer than 1.4-beta4, and they are a
# change to the data readers implementation and addition of the
# *compiler-options* Var.

# 1.4-beta3 is only 1 commit newer than 1.4-beta2, and it is the
# option to remove metadata during compilation.

ALL_BENCHMARK_CLOJURE_VERSIONS="`all_clojure_versions_except 1.4-beta3 1.4-beta5 1.4-beta7 1.5-alpha6 1.5-alpha7 1.5-RC1 1.5-RC2`"

all_benchmark_clojure_versions_except()
{
    local clojure_versions_to_exclude_set=`make_clojure_version_set $*`
    local tmp
    local ret_val=""
    for v in ${ALL_BENCHMARK_CLOJURE_VERSIONS}
    do
        check_clojure_version_spec "${v}"
        tmp="${CLJ_VERSION_STR}"
        check_exact_string_in_string_set "${tmp}" "${clojure_versions_to_exclude_set}"
        if [ $? != 0 ]
        then
            # The version is not in the list of versions to exclude,
            # so add it to return value.
	    ret_val="${ret_val} ${v}"
        fi
    done
    echo "${ret_val}"
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


# Options to give on java command line for all Clojure and Java programs
JAVA_OPTS="-server -Xmixed"
#JAVA_OPTS="-server -Xint"
#JAVA_OPTS="-client -Xmixed"
#JAVA_OPTS="-client -Xint"


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
