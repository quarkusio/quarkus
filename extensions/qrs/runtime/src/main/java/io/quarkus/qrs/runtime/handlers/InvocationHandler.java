package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.spi.EndpointInvoker;

public class InvocationHandler implements RestHandler {
    private final EndpointInvoker invoker;

    public InvocationHandler(EndpointInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        try {
            Object result = invoker.invoke(requestContext.getEndpointInstance(), requestContext.getParameters());
            requestContext.setResult(result);
        } catch (Throwable t) {
            requestContext.setThrowable(t);
        }
    }
}
