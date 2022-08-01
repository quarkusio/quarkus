package io.quarkus.kafka.client.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class KafkaBuildTimeUiConfig {

    /**
     * The path where Kafka UI is available.
     * The value `/` is not allowed as it blocks the application from serving anything else.
     * By default, this URL will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.
     */
    @ConfigItem(defaultValue = "kafka-ui")
    public String rootPath;
    /**
     * Whether or not to enable Kafka Dev UI in non-development native mode.
     */
    @ConfigItem(name = "handlerpath", defaultValue = "kafka-admin")
    public String handlerRootPath;
    /**
     * Always include the UI. By default, this will only be included in dev and test.
     * Setting this to true will also include the UI in Prod
     */
    @ConfigItem(defaultValue = "false")
    public boolean alwaysInclude;

}
