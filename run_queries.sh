#!/usr/bin/env bash

QUERIES=$1
OUTPUT_FILE=$2
ENDPOINT=$3
EPSILON=$4

for qfile in $QUERIES/*.rq; do
  echo $qfile
  echo gtimeout 10m mvn exec:java -Dexec.mainClass=cl.utfsm.di.RDFDifferentialPrivacy.Run.RunSymbolic -Dexec.args="-f $qfile -d $ENDPOINT -o $OUTPUT_FILE -v true -e $ENDPOINT -eps $EPSILON"
  gtimeout 10m mvn exec:java -Dexec.mainClass=cl.utfsm.di.RDFDifferentialPrivacy.Run.RunSymbolic -Dexec.args="-f $qfile -d $ENDPOINT -o $OUTPUT_FILE -v true -e $ENDPOINT -eps $EPSILON"
done
