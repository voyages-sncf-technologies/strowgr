# Docker

For building a docker image through maven, based on [Dockerfile template](src/docker/Dockerifile):

```shell
$  mvn  process-resources docker:build
```

An image `strowgr/admin-gui` is built in local.
