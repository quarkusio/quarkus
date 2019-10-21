package io.quarkus.amazon.s3.runtime;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class S3Config {

    /**
     * Enable using the accelerate endpoint when accessing S3.
     * 
     * <p>
     * Accelerate endpoints allow faster transfer of objects by using Amazon CloudFront's globally distributed edge locations.
     */
    @ConfigItem(defaultValue = "false")
    public boolean accelerateMode;

    /**
     * Enable doing a validation of the checksum of an object stored in S3.
     */
    @ConfigItem(defaultValue = "true")
    public boolean checksumValidation;

    /**
     * Enable using chunked encoding when signing the request payload for
     * {@link software.amazon.awssdk.services.s3.model.PutObjectRequest}
     * and {@link software.amazon.awssdk.services.s3.model.UploadPartRequest}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean chunkedEncoding;

    /**
     * Enable dualstack mode for accessing S3. If you want to use IPv6 when accessing S3, dualstack
     * must be enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean dualstack;

    /**
     * Enable using path style access for accessing S3 objects instead of DNS style access.
     * DNS style access is preferred as it will result in better load balancing when accessing S3.
     */
    @ConfigItem(defaultValue = "false")
    public boolean pathStyleAccess;

    /**
     * AWS SDK client configurations
     */
    @ConfigItem(name = ConfigItem.PARENT)
    @ConfigDocSection
    public SdkConfig sdk;

    /**
     * AWS services configurations
     */
    @ConfigItem
    @ConfigDocSection
    public AwsConfig aws;

    /**
     * Sync HTTP transport configurations
     */
    @ConfigItem
    @ConfigDocSection
    public SyncHttpClientConfig syncClient;

    /**
     * Netty HTTP transport configurations
     */
    @ConfigItem
    @ConfigDocSection
    public NettyHttpClientConfig asyncClient;
}
