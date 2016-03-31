#!/usr/bin/env bash
#set -x
CWD=$(cd $(dirname $0);pwd)
TS=$(date +%s)
FILENAME="$CWD/../data/HAsample.conf"
BASE64=$(base64 -w 0 -i $FILENAME )
SYSLOG_BASE64=$(base64 -w 0 -i "$CWD/../data/syslog_fragment.conf" )
DOCKER_HOST=$(docker-machine ip default)
UUID=$(uuidgen)
DATA=$(
cat <<-EOF
{
    "conf":"$BASE64",
    "syslogFragment":"$SYSLOG_BASE64"
    "timestamp":$TS,
    "correlationid":"$UUID",
    "application":"OCE",
    "platform":"REC1",
    "hapversion": "1.4.22"
}
EOF
)

curl -d "$DATA" -H "Content-Type: application/json" -X POST http://$DOCKER_HOST:4151/pub?topic=commit_requested_default-name
