package org.jboss.resteasy.reactive.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

public class ServerResourceMethod extends ResourceMethod {

    private Supplier<EndpointInvoker> invoker;

    private Set<String> methodAnnotationNames;

    private List<HandlerChainCustomizer> handlerChainCustomizers = new ArrayList<>();
    private ParameterExtractor customerParameterExtractor;

    public ServerResourceMethod() {
    }

    public ServerResourceMethod(String httpMethod, String path, String[] produces, String sseElementType, String[] consumes,
            Set<String> nameBindingNames, String name, String returnType, String simpleReturnType, MethodParameter[] parameters,
            boolean blocking, boolean suspended, boolean sse, boolean formParamRequired, boolean multipart,
            List<ResourceMethod> subResourceMethods, Supplier<EndpointInvoker> invoker, Set<String> methodAnnotationNames,
            List<HandlerChainCustomizer> handlerChainCustomizers, ParameterExtractor customerParameterExtractor) {
        super(httpMethod, path, produces, sseElementType, consumes, nameBindingNames, name, returnType, simpleReturnType,
                parameters, blocking, suspended, sse, formParamRequired, multipart, subResourceMethods);
        this.invoker = invoker;
        this.methodAnnotationNames = methodAnnotationNames;
        this.handlerChainCustomizers = handlerChainCustomizers;
        this.customerParameterExtractor = customerParameterExtractor;
    }

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
