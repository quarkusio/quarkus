####
# Before building the docker image run:
#
# mvn package -Pnative -Dnative-image.docker-build=true
#
# Then, build the image with:
#
# docker build -f src/main/docker/Dockerfile -t shamrock/${mProjectArtifactId} .
#
# Then run the container using:
#
# docker run -i --rm -p 8080:8080 shamrock/${mProjectArtifactId}
#
###
FROM centos:7
WORKDIR /work/
COPY target/*-runner /work/application
RUN chmod 775 /work
EXPOSE 8080
CMD ["./application", "-Dshamrock.http.host=0.0.0.0"]