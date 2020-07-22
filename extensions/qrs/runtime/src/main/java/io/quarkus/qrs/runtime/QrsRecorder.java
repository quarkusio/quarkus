package io.quarkus.qrs.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.ws.rs.core.MediaType;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.qrs.runtime.core.ArcBeanFactory;
import io.quarkus.qrs.runtime.core.ContextParamExtractor;
import io.quarkus.qrs.runtime.core.ExceptionMapping;
import io.quarkus.qrs.runtime.core.FormParamExtractor;
import io.quarkus.qrs.runtime.core.HeaderParamExtractor;
import io.quarkus.qrs.runtime.core.ParameterExtractor;
import io.quarkus.qrs.runtime.core.PathParamExtractor;
import io.quarkus.qrs.runtime.core.QueryParamExtractor;
import io.quarkus.qrs.runtime.core.ResourceRequestInterceptorHandler;
import io.quarkus.qrs.runtime.core.ResourceResponseInterceptorHandler;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.handlers.BlockingHandler;
import io.quarkus.qrs.runtime.handlers.InstanceHandler;
import io.quarkus.qrs.runtime.handlers.InvocationHandler;
import io.quarkus.qrs.runtime.handlers.ParameterHandler;
import io.quarkus.qrs.runtime.handlers.QrsInitialHandler;
import io.quarkus.qrs.runtime.handlers.ReadBodyHandler;
import io.quarkus.qrs.runtime.handlers.ResponseHandler;
import io.quarkus.qrs.runtime.handlers.ResponseWriterHandler;
import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.mapping.RequestMapper;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.quarkus.qrs.runtime.mapping.URITemplate;
import io.quarkus.qrs.runtime.model.MethodParameter;
import io.quarkus.qrs.runtime.model.ParameterType;
import io.quarkus.qrs.runtime.model.ResourceClass;
import io.quarkus.qrs.runtime.model.ResourceExceptionMapper;
import io.quarkus.qrs.runtime.model.ResourceInterceptors;
import io.quarkus.qrs.runtime.model.ResourceMethod;
import io.quarkus.qrs.runtime.model.ResourceReader;
import io.quarkus.qrs.runtime.model.ResourceRequestInterceptor;
import io.quarkus.qrs.runtime.model.ResourceResponseInterceptor;
import io.quarkus.qrs.runtime.model.ResourceWriter;
import io.quarkus.qrs.runtime.spi.BeanFactory;
import io.quarkus.qrs.runtime.spi.EndpointInvoker;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QrsRecorder {

    public <T> BeanFactory<T> factory(String targetClass, BeanContainer beanContainer) {
        return new ArcBeanFactory<>(loadClass(targetClass),
                beanContainer);
    }

    public Supplier<EndpointInvoker> invoker(String baseName) {
        return new Supplier<EndpointInvoker>() {
            @Override
            public EndpointInvoker get() {
                try {
                    return (EndpointInvoker) loadClass(baseName).newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException("Unable to generate endpoint invoker", e);
                }

            }
        };
    }

    public Handler<RoutingContext> handler(ResourceInterceptors interceptors,
            ExceptionMapping exceptionMapping,
            Serialisers serialisers,
            List<ResourceClass> resourceClasses,
            Executor blockingExecutor) {
        Map<String, RequestMapper<RuntimeResource>> mappersByMethod = new HashMap<>();
        Map<String, List<RequestMapper.RequestPath<RuntimeResource>>> templates = new HashMap<>();
        for (ResourceClass clazz : resourceClasses) {
            for (ResourceMethod method : clazz.getMethods()) {
                List<RestHandler> handlers = new ArrayList<>();

                List<ResourceRequestInterceptor> requestInterceptors = interceptors.getRequestInterceptors();
                if (!requestInterceptors.isEmpty()) {
                    handlers.add(new ResourceRequestInterceptorHandler(requestInterceptors));
                }

                EndpointInvoker invoker = method.getInvoker().get();
                handlers.add(new InstanceHandler(clazz.getFactory()));
                Class<?>[] parameterTypes = new Class[method.getParameters().length];
                for (int i = 0; i < method.getParameters().length; ++i) {
                    parameterTypes[i] = loadClass(method.getParameters()[i].type);
                }
                // some parameters need the body to be read
                MethodParameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    MethodParameter param = parameters[i];
                    if (param.parameterType == ParameterType.FORM) {
                        handlers.add(new ReadBodyHandler());
                        break;
                    }
                }
                for (int i = 0; i < parameters.length; i++) {
                    MethodParameter param = parameters[i];
                    ParameterExtractor extractor;
                    switch (param.parameterType) {
                        case HEADER:
                            extractor = new HeaderParamExtractor(param.name, true);
                            break;
                        case FORM:
                            extractor = new FormParamExtractor(param.name, true);
                            break;
                        case PATH:
                            extractor = new PathParamExtractor(param.name);
                            break;
                        case CONTEXT:
                            extractor = new ContextParamExtractor(param.type);
                            break;
                        default:
                            extractor = new QueryParamExtractor(param.name, true);
                            break;
                    }
                    handlers.add(new ParameterHandler(i, extractor, null));
                }
                if (method.isBlocking()) {
                    handlers.add(new BlockingHandler(blockingExecutor));
                }
                handlers.add(new InvocationHandler(invoker));
                handlers.add(new ResponseHandler());
                List<ResourceResponseInterceptor> responseInterceptors = interceptors.getResponseInterceptors();
                if (!responseInterceptors.isEmpty()) {
                    handlers.add(new ResourceResponseInterceptorHandler(responseInterceptors));
                }
                handlers.add(new ResponseWriterHandler());
                RuntimeResource resource = new RuntimeResource(method.getMethod(), new URITemplate(method.getPath()),
                        method.getProduces() == null ? null : MediaType.valueOf(method.getProduces()[0]),
                        method.getConsumes() == null ? null : MediaType.valueOf(method.getConsumes()[0]), invoker,
                        clazz.getFactory(), handlers.toArray(new RestHandler[0]), method.getName(), parameterTypes,
                        loadClass(method.getReturnType()));
                List<RequestMapper.RequestPath<RuntimeResource>> list = templates.get(method.getMethod());
                if (list == null) {
                    templates.put(method.getMethod(), list = new ArrayList<>());
                }
                list.add(new RequestMapper.RequestPath<>(resource.getPath(), resource));
            }
        }
        List<RequestMapper.RequestPath<RuntimeResource>> nullMethod = templates.remove(null);
        if (nullMethod == null) {
            nullMethod = Collections.emptyList();
        }
        for (Map.Entry<String, List<RequestMapper.RequestPath<RuntimeResource>>> i : templates.entrySet()) {
            i.getValue().addAll(nullMethod);
            mappersByMethod.put(i.getKey(), new RequestMapper<>(i.getValue()));
        }
        return new QrsInitialHandler(mappersByMethod, exceptionMapping, serialisers);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> loadClass(String name) {
        try {
            return (Class<T>) Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerExceptionMapper(ExceptionMapping exceptionMapping, String string,
            ResourceExceptionMapper<Throwable> mapper) {
        exceptionMapping.addExceptionMapper(loadClass(string), mapper);
    }

    public void registerWriter(Serialisers serialisers, String entityClassName,
            ResourceWriter<?> writer) {
        serialisers.addWriter(loadClass(entityClassName), writer);
    }

    public void registerReader(Serialisers serialisers, String entityClassName,
            ResourceReader<?> reader) {
        serialisers.addReader(loadClass(entityClassName), reader);
    }
}
