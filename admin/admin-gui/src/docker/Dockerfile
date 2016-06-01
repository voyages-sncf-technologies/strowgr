FROM java:8

LABEL application.name STROWGRUI
LABEL application.desc "Strowgr admin ui"

LABEL service.8080_tcp.name "ui"
LABEL service.8080_tcp.desc "Strowgr web ui"
EXPOSE 8080

COPY admin-gui-${version}.jar /app.jar

CMD ["java", "-jar", "/app.jar"]