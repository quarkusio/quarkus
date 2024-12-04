package io.quarkus.amazon.lambda.http.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.lambda-http")
public interface LambdaHttpBuildTimeConfig {
    /**
     * Enable security mechanisms to process lambda and AWS based security (i.e. Cognito, IAM) from
     * the http event sent from API Gateway
     */
    @WithDefault("false")
    boolean enableSecurity();
}
