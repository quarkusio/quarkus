package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Stork config for new Vert.x gRPC
 */
@ConfigGroup
public class StorkConfig {
    /**
     * Number of threads on a delayed gRPC ClientCall
     */
    @ConfigItem(defaultValue = "10")
    public int threads;

    /**
     * Deadline in milliseconds of delayed gRPC call
     */
    @ConfigItem(defaultValue = "5000")
    public long deadline;

    /**
     * Number of retries on a gRPC ClientCall
     */
    @ConfigItem(defaultValue = "3")
    public int retries;

    /**
     * Initial delay in seconds on refresh check
     */
    @ConfigItem(defaultValue = "60")
    public long delay;

    /**
     * Refresh period in seconds
     */
    @ConfigItem(defaultValue = "120")
    public long period;
}
