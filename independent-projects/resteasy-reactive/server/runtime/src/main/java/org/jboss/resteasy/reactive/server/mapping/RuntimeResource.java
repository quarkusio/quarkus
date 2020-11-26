package org.jboss.resteasy.reactive.server.mapping;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.SimplifiedResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveSimplifiedResourceInfo;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.server.util.ScoreSystem;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.EndpointInvoker;

public class RuntimeResource {

    private final String httpMethod;
    private final URITemplate path;
    private final URITemplate classPath;
    private final ServerMediaType produces;
    private final List<MediaType> consumes;
    private final EndpointInvoker invoker;
    private final BeanFactory<Object> endpointFactory;
    private final ServerRestHandler[] handlerChain;
    private final String javaMethodName;
    private final Class<?>[] parameterTypes;
    private final Type returnType;
    private final boolean blocking;
    private final Class<?> resourceClass;
    private final LazyMethod lazyMethod;
    private final Map<String, Integer> pathParameterIndexes;
    private final Map<ScoreSystem.Category, List<ScoreSystem.Diagnostic>> score;
    private final MediaType sseElementType;
    private final Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> classExceptionMappers;

    public RuntimeResource(String httpMethod, URITemplate path, URITemplate classPath, ServerMediaType produces,
            List<MediaType> consumes,
            EndpointInvoker invoker,
            BeanFactory<Object> endpointFactory, ServerRestHandler[] handlerChain, String javaMethodName,
            Class<?>[] parameterTypes,
            Type returnType, boolean blocking, Class<?> resourceClass, LazyMethod lazyMethod,
            Map<String, Integer> pathParameterIndexes, Map<ScoreSystem.Category, List<ScoreSystem.Diagnostic>> score,
            MediaType sseElementType,
            Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> classExceptionMappers) {
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
        this.score = score;
        this.sseElementType = sseElementType;
        this.classExceptionMappers = classExceptionMappers;
    }

    public ServerRestHandler[] getHandlerChain() {
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

    public List<MediaType> getConsumes() {
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

    public SimplifiedResourceInfo getSimplifiedResourceInfo() {
        return new ResteasyReactiveSimplifiedResourceInfo(javaMethodName, resourceClass, parameterTypes);
    }

    public MediaType getSseElementType() {
        return sseElementType;
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

    public Map<ScoreSystem.Category, List<ScoreSystem.Diagnostic>> getScore() {
        return score;
    }

    public Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> getClassExceptionMappers() {
        return classExceptionMappers;
    }

    @Override
    public String toString() {
        return "RuntimeResource{ method: " + javaMethodName + ", path: " + path + "}";
    }
}
