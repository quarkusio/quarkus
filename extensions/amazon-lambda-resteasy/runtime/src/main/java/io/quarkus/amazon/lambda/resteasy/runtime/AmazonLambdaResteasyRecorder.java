package io.quarkus.amazon.lambda.resteasy.runtime;

import java.util.Map;

import io.quarkus.amazon.lambda.resteasy.runtime.container.StreamLambdaHandler;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AmazonLambdaResteasyRecorder {

    public void initHandler(Map<String, String> initParameters, AmazonLambdaResteasyConfig config) {
        StreamLambdaHandler.initHandler(initParameters, config.debug);
    }
}
