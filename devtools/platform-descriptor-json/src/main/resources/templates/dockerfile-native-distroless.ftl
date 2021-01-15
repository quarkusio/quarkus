####
# This Dockerfile is used in order to build a distroless container that runs the Quarkus application in native (no JVM) mode
#
# Before building the container image run:
#
# mvn package -Pnative -Dquarkus.native.container-build=true
#
# Then, build the image with:
#
# docker build -f src/main/docker/Dockerfile.native-distroless -t quarkus/${project_artifactId} .
#
# Then run the container using:
#
# docker run -i --rm -p 8080:8080 quarkus/${project_artifactId}
#
###
FROM quay.io/quarkus/quarkus-distroless-image:20.3-java11
COPY ${build-dir}/*-runner /application

EXPOSE 8080
USER 1001

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]