####
# This Dockerfile is used in order to build a container that runs the Quarkus application in JVM mode
#
# Before building the docker image run:
#
# mvn package
#
# Then, build the image with:
#
# docker build -f src/main/docker/Dockerfile.jvm -t quarkus/${project_artifactId}-jvm .
#
# Then run the container using:
#
# docker run -i --rm -p 8080:8080 quarkus/${project_artifactId}-jvm
#
###
FROM fabric8/java-jboss-openjdk8-jdk
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Dquarkus.log.file.enable=false"
COPY target/lib/* /deployments/lib/
COPY target/*-runner.jar /deployments/app.jar
ENTRYPOINT [ "/deployments/run-java.sh" ]
