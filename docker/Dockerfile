FROM java:8
LABEL SERVICE_8080_NAME haaas-admin
VOLUME /tmp
EXPOSE 8080
COPY app.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/urandom","-jar","/app.jar"]
