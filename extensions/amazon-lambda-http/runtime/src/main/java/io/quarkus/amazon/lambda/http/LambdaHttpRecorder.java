package io.quarkus.amazon.lambda.http;

import java.util.regex.Pattern;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LambdaHttpRecorder {
    /**
     * @deprecated Properly use the config object
     */
    @Deprecated
    static LambdaHttpConfig config;
    /**
     * @deprecated Properly use the config object
     */
    @Deprecated
    static Pattern groupPattern;

    private final RuntimeValue<LambdaHttpConfig> runtimeConfig;

    public LambdaHttpRecorder(final RuntimeValue<LambdaHttpConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void setConfig() {
        config = runtimeConfig.getValue();
        groupPattern = Pattern.compile(runtimeConfig.getValue().cognitoClaimMatcher());
    }
}
