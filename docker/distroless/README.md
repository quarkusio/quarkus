# Distroless base image

This project creates a [distroless image](https://github.com/GoogleContainerTools/distroless) suitable to run Shamrock applications.

This image contains a minimal Linux, glibc-based system. 
It contains:

* ca-certificates
* A /etc/passwd entry for a root user
* A /tmp directory
* tzdata
* glibc
* libssl
* openssl
* zlib

The final image is about 17Mb.

## Using the image

Build the native executable using:

```bash
mvn package -Pnative -Dnative-image.docker-build=true
```

Then, create the following `Dockerfile`:

```dockerfile
FROM cescoffier/native-base:latest
COPY target/*-runner /application
EXPOSE 8080
CMD ["./application", "-Dshamrock.http.host=0.0.0.0"]
```

Build the docker image using (change the namespace/name):

```bash
docker build -t protean-demo/demo . 
```

You can then run your application using:

```bash
docker run -i --rm -p 8080:8080 protean-demo/demo
```

## Build

The build requires [bazel](https://bazel.build/). 

```bash
bazel build
bazel run :latest
```

It builds the `cescoffier/native-base:latest` image.

## Updating the dependencies

* Update the hash and versions in the `dependencies.bzl` file.



