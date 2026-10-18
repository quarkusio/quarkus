package io.quarkus.reactive.db2.client.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DataSourceReactiveDB2Config {

    /**
     * Whether SSL/TLS is enabled.
     */
    @WithDefault("false")
    public boolean ssl();

}
