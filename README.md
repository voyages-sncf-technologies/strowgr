[![Build Status](https://travis-ci.org/voyages-sncf-technologies/strowgr.svg?branch=develop)](https://travis-ci.org/voyages-sncf-technologies/strowgr) [![codecov](https://codecov.io/gh/voyages-sncf-technologies/strowgr/branch/develop/graph/badge.svg)](https://codecov.io/gh/voyages-sncf-technologies/strowgr) ![guillaume](https://img.shields.io/badge/works%20on%20guillaume's%20computer-ok-green.svg)


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

