package io.quarkus.funqy.lambda.config;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Advanced event handling configuration
 */
@ConfigGroup
public interface AdvancedEventHandlingConfig {

    /**
     * Sqs related config.
     */
    Sqs sqs();

    /**
     * Sns related config.
     */
    Sns sns();

    /**
     * Kinesis related config.
     */
    Kinesis kinesis();

    /**
     * DynamoDb related config.
     */
    DynamoDb dynamoDb();
}
