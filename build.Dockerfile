FROM java

RUN mkdir apache-maven && curl http://mirrors.ircam.fr/pub/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz | tar xz -C apache-maven --strip-components 1
COPY . ./
VOLUME /dist
VOLUME /root/.m2

CMD /apache-maven/bin/mvn package -DskipTests && cp admin-gui/target/admin-gui-*.jar /dist
