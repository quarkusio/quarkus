package io.quarkus.amazon.lambda.http;

import java.util.regex.Pattern;

public class LambdaHttpRecorder {
    /**
     * @deprecated Properly use the config object
     */
    @Deprecated
    static volatile LambdaHttpConfig config;
    /**
     * @deprecated Properly use the config object
     */
    @Deprecated
    static volatile Pattern groupPattern;

    public static void setConfig(LambdaHttpConfig runtimeConfig) {
        config = runtimeConfig;
        groupPattern = Pattern.compile(runtimeConfig.cognitoClaimMatcher());
    }
}
