## Naming strategy

Two naming strategy are available
### Default naming strategy

Use docker engine ip address, service public port and service name

### Container name naming strategy

This strategy uses the container name suffixed by the service name.

eg for a container **CONT01** running the backend **WEB**, the id will be **CONT01_WEB** 

```
docker run --name CONT01 -l registrator.id_generator=container_name your/image
```

Remember the label for service name should be declared as an "image label", in the Dockerfile  
