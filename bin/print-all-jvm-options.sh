#! /bin/bash

B=`dirname $0`
source "${B}/../env.sh"
"${JAVA}" -XX:+UnlockDiagnosticVMOptions -XX:+PrintFlagsFinal -version
