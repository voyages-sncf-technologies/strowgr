#!/bin/bash
prefix=strowgrit
network=$prefix"_default"
cwd=$(cd $(dirname $0);pwd)
compose="docker-compose -p $prefix"

function cleanup(){
    $compose down
    docker rm -f $prefix"-tests"
}

cd $cwd
cleanup
$compose up -d
$compose logs --no-color > "$cwd/logs/mixed.log" &

IMAGE=$prefix"-tests"

docker build -t $IMAGE .

docker run --rm --net $network \
    -v $cwd:/scripts \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /usr/local/bin/docker:/usr/bin/docker \
    --name $prefix"-tests" \
    $IMAGE /bin/sh -c /scripts/suite.sh

$compose down