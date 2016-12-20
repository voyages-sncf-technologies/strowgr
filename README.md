[![Build Status](https://travis-ci.org/voyages-sncf-technologies/strowgr.svg?branch=develop)](https://travis-ci.org/voyages-sncf-technologies/strowgr) [![codecov](https://codecov.io/gh/voyages-sncf-technologies/strowgr/branch/develop/graph/badge.svg)](https://codecov.io/gh/voyages-sncf-technologies/strowgr) ![guillaume](https://img.shields.io/badge/works%20on%20guillaume's%20computer-ok-green.svg) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/b5eb23250055421abbe5bf62eab8a5fd)](https://www.codacy.com/app/garnaud25/strowgr?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=voyages-sncf-technologies/strowgr&amp;utm_campaign=Badge_Grade)


# strowgr

A service discovery around Haproxy


## Build

Build the whole project:

```shell
$ mvn package
```

Build additionally docker images of `admin` and `sidekick`:
                  
```shell
$ mvn package -Pbuild-docker -Ptarget-linux
```


## Release

For instance, the release of 0.2.5:

```shell
$ ./release.sh 0.2.5 0.2.6-SNAPSHOT
```

All these steps could be done by _mvn release:prepare_ and _mvn release:perform_ but some issues must be fixed for not publishing in a classical maven repo (perform failed because dependencies on submodules has not been found).


