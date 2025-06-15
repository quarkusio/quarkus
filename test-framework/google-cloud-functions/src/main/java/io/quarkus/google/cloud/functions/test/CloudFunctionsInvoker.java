package io.quarkus.google.cloud.functions.test;

import com.google.cloud.functions.invoker.runner.Invoker;

class CloudFunctionsInvoker {

    private final Invoker invoker;

    CloudFunctionsInvoker(FunctionType functionType) {
        this(functionType, 8081);
    }

    CloudFunctionsInvoker(FunctionType functionType, int port) {
        int realPort = port == 0 ? SocketUtil.findAvailablePort() : port;
        if (realPort != port) {
            System.setProperty("quarkus.http.test-port", String.valueOf(realPort));
        }
        this.invoker = new Invoker(realPort, functionType.getTarget(), functionType.getSignatureType(),
                Thread.currentThread().getContextClassLoader());
    }

    void start() throws Exception {
        this.invoker.startTestServer();
    }

    void stop() throws Exception {
        this.invoker.stopServer();
    }
}
