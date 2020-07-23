package io.quarkus.qrs.runtime.mapping;

import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.model.ResourceWriter;
import io.quarkus.qrs.runtime.spi.BeanFactory;
import io.quarkus.qrs.runtime.spi.EndpointInvoker;

public class RuntimeResource {

    private final String httpMethod;
    private final URITemplate path;
    private final MediaType produces;
    private final MediaType consumes;
    private final EndpointInvoker invoker;
    private final BeanFactory<Object> endpointFactory;
    private final RestHandler[] handlerChain;
    private final String method;
    private final Class<?>[] parameterTypes;
    private final Type returnType;
    private final ResourceWriter<Object> buildTimeWriter;

    public RuntimeResource(String httpMethod, URITemplate path, MediaType produces, MediaType consumes, EndpointInvoker invoker,
            BeanFactory<Object> endpointFactory, RestHandler[] handlerChain, String method, Class<?>[] parameterTypes,
            Type returnType, ResourceWriter<Object> buildTimeWriter) {
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
        this.buildTimeWriter = buildTimeWriter;
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

    public Type getReturnType() {
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

    public BeanFactory<Object> getEndpointFactory() {
        return endpointFactory;
    }

    public ResourceWriter<Object> getBuildTimeWriter() {
        return buildTimeWriter;
    }

    @Override
    public String toString() {
        return "RuntimeResource{ method: " + method + ", path: " + path + "}";
    }
}
