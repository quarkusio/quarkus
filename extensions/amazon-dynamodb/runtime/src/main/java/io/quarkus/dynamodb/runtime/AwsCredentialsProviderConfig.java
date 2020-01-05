package io.quarkus.dynamodb.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.configuration.MemorySize;

@ConfigGroup
public class AwsCredentialsProviderConfig {

    // @formatter:off
    /**
     * Configure the credentials provider that should be used to authenticate with AWS.
     *
     * Available values:
     *
     * * `default` - the provider will attempt to identify the credentials automatically using the following checks:
     * ** Java System Properties - `aws.accessKeyId` and `aws.secretKey`
     * ** Environment Variables - `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
     * ** Credential profiles file at the default location (`~/.aws/credentials`) shared by all AWS SDKs and the AWS CLI
     * ** Credentials delivered through the Amazon EC2 container service if `AWS_CONTAINER_CREDENTIALS_RELATIVE_URI` environment variable is set and security manager has permission to access the variable.
     * ** Instance profile credentials delivered through the Amazon EC2 metadata service
     * * `static` - the provider that uses the access key and secret access key specified in the `static-provider` section of the config.
     * * `system-property` - it loads credentials from the `aws.accessKeyId`, `aws.secretAccessKey` and `aws.sessionToken` system properties.
     * * `env-variable` - it loads credentials from the `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` and `AWS_SESSION_TOKEN` environment variables.
     * * `profile` - credentials are based on AWS configuration profiles. This loads credentials from
     *               a http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html[profile file],
     *               allowing you to share multiple sets of AWS security credentials between different tools like the AWS SDK for Java and the AWS CLI.
     * * `container` - It loads credentials from a local metadata service. Containers currently supported by the AWS SDK are
     *                 **Amazon Elastic Container Service (ECS)** and **AWS Greengrass**
     * * `instance-profile` - It loads credentials from the Amazon EC2 Instance Metadata Service.
     * * `process` - Credentials are loaded from an external process. This is used to support the credential_process setting in the profile
     *               credentials file. See https://docs.aws.amazon.com/cli/latest/topic/config-vars.html#sourcing-credentials-from-external-processes[Sourcing Credentials From External Processes]
     *               for more information.
     * * `anonymous` - It always returns anonymous AWS credentials. Anonymous AWS credentials result in un-authenticated requests and will
     *                 fail unless the resource or API's policy has been configured to specifically allow anonymous access.
     *
     * @asciidoclet
     */
    // @formatter:on
    @ConfigItem(defaultValue = "default")
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
         * Whether this provider should fetch credentials asynchronously in the background.
         * <p>
         * If this is `true`, threads are less likely to block, but additional resources are used to maintain the provider.
         */
        @ConfigItem
        public boolean asyncCredentialUpdateEnabled;

        /**
         * Whether the provider should reuse the last successful credentials provider in the chain.
         * <p>
         * Reusing the last successful credentials provider will typically return credentials faster than searching through the
         * chain.
         */
        @ConfigItem(defaultValue = "true")
        public boolean reuseLastProviderEnabled;
    }

    @ConfigGroup
    public static class StaticCredentialsProviderConfig {
        /**
         * AWS Access key id
         */
        @ConfigItem
        public Optional<String> accessKeyId;

        /**
         * AWS Secret access key
         */
        @ConfigItem
        public Optional<String> secretAccessKey;
    }

    @ConfigGroup
    public static class ProfileCredentialsProviderConfig {
        /**
         * The name of the profile that should be used by this credentials provider.
         * <p>
         * If not specified, the value in `AWS_PROFILE` environment variable or `aws.profile` system property is used and
         * defaults to `default` name.
         */
        @ConfigItem
        public Optional<String> profileName;
    }

    @ConfigGroup
    public static class ProcessCredentialsProviderConfig {
        /**
         * Whether the provider should fetch credentials asynchronously in the background.
         * <p>
         * If this is true, threads are less likely to block when credentials are loaded, but additional resources are used to
         * maintain the provider.
         */
        @ConfigItem
        public boolean asyncCredentialUpdateEnabled;

        /**
         * The amount of time between when the credentials expire and when the credentials should start to be
         * refreshed.
         * <p>
         * This allows the credentials to be refreshed *before* they are reported to expire.
         */
        @ConfigItem(defaultValue = "15S")
        public Duration credentialRefreshThreshold;

        /**
         * The maximum size of the output that can be returned by the external process before an exception is raised.
         */
        @ConfigItem(defaultValue = "1024")
        public MemorySize processOutputLimit;

        /**
         * The command that should be executed to retrieve credentials.
         */
        @ConfigItem
        public Optional<String> command;
    }
}
