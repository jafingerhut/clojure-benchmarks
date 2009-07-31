#! /bin/bash

source ../env.sh

JVM_OPTS="-client -Xmx544m"

$JAVA $JVM_OPTS revcomp "$@"
