package io.quarkus.google.cloud.functions.test;

import com.google.cloud.functions.invoker.runner.Invoker;

class CloudFunctionsInvoker {

    private final Invoker invoker;
    private final int port;

    CloudFunctionsInvoker(FunctionType functionType, int port) {
        int realPort = port == 0 ? SocketUtil.findAvailablePort() : port;
        this.invoker = new Invoker(
                realPort,
                functionType.getTarget(),
                functionType.getSignatureType(),
                Thread.currentThread().getContextClassLoader());
        this.port = realPort;
    }

    public int actualPort() {
        return port;
    }

    void start() throws Exception {
        this.invoker.startTestServer();
    }

    void stop() throws Exception {
        this.invoker.stopServer();
    }
}
