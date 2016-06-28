#!/bin/sh

cwd=$(cd $(dirname $0);pwd)

cd $cwd

echo
while true;do
    echo `date` "### Wait for admin to start"
    curl -s http://admin:8080/api/entrypoints -H "Content-type: application/json" && break
    sleep 1
done

echo
while true;do
    echo `date` "### Wait for channels to be created"
    ( curl -s http://nsqlookupd:4161/channels?topic=commit_requested_default-name | grep "slave" ) && break
    sleep 1
done

echo
echo `date` "### Create entrypoint"
curl -s -X PUT \
    -H "Content-Type: application/json" -d @firstTest.json  \
    http://admin:8080/api/entrypoints/DEM/REL2
echo

#for i in $(seq 1 10);do
echo
while true;do
    echo `date` "### Check entrypoint creation"
    curl -s -f -X GET -H "Content-Type: application/json" http://admin:8080/api/entrypoints/DEM/REL2/current && break
    sleep 4
done
