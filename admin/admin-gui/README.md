# Docker

For building a docker image through maven, based on [Dockerfile template](src/docker/Dockerfile):

```shell
$  mvn  process-resources docker:build
```

An image `strowgr/admin-gui` is built locally.
