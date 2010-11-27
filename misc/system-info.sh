#! /bin/bash

OS=`uname -o 2>/dev/null`
if [ $? -ne 0 ]
then
    # Option -o does not exist in Mac OS X default version of uname
    # command.
    OS=`uname -s 2>/dev/null`
fi

set -x

uname -a

# TBD: What kind of commands are there for Windows like the ones below?

if [ "$OS" == "GNU/Linux" ]
then
    cat /proc/cpuinfo
    cat /proc/meminfo
elif [ "$OS" == "Darwin" ]
then
    /usr/sbin/system_profiler -detailLevel full SPHardwareDataType
    sw_vers
elif [ "$OS" == "Cygwin" ]
then
    # This tool is part of Mark Russinovich's Sysinternals Suite,
    # available here:
    # http://technet.microsoft.com/en-us/sysinternals/bb842062.aspx
    coreinfo
    procfeatures
    psinfo
fi
