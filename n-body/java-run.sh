#! /bin/bash

source ../env.sh

JVM_OPTS="-server"

$JAVA $JVM_OPTS -cp obj/java nbody "$@"
