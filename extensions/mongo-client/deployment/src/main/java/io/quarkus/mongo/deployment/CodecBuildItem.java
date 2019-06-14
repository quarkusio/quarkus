package io.quarkus.mongo.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class CodecBuildItem extends SimpleBuildItem {

    private List<String> codecsClassNames;
    private List<String> codecProviderClassNames;

    public CodecBuildItem(List<String> codecsClassNames, List<String> codecProviderClassNames) {
        this.codecsClassNames = codecsClassNames;
        this.codecProviderClassNames = codecProviderClassNames;
    }

    public List<String> getCodecsClassNames() {
        return codecsClassNames;
    }

    public List<String> getCodecProviderClassNames() {
        return codecProviderClassNames;
    }
}
