####
# This Dockerfile is used in order to build a container that runs the Quarkus application in native (no JVM) mode
#
# Before building the container image run:
#
# mvn package -Pnative -Dquarkus.native.container-build=true
#
# Then, build the image with:
#
# docker build -f src/main/docker/Dockerfile.native -t quarkus/${project_artifactId} .
#
# Then run the container using:
#
# docker run -i --rm -p 8080:8080 quarkus/${project_artifactId}
#
###
FROM registry.access.redhat.com/ubi8/ubi-minimal:8.1
WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work
COPY --chown=1001:root ${build_dir}/*-runner /work/application

EXPOSE 8080
USER 1001

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
