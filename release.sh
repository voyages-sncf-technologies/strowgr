#!/bin/bash

# WARNING: naive version, workspace should be clean

if [ "$#" -ne 2 ]; then
	    echo "Illegal number of parameters. Usage: ./release.sh <release version> <new version>"
fi

RELEASE_VERSION=$1
NEW_VERSION=$2

mvn versions:set -DnewVersion=$RELEASE_VERSION
mvn versions:commit
git add .
git commit -m "[release] v$RELEASE_VERSION"
git push
git tag "v$RELEASE_VERSION"
git push --tags origin master
mvn versions:set -DnewVersion=$NEW_VERSION
mvn versions:commit
git add .
git commit -m "[build] $NEW_VERSION"
git push
