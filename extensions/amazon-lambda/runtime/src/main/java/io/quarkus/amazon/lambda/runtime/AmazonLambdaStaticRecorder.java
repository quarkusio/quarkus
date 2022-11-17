package io.quarkus.amazon.lambda.runtime;

import java.util.Set;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AmazonLambdaStaticRecorder {
    public void setHandlerClass(Class<? extends RequestHandler<?, ?>> handler) {
        AmazonLambdaRecorder.initializeHandlerClass(handler);
    }

    public void setStreamHandlerClass(Class<? extends RequestStreamHandler> handler) {
        AmazonLambdaRecorder.streamHandlerClass = handler;
    }

    public void setExpectedExceptionClasses(Set<Class<?>> classes) {
        AmazonLambdaRecorder.expectedExceptionClasses = classes;
    }

}
