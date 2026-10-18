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
    private String actualDeclaringClassName;
    private String classDeclMethodThatHasJaxRsEndpointDefiningAnn;

    public ServerResourceMethod() {
    }

    public ServerResourceMethod(String httpMethod, String path, String[] produces, String streamElementType, String[] consumes,
            Set<String> nameBindingNames, String name, String returnType, String simpleReturnType, MethodParameter[] parameters,
            boolean blocking, boolean suspended, boolean sse, boolean formParamRequired,
            List<ResourceMethod> subResourceMethods, Supplier<EndpointInvoker> invoker, Set<String> methodAnnotationNames,
            List<HandlerChainCustomizer> handlerChainCustomizers, ParameterExtractor customerParameterExtractor,
            boolean encoded) {
        super(httpMethod, path, produces, streamElementType, consumes, nameBindingNames, name, returnType, simpleReturnType,
                parameters, blocking, suspended, sse, formParamRequired, subResourceMethods, encoded);
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

    public String getActualDeclaringClassName() {
        return actualDeclaringClassName;
    }

    public void setActualDeclaringClassName(String actualDeclaringClassName) {
        this.actualDeclaringClassName = actualDeclaringClassName;
    }

    /**
     * Returns a declaring class name of a resource method annotated with Jakarta REST endpoint defining annotations.
     * This class can be different to {@link #getActualDeclaringClassName()} when this method is overridden on subclasses,
     * or when method-level {@link jakarta.ws.rs.Path} is defined on non-default interface method.
     *
     * @return declaring class name if different to {@link #getActualDeclaringClassName()} or null
     */
    public String getClassDeclMethodThatHasJaxRsEndpointDefiningAnn() {
        return classDeclMethodThatHasJaxRsEndpointDefiningAnn;
    }

    /**
     * Sets a declaring class name of a resource method annotated with Jakarta REST endpoint defining annotations.
     * Should only be set when the name is different to {@link #getActualDeclaringClassName()}.
     *
     * @param classDeclMethodThatHasJaxRsEndpointDefiningAnn class name
     */
    public void setClassDeclMethodThatHasJaxRsEndpointDefiningAnn(String classDeclMethodThatHasJaxRsEndpointDefiningAnn) {
        this.classDeclMethodThatHasJaxRsEndpointDefiningAnn = classDeclMethodThatHasJaxRsEndpointDefiningAnn;
    }
}
