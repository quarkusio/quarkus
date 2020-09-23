package io.quarkus.rest.runtime.mapping;

import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.handlers.RestHandler;
import io.quarkus.rest.runtime.spi.BeanFactory;
import io.quarkus.rest.runtime.spi.EndpointInvoker;
import io.quarkus.rest.runtime.util.ServerMediaType;

public class RuntimeResource {

    private final String httpMethod;
    private final URITemplate path;
    private final URITemplate classPath;
    private final ServerMediaType produces;
    private final MediaType consumes;
    private final EndpointInvoker invoker;
    private final BeanFactory<Object> endpointFactory;
    private final RestHandler[] handlerChain;
    private final String javaMethodName;
    private final Class<?>[] parameterTypes;
    private final Type returnType;
    private final boolean blocking;
    private final Class<?> resourceClass;
    private final LazyMethod lazyMethod;
    private final Map<String, Integer> pathParameterIndexes;

    public RuntimeResource(String httpMethod, URITemplate path, URITemplate classPath, ServerMediaType produces,
            MediaType consumes,
            EndpointInvoker invoker,
            BeanFactory<Object> endpointFactory, RestHandler[] handlerChain, String javaMethodName, Class<?>[] parameterTypes,
            Type returnType, boolean blocking, Class<?> resourceClass, LazyMethod lazyMethod,
            Map<String, Integer> pathParameterIndexes) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.classPath = classPath;
        this.produces = produces;
        this.consumes = consumes;
        this.invoker = invoker;
        this.endpointFactory = endpointFactory;
        this.handlerChain = handlerChain;
        this.javaMethodName = javaMethodName;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.blocking = blocking;
        this.resourceClass = resourceClass;
        this.lazyMethod = lazyMethod;
        this.pathParameterIndexes = pathParameterIndexes;
    }

    public RestHandler[] getHandlerChain() {
        return handlerChain;
    }

    public String getJavaMethodName() {
        return javaMethodName;
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

    public ServerMediaType getProduces() {
        return produces;
    }

    public MediaType getConsumes() {
        return consumes;
    }

    public EndpointInvoker getInvoker() {
        return invoker;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public Class<?> getResourceClass() {
        return resourceClass;
    }

    public BeanFactory<Object> getEndpointFactory() {
        return endpointFactory;
    }

    public LazyMethod getLazyMethod() {
        return lazyMethod;
    }

    /**
     * The @Path that is present on the class itself
     * 
     * @return
     */
    public URITemplate getClassPath() {
        return classPath;
    }

    public Map<String, Integer> getPathParameterIndexes() {
        return pathParameterIndexes;
    }

    @Override
    public String toString() {
        return "RuntimeResource{ method: " + javaMethodName + ", path: " + path + "}";
    }
}
