#!/bin/bash

set -xe

./gradlew shadowJar

OUTDIR=./solutions
SOLUTION=$OUTDIR/solution-$(date --iso-8601=seconds).zip

mkdir -p $OUTDIR
find . -regextype sed -regex "./part.*/.*sol" -exec zip -j $SOLUTION {} \;
