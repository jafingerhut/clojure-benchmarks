#! /bin/bash

source ../env.sh

$JAVA -server fannkuch "$@"
