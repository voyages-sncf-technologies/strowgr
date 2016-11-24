## Prepare your image
Use labels to provide metadata for your image

*Dockerfile*
```
LABEL application.name="myapp" \
      application.desc="my app" \
      service.8080_tcp.name="myapp-api" \
      service.8080_tcp.desc="my app rest API"
```

Then you only need to provide the platform to register the container:

```
docker run --name CONT01 -l platform.name REC1 example/myapp 
```

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
