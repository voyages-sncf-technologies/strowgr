#!/usr/bin/env bash
CWD=$(cd $(dirname $0);pwd)
TS=$(date +%s)
base64 -i "$CWD/../data/HAsample.conf"
BASE64=$(base64 -i "$CWD/data/../HAsample.conf" )
DATA="{'conf' : '$BASE64','timestamp' : $TS, 'correlationid' : '$UUID', 'application' : 'OCE', 'platform' : 'REC1' }"
echo $D