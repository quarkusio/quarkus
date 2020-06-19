package io.quarkus.qrs.runtime.mapping;

import javax.ws.rs.core.MediaType;

import io.quarkus.qrs.runtime.core.RestHandler;
import io.quarkus.qrs.runtime.spi.EndpointFactory;
import io.quarkus.qrs.runtime.spi.EndpointInvoker;

public class RuntimeResource {

    private final String httpMethod;
    private final URITemplate path;
    private final MediaType produces;
    private final MediaType consumes;
    private final EndpointInvoker invoker;
    private final EndpointFactory endpointFactory;
    private final RestHandler[] handlerChain;
    private final String method;
    private final Class<?>[] parameterTypes;
    private final Class<?> returnType;

    public RuntimeResource(String httpMethod, URITemplate path, MediaType produces, MediaType consumes, EndpointInvoker invoker,
            EndpointFactory endpointFactory, RestHandler[] handlerChain, String method, Class<?>[] parameterTypes,
            Class<?> returnType) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.produces = produces;
        this.consumes = consumes;
        this.invoker = invoker;
        this.endpointFactory = endpointFactory;
        this.handlerChain = handlerChain;
        this.method = method;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public RestHandler[] getHandlerChain() {
        return handlerChain;
    }

    public String getMethod() {
        return method;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public URITemplate getPath() {
        return path;
    }

    public MediaType getProduces() {
        return produces;
    }

    public MediaType getConsumes() {
        return consumes;
    }

    public EndpointInvoker getInvoker() {
        return invoker;
    }

    public EndpointFactory getEndpointFactory() {
        return endpointFactory;
    }

}
