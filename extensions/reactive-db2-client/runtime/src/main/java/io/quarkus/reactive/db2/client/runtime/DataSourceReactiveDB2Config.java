package io.quarkus.reactive.db2.client.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceReactiveDB2Config {

    /**
     * Whether SSL/TLS is enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean ssl = false;

}
