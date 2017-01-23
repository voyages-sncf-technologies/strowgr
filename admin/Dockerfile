FROM java:8

LABEL application.name StrowgrAdminUI
LABEL application.desc "Strowgr Admin ui"

EXPOSE 8080

COPY admin-gui/target/admin-gui-*.jar /app.jar
COPY admin-gui/src/main/resources/local-configuration.yaml /server.yaml

ENTRYPOINT ["java", "-jar", "/app.jar", "server", "/server.yaml"]