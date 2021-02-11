package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Test config root with "RuntimeConfig" suffix.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class FooRuntimeConfig {

    /**
     * Test property.
     */
    @ConfigItem(defaultValue = "baz")
    public String bar;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FooRuntimeConfig [bar=").append(bar).append("]");
        return builder.toString();
    }

}
