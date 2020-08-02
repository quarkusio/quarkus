package io.quarkus.quartz.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class QuartzAdditionalPropsConfig {
    /**
     * Class name for the configuration.
     */
    @ConfigItem(name = "class")
    public String clazz;

    /**
     * The props name and the values for the props.
     */
    @ConfigDocSection
    @ConfigDocMapKey("propsAndValue")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, String> propsValue;
}
