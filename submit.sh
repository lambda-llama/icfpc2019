#!/bin/bash

TEAM_ID=0ea1053c92f2121edaffc72f
SOLUTION=${1:?Usage: submit.sh SOLUTION_PATH}

curl -F "private_id=$TEAM_ID" -F "file=@$SOLUTION" https://monadic-lab.org/submit
