package io.quarkus.funqy.lambda.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Kinesis event config
 */
@ConfigGroup
public interface DynamoDb {

    /**
     * Allows functions to return partially successful responses for a batch of event records.
     */
    @WithDefault("true")
    boolean reportBatchItemFailures();
}
