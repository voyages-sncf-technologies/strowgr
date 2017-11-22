[![Build Status](https://travis-ci.org/voyages-sncf-technologies/strowgr.svg?branch=develop)](https://travis-ci.org/voyages-sncf-technologies/strowgr) [![codecov](https://codecov.io/gh/voyages-sncf-technologies/strowgr/branch/develop/graph/badge.svg)](https://codecov.io/gh/voyages-sncf-technologies/strowgr) ![guillaume](https://img.shields.io/badge/works%20on%20guillaume's%20computer-ok-green.svg) 


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


### start env with Docker
CAUTION : build strowgr stack with Docker actually fails 

```bash

# init a swarm
docker swarm init

# start side services
docker stack deploy -c docker-compose.yml strowgr

# check replicas with
docker service ls

```

### start env process by process
```bash
# start nsqlookup
curl -d 'hello world 1' 'http://127.0.0.1:4151/pub?topic=test'

# start nsq
nsqd --lookupd-tcp-address=127.0.0.1:4160

# start nsq admin
nsqadmin --lookupd-http-address=127.0.0.1:4161

# start consul
consul agent -dev
```

### start Strowgr admin
```bash
# build
mvn clean package -f admin
# run
java -jar admin/admin-gui/target/admin-gui-*.jar server admin/admin-gui/src/main/resources/configuration.yaml
```

when topic are not yet created, you add to create it (see <http://nsq.io/overview/quick_start.html>)

```bash
curl -d 'hello world 1' 'http://127.0.0.1:4151/pub?topic=test'
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
      
    SRE-81: ajouter des ACLs à Strowgr: filtrer les entrypoints/haproxy de prod si le profil de consultation n'appartient au groupe prod.
    Nouveaux éléments de configuration:
      platformValue: par défaut production, mais possibilité de changer pour tester dans un environnemment non production.
      authenticatorType: ldap cible de déploiement.
                         none: pas d'authentification: dev en poste local.
                         prod_mock: authentification systématique bouchonnée permettant de créer un profil appartenant au groupe production: pour test en local.
                         noprod_mock: authentification systématique bouchonnée permettant de créer un profil n' appartenant au groupe production: pour test en local.
      
### Additional Informations
  strowgr/admin/admin-consul/insertDatas.bat is a windows command to insert datas for testing in local: it is actually a subset of production datas: enrich it with new use cases if useful  
