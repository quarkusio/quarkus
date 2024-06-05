package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import java.util.Map;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.SimpleBuildItem;

public final class GeneratedJsonSerializationBuildItem extends SimpleBuildItem {

    private final Map<String, ClassInfo> jsonClasses;

    public GeneratedJsonSerializationBuildItem(Map<String, ClassInfo> jsonClasses) {
        this.jsonClasses = jsonClasses;
    }

    public Map<String, ClassInfo> getJsonClasses() {
        return jsonClasses;
    }
}
