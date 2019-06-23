#!/bin/bash

./gradlew shadowJar

for f in $(find part-* -name '*.desc' | sort -n ); do
    java -jar ./build/libs/icfpc2019.jar --non-interactive --info_only --show_bonus_count $f
done
