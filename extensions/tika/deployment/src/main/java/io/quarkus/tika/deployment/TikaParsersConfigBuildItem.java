package io.quarkus.tika.deployment;

import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.tika.runtime.TikaParserParameter;

public final class TikaParsersConfigBuildItem extends SimpleBuildItem {

    private final Map<String, List<TikaParserParameter>> parsersConfig;

    public TikaParsersConfigBuildItem(Map<String, List<TikaParserParameter>> parsersConfig) {
        this.parsersConfig = parsersConfig;
    }

    public Map<String, List<TikaParserParameter>> getConfiguration() {
        return parsersConfig;
    }

}
