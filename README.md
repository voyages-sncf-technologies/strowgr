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

Some components need to bind to an explicit interface in your machine. So docker compose defined here uses environment variable `local_ip`:

```bash

$ export local_ip=1.2.3.4
$ docker swarm init # init a swarm
$ docker stack deploy -c docker-compose.yml strowgr # start side services
$ docker service ls # check replicas with
ID                  NAME                      MODE                REPLICAS            IMAGE                     PORTS
weouwdzcpenf        strowgr_consul            replicated          1/1                 consul:v0.6.4             *:8500->8500/tcp
j5grtmvaz3xm        strowgr_nsqadmin          replicated          1/1                 nsqio/nsq:v0.3.7          *:4171->4171/tcp
59rrur8slmwr        strowgr_nsqd              replicated          1/1                 nsqio/nsq:v0.3.7          *:4150->4150/tcp,*:4151->4151/tcp
0a3ktssdtigr        strowgr_nsqlookupd        replicated          1/1                 nsqio/nsq:v0.3.7          *:4160->4160/tcp,*:4161->4161/tcp
w2t9quryyrcf        strowgr_sidekick-master   replicated          1/1                 strowgr/sidekick:v0.3.4   *:53000->53000/tcp,*:53001->53001/tcp,*:53002->53002/tcp,*:53003->53003/tcp
pfuyrm0nyjqy        strowgr_webpage           replicated          1/1                 nginx:1.11.8-alpine       *:80->80/tcp
```

If all your services are up (replicas 1/1), you can access to the [setup page](http://localhost:80). All information are here for a full setup on local.


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
      
    SRE-81: ajouter des ACLs à Strowgr: filtering entrypoints/haproxy whose platform value is production if user doesn't belong to specified confgi element 'prodGroupName'
    New configuration elements:
      platformValue: default production, but it is possible de change the value in order to test in non prod environement.
      authenticatorType: ldap target deployment value.
                         for dev in local desk: 
                         none: no authentication
                         prod_mock: authentication in success, creating a user belonging to production group.
                         noprod_mock: authentication in success, creating a user not belonging to production group.
      
### Additional Informations
  strowgr/admin/admin-consul/insertDatas.bat is a windows command to insert datas for testing in local: it is actually a subset of production datas: enrich it with new use cases if useful
  You can export keys from an environment with the syntax:
    consul.exe kv export -http-addr=url_of_consul > json_file   
    Exemple for parisiancocktail:
      consul.exe kv export -http-addr=http://parisiancocktail:52000 > ass_keys.json
  You can import then in local environment with same "philosophy"
    consul.exe kv import -http-addr=http://localhost:8500  @ass_keys.json
  if not https....
  
  consul.exe is here the windows download in local.
