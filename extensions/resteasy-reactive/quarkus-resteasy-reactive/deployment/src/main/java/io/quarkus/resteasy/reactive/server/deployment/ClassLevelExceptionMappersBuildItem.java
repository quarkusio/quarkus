package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Map;
import java.util.Objects;

import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.builder.item.SimpleBuildItem;

final class ClassLevelExceptionMappersBuildItem extends SimpleBuildItem {

    /**
     * The key is the DotName of the class which contains methods annotated with {@link ServerExceptionMapper}
     * and the value is a map of from exception class name to generated exception mapper class name
     */
    private final Map<DotName, Map<String, String>> mappers;

    ClassLevelExceptionMappersBuildItem(Map<DotName, Map<String, String>> mappers) {
        this.mappers = Objects.requireNonNull(mappers);
    }

    public Map<DotName, Map<String, String>> getMappers() {
        return mappers;
    }
}
