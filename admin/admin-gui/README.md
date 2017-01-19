# Docker

For building a docker image through maven, based on [Dockerfile template](src/main/docker/Dockerfile):

```shell
$  mvn clean package -P build-docker
```

An image `strowgr/admin` is built locally.
