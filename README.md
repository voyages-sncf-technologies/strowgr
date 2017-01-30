[![Build Status](https://travis-ci.org/voyages-sncf-technologies/strowgr.svg?branch=develop)](https://travis-ci.org/voyages-sncf-technologies/strowgr) [![codecov](https://codecov.io/gh/voyages-sncf-technologies/strowgr/branch/develop/graph/badge.svg)](https://codecov.io/gh/voyages-sncf-technologies/strowgr) ![guillaume](https://img.shields.io/badge/works%20on%20guillaume's%20computer-ok-green.svg) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/b5eb23250055421abbe5bf62eab8a5fd)](https://www.codacy.com/app/garnaud25/strowgr?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=voyages-sncf-technologies/strowgr&amp;utm_campaign=Badge_Grade)


# strowgr

A service discovery around Haproxy


## Build

Build the whole project:

```shell
$ mvn package
```

Build additionally docker images of `admin`:
                  
```shell
$ docker build -t strowgr/admin:latest admin
```


## Release

For instance, the release of 0.2.5:

```shell
$ ./release.sh 0.2.5 0.2.6-SNAPSHOT
```

All these steps could be done by _mvn release:prepare_ and _mvn release:perform_ but some issues must be fixed for not publishing in a classical maven repo (perform failed because dependencies on submodules has not been found).


## Start Admin locally

```bash
# start side services
docker-compose up

# start admin app
mvn clean package -f admin
java -jar admin/admin-gui/target/admin-gui-*.jar server admin/admin-gui/src/main/resources/configuration.yaml
```

## Commands

Strowgr uses _dropwizard_ with some additional commands.

    $ java -jar target/admin-gui-0.2.5-SNAPSHOT.jar
    usage: java -jar admin-gui-0.2.5-SNAPSHOT.jar
           [-h] [-v] {server,check,init,config} ...
    
    positional arguments:
      {server,check,init,config}
                             available commands
    
    optional arguments:
      -h, --help             show this help message and exit
      -v, --version          show the application version and exit
      
      
### server

Dropwizard start basic command.

    $ java -jar target/admin-gui-0.2.5-SNAPSHOT.jar server -h
    usage: java -jar admin-gui-0.2.5-SNAPSHOT.jar
           server [-h] [file]
    
    Runs the Dropwizard application as an HTTP server
    
    positional arguments:
      file                   application configuration file
    
    optional arguments:
      -h, --help             show this help message and exit
      
   
### check

Dopwizard config check command.

    $ java -jar target/admin-gui-0.2.5-SNAPSHOT.jar check -h
    usage: java -jar admin-gui-0.2.5-SNAPSHOT.jar
           check [-h] [file]
    
    Parses and validates the configuration file
    
    positional arguments:
      file                   application configuration file
    
    optional arguments:
      -h, --help             show this help message and exit
      
     
### config

Strowgr command for generating default config yaml:


    $ java -jar target/admin-gui-0.2.5-SNAPSHOT.jar config
    argument -o/--output-file is required
    usage: java -jar admin-gui-0.2.5-SNAPSHOT.jar
           config -o OUTPUT-FILE [-h]
    
    generate configuration file
    
    optional arguments:
      -o OUTPUT-FILE, --output-file OUTPUT-FILE
                             output file of generated configuration
      -h, --help             show this help message and exit
      
      
