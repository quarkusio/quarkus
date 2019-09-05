package io.quarkus.dynamodb.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AwsCredentialsProviderConfig {

    /**
     * Credentials provider
     */
    @ConfigItem(defaultValue = "DEFAULT")
    public AwsCredentialsProviderType type;

    /**
     * Default credentials provider configuration
     */
    @ConfigItem
    public DefaultCredentialsProviderConfig defaultProvider;

    /**
     * Static credentials provider configuration
     */
    @ConfigItem
    public StaticCredentialsProviderConfig staticProvider;

    /**
     * AWS Profile credentials provider configuration
     */
    @ConfigItem
    public ProfileCredentialsProviderConfig profileProvider;

    /**
     * Process credentials provider configuration
     */
    @ConfigItem
    public ProcessCredentialsProviderConfig processProvider;

    @ConfigGroup
    public static class DefaultCredentialsProviderConfig {

        /**
         * Fetch credentials asynchronously in the background.
         * 
         * <p>
         * By default, this is disabled.
         */
        @ConfigItem(defaultValue = "false")
        public Optional<Boolean> asyncCredentialUpdateEnabled;

        /**
         * Reuse the last successful credentials provider in the chain. It will typically return credentials faster than
         * searching through the chain.
         *
         * <p>
         * By default, this is enabled.
         */
        @ConfigItem(defaultValue = "true")
        public Optional<Boolean> reuseLastProviderEnabled;
    }

    @ConfigGroup
    public static class StaticCredentialsProviderConfig {
        /**
         * Access key id
         */
        @ConfigItem
        public String accessKeyId;

        /**
         * Secret access key
         */
        @ConfigItem
        public String secretAccessKey;
    }

    @ConfigGroup
    public static class ProfileCredentialsProviderConfig {
        /**
         * Profile name
         *
         * <p>
         * By default, the profile name is 'default'.
         */
        @ConfigItem
        public Optional<String> profileName;
    }

    @ConfigGroup
    public static class ProcessCredentialsProviderConfig {
        /**
         * Configure whether the provider should fetch credentials asynchronously in the background.
         * If this is true, threads are less likely to block when credentials are loaded,
         * but additional resources are used to maintain the provider.
         *
         * <p>
         * By default, this is disabled.
         */
        @ConfigItem
        public Optional<Boolean> asyncCredentialUpdateEnabled;

        /**
         * Configure the amount of time between when the credentials expire and when the credentials should start to be
         * refreshed. This allows the credentials to be refreshed *before* they are reported to expire.
         *
         * <p>
         * Default: 15 seconds.
         */
        @ConfigItem
        public Optional<Duration> credentialRefreshThreshold;

        /**
         * Configure the maximum amount of data that can be returned by the external process before an exception is
         * raised.
         *
         * <p>
         * Default: 1024 bytes.
         */
        @ConfigItem
        public Optional<Integer> processOutputLimit;

        /**
         * Command that should be executed to retrieve credentials.
         */
        @ConfigItem
        public String command;
    }
}
