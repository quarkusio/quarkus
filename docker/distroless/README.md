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

## Build

The build requires [bazel](https://bazel.build/). 

```bash
bazel build
bazel run :latest
```

It builds the `cescoffier/native-base:latest` image.

## Updating the dependencies

* Update the hash and versions in the `dependencies.bzl` file.



