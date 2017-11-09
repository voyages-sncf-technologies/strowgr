FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/admin-gui-0.3.2-SNAPSHOT.jar admin-gui.jar
ADD src/main/resources/* /tmp/
ENTRYPOINT exec java -jar /admin-gui.jar server /tmp/configuration.yaml