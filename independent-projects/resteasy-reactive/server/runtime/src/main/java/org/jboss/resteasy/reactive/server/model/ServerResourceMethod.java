package org.jboss.resteasy.reactive.server.model;

import java.util.function.Supplier;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

public class ServerResourceMethod extends ResourceMethod {

    private Supplier<EndpointInvoker> invoker;

    public Supplier<EndpointInvoker> getInvoker() {
        return invoker;
    }

    public ResourceMethod setInvoker(Supplier<EndpointInvoker> invoker) {
        this.invoker = invoker;
        return this;
    }
}
