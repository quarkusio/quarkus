package io.quarkus.redis.lettuce.runtime.internal;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.DockerImageNames;

final class RedisImage {

    static final String KEY = "redis.image";

    private RedisImage() {
    }

    static DockerImageName name() {
        return DockerImageName.parse(DockerImageNames.getImage(KEY));
    }

    static GenericContainer<?> plainServer() {
        return new GenericContainer<>(name()).withExposedPorts(6379);
    }

}
