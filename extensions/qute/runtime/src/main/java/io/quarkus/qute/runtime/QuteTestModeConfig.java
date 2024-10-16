package io.quarkus.qute.runtime;

import io.quarkus.qute.RenderedResults;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class QuteTestModeConfig {

    /**
     * By default, the rendering results of injected and type-safe templates are recorded in the managed
     * {@link RenderedResults} which is registered as a CDI bean.
     */
    @ConfigItem(defaultValue = "true")
    public boolean recordRenderedResults;

}