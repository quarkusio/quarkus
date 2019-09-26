package io.quarkus.amazon.lambda.resteasy.runtime.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class StreamLambdaHandler implements RequestStreamHandler {

    private static ResteasyLambdaContainerHandler handler;

    public StreamLambdaHandler() throws ReflectiveOperationException {
        final Class<?> aClass = Class.forName("io.quarkus.runner.ApplicationImpl1");

        aClass.getMethod("doStart", String[].class)
                .invoke(aClass.newInstance(), new Object[] { new String[0] });
    }

    public static void initHandler(Map<String, String> initParameters, boolean debugMode) {
        if (debugMode) {
            Timer.enable();
        }
        StreamLambdaHandler.handler = ResteasyLambdaContainerHandler.getAwsProxyHandler(initParameters);
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}