package io.quarkus.minio;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class MinioConfiguration {

    /**
     * The minio server URL. 
     * 
     * [NOTE]
     * ====
     * Value must start with `http://` or `https://`
     * ====
     * 
     * @asciidoclet
     */
    @ConfigItem
    String url;

    /**
     * The minio server access key
     * 
     * @asciidoclet
     */
    @ConfigItem
    String accessKey;

    /**
     * The minio server secret key
     * 
     * @asciidoclet
     */
    @ConfigItem
    String secretKey;
}
