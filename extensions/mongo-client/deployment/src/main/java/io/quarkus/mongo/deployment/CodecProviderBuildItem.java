package io.quarkus.mongo.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class CodecProviderBuildItem extends SimpleBuildItem {

    private List<String> codecProviderClassNames;

    public CodecProviderBuildItem(List<String> codecProviderClassNames) {
        this.codecProviderClassNames = codecProviderClassNames;
    }

    public List<String> getCodecProviderClassNames() {
        return codecProviderClassNames;
    }
}
