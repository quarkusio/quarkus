package io.quarkus.amazon.lambda.http;

import java.util.regex.Pattern;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LambdaHttpRecorder {
    static LambdaHttpConfig config;
    static Pattern groupPattern;

    public void setConfig(LambdaHttpConfig c) {
        config = c;
        String pattern = c.cognitoClaimMatcher();
        groupPattern = Pattern.compile(pattern);
    }
}
