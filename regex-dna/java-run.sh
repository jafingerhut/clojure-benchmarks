#! /bin/bash

source ../env.sh

JVM_OPTS="-server -Xmx1024m"

$JAVA $JVM_OPTS regexdna "$@"
