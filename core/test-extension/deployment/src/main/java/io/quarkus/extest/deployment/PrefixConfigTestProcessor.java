package io.quarkus.extest.deployment;

import java.util.List;

import io.quarkus.arc.deployment.ConfigPropertyBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.extest.runtime.config.PrefixBuildTimeConfig;
import io.quarkus.extest.runtime.config.PrefixConfig;

public class PrefixConfigTestProcessor {
    @BuildStep
    void validateBuildTime(BuildProducer<ConfigPropertyBuildItem> configProperties,
            PrefixBuildTimeConfig prefixBuildTimeConfig) {
        assert prefixBuildTimeConfig.prop.equals("1234");
        assert prefixBuildTimeConfig.map.get("prop").equals("1234");
    }

    @BuildStep
    void validateRuntime(BuildProducer<ConfigPropertyBuildItem> configProperties,
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            PrefixConfig prefixConfig) {
        assert prefixConfig.prop.equals("1234");
        assert prefixConfig.map.get("prop").equals("1234");
        assert prefixConfig.nested.nestedValue.equals("nested-1234");
        assert prefixConfig.nested.oov.getPart1().equals("nested-1234");
        assert prefixConfig.nested.oov.getPart2().equals("nested-5678");
        assert configDescriptionBuildItems.stream().map(ConfigDescriptionBuildItem::getPropertyName)
                .anyMatch("my.prefix.prop"::equals);
    }
}
