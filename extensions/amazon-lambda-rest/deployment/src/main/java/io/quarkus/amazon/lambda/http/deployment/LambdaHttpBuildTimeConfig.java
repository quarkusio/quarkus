package io.quarkus.amazon.lambda.http.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class LambdaHttpBuildTimeConfig {
    /**
     * Enable security mechanisms to process lambda and AWS based security (i.e. Cognito, IAM) from
     * the http event sent from API Gateway
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableSecurity;
}
