package io.quarkus.amazon.lambda.runtime;

import java.util.Set;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder.RequestHandlerDefinition;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AmazonLambdaStaticRecorder {
    public void setHandlerClass(RequestHandlerDefinition requestHandlerDefinition) {
        AmazonLambdaRecorder.initializeHandlerClass(requestHandlerDefinition);
    }

    public void setStreamHandlerClass(Class<? extends RequestStreamHandler> handler) {
        AmazonLambdaRecorder.streamHandlerClass = handler;
    }

    public void setExpectedExceptionClasses(Set<Class<?>> classes) {
        AmazonLambdaRecorder.expectedExceptionClasses = classes;
    }

}
