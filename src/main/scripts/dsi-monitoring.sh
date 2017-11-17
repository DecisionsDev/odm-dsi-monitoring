#!/bin/bash

set -e

RP=`realpath $0`
SDIR=`dirname $RP`

echo "Installation directory: $SDIR"

java -classpath $SDIR/../lib/restConnector.jar:$SDIR/classes:$SDIR/jcommander-1.48.jar com.ibm.ia.monitoring.DSIMonitoring $*
