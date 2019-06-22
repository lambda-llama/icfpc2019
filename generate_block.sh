#!/bin/bash

./gradlew shadowJar

java -jar ./build/libs/icfpc2019.jar --generate "lambda-client/blocks/$1/puzzle.cond"
