package io.quarkus.qute.runtime;

import io.quarkus.qute.RenderedResults;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface QuteTestModeConfig {

    /**
     * By default, the rendering results of injected and type-safe templates are recorded in the managed
     * {@link RenderedResults} which is registered as a CDI bean.
     */
    @WithDefault("true")
    boolean recordRenderedResults();

}