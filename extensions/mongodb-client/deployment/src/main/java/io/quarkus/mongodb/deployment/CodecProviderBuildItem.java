package io.quarkus.mongodb.deployment;

import java.util.List;

import org.bson.codecs.configuration.CodecProvider;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Register additional {@link CodecProvider}s for the MongoDB clients.
 */
public final class CodecProviderBuildItem extends SimpleBuildItem {

    private final List<String> codecProviderClassNames;

    public CodecProviderBuildItem(List<String> codecProviderClassNames) {
        this.codecProviderClassNames = codecProviderClassNames;
    }

    public List<String> getCodecProviderClassNames() {
        return codecProviderClassNames;
    }
}
