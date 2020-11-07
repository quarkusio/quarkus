package io.quarkus.minio;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class MinioConfiguration {

    /**
     * The minio server URL
     */
    @ConfigItem
    String url;

    /**
     * The minio server access key
     */
    @ConfigItem
    String accessKey;

    /**
     * The minio server secret key
     */
    @ConfigItem
    String secretKey;
}
