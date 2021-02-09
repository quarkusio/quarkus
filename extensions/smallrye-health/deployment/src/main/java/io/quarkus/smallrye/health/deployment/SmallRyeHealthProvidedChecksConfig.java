package io.quarkus.smallrye.health.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SmallRyeHealthProvidedChecksConfig {

    /**
     * Configuration for the HeapMemoryHealthCheck (io.smallrye.health.checks.HeapMemoryHealthCheck).
     */
    @ConfigItem
    @ConfigDocSection
    HeapMemoryHealthCheckConfig heapMemory;

    @ConfigGroup
    public static final class HeapMemoryHealthCheckConfig {

        /**
         * Whether the HeapMemoryHealthCheck is enabled.
         */
        @ConfigItem(defaultValue = "false")
        boolean enabled;

        /**
         * Maximum permitted percentage of used heap memory. Must be a number between 0 and 1.
         */
        @ConfigItem(defaultValue = "0.9")
        double maxPercentage;

    }

    /**
     * Configuration for the NonHeapMemoryHealthCheck (io.smallrye.health.checks.NonHeapMemoryHealthCheck).
     */
    @ConfigItem
    @ConfigDocSection
    NonHeapMemoryHealthCheckConfig nonHeapMemory;

    @ConfigGroup
    public static final class NonHeapMemoryHealthCheckConfig {

        /**
         * Whether the NonHeapMemoryHealthCheck is enabled.
         */
        @ConfigItem(defaultValue = "false")
        boolean enabled;

        /**
         * Maximum permitted percentage of used non-heap memory. Must be a number between 0 and 1.
         */
        @ConfigItem(defaultValue = "0.9")
        double maxPercentage;

    }

    /**
     * Configuration for the InetAddressHealthCheck (io.smallrye.health.checks.InetAddressHealthCheck).
     */
    @ConfigItem
    @ConfigDocSection
    InetAddressHealthCheckConfig inetAddress;

    @ConfigGroup
    public static final class InetAddressHealthCheckConfig {

        /**
         * Whether the InetAddressHealthCheck is enabled.
         */
        @ConfigItem(defaultValue = "false")
        boolean enabled;

        /**
         * The host name to check connection to.
         */
        @ConfigItem
        Optional<String> host;

    }

    /**
     * Configuration for the SocketHealthCheck (io.smallrye.health.checks.SocketHealthCheck).
     */
    @ConfigItem
    @ConfigDocSection
    SocketHealthCheckConfig socket;

    @ConfigGroup
    public static final class SocketHealthCheckConfig {

        /**
         * Whether the SocketHealthCheck is enabled.
         */
        @ConfigItem(defaultValue = "false")
        boolean enabled;

        /**
         * The host name to check connection to.
         */
        @ConfigItem
        Optional<String> host;

        /**
         * The port to check connection to.
         */
        @ConfigItem
        Optional<Integer> port;

    }

    /**
     * Configuration for the SystemLoadHealthCheck (io.smallrye.health.checks.SystemLoadHealthCheck).
     */
    @ConfigItem
    @ConfigDocSection
    SystemLoadHealthCheckConfig systemLoad;

    @ConfigGroup
    public static final class SystemLoadHealthCheckConfig {

        /**
         * Whether the SystemLoadHealthCheck is enabled.
         */
        @ConfigItem(defaultValue = "false")
        boolean enabled;

        /**
         * Maximum allowed system load. Must be a number between 0 and 1.
         */
        @ConfigItem(defaultValue = "0.7")
        double max;

    }

    /**
     * Configuration for the ThreadHealthCheck (io.smallrye.health.checks.ThreadHealthCheck).
     */
    @ConfigItem
    @ConfigDocSection
    ThreadHealthCheckConfig thread;

    @ConfigGroup
    public static final class ThreadHealthCheckConfig {

        /**
         * Whether the ThreadHealthCheck is enabled.
         */
        @ConfigItem(defaultValue = "false")
        boolean enabled;

        /**
         * Maximum allowed number of threads.
         */
        @ConfigItem
        Optional<Integer> max;

    }

    /**
     * Configuration for the UrlHealthCheck (io.smallrye.health.checks.UrlHealthCheck).
     */
    @ConfigItem
    @ConfigDocSection
    UrlHealthCheckConfig url;

    @ConfigGroup
    public static final class UrlHealthCheckConfig {

        /**
         * Whether the UrlHealthCheck is enabled.
         */
        @ConfigItem(defaultValue = "false")
        boolean enabled;

        /**
         * The URL to check.
         */
        @ConfigItem
        Optional<String> address;

    }

}
