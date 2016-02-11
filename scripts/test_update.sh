#!/usr/bin/env bash
set -x
CWD=$(cd $(dirname $0);pwd)
TS=$(date +%s)
BASE64=$(base64 -i "$CWD/../data/HAsample.conf" )
DOCKER_HOST=$(docker-machine ip default)
UUID=$(uuidgen)
DATA=$(cat <<- OEF
{
    "conf" : "$BASE64",
    "timestamp" : $TS,
    "correlationid" : "$UUID",
    "application" : "OCE",
    "platform" : "REC1"
}
EOF)

curl -d "$DATA" -H "Content-Type: application/json" -X POST http://$DOCKER_HOST:4151/pub?topic=try_update_default-name
