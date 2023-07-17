package io.quarkus.google.cloud.functions.test;

import com.google.cloud.functions.invoker.runner.Invoker;

class CloudFunctionsInvoker {

    private final Invoker invoker;

    CloudFunctionsInvoker(FunctionType functionType) {
        this(functionType, 8081);
    }

    CloudFunctionsInvoker(FunctionType functionType, int port) {
        this.invoker = new Invoker(
                port,
                functionType.getTarget(),
                functionType.getSignatureType(),
                Thread.currentThread().getContextClassLoader());
    }

    void start() throws Exception {
        this.invoker.startTestServer();
    }

    void stop() throws Exception {
        this.invoker.stopServer();
    }
}
