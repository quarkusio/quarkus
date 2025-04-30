package io.quarkus.cli.image;

public enum Builder {
    docker,
    podman,
    jib,
    buildpack,
    openshift
}
