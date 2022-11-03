package io.quarkus.funqy.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import io.quarkus.runtime.Quarkus;

/**
 * Intended to be used within a java runtime lambda deployment.
 * This handler is a tiny wrapper for the app-developer-defined lambda handler.
 * It delegates to a wrapper that uses arc to instantiate the
 * app-developer's handler. Jackson is used to (de)serialize input and output types of app-dev's handler.
 *
 */
public class FunqyStreamHandler implements RequestStreamHandler {
    public FunqyStreamHandler() {
        Quarkus.manualInitialize();
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Quarkus.manualStart();

        FunqyLambdaBindingRecorder.handle(inputStream, outputStream, context);
    }
}
