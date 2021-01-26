package org.jboss.resteasy.reactive.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

public class ServerResourceMethod extends ResourceMethod {

    private Supplier<EndpointInvoker> invoker;

    private Set<String> methodAnnotationNames;

    private List<HandlerChainCustomizer> handlerChainCustomizers = new ArrayList<>();
    private ParameterExtractor customerParameterExtractor;

    public Supplier<EndpointInvoker> getInvoker() {
        return invoker;
    }

    public ResourceMethod setInvoker(Supplier<EndpointInvoker> invoker) {
        this.invoker = invoker;
        return this;
    }

    public Set<String> getMethodAnnotationNames() {
        return methodAnnotationNames;
    }

    public void setMethodAnnotationNames(Set<String> methodAnnotationNames) {
        this.methodAnnotationNames = methodAnnotationNames;
    }

    public List<HandlerChainCustomizer> getHandlerChainCustomizers() {
        return handlerChainCustomizers;
    }

    public ServerResourceMethod setHandlerChainCustomizers(List<HandlerChainCustomizer> handlerChainCustomizers) {
        this.handlerChainCustomizers = handlerChainCustomizers;
        return this;
    }

    public ParameterExtractor getCustomerParameterExtractor() {
        return customerParameterExtractor;
    }

    public ServerResourceMethod setCustomerParameterExtractor(ParameterExtractor customerParameterExtractor) {
        this.customerParameterExtractor = customerParameterExtractor;
        return this;
    }
}
