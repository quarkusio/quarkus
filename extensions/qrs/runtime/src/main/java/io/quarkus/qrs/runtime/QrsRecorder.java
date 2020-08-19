package io.quarkus.qrs.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.qrs.runtime.core.ArcBeanFactory;
import io.quarkus.qrs.runtime.core.ExceptionMapping;
import io.quarkus.qrs.runtime.core.LazyMethod;
import io.quarkus.qrs.runtime.core.QrsDeployment;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.core.parameters.AsyncResponseExtractor;
import io.quarkus.qrs.runtime.core.parameters.BodyParamExtractor;
import io.quarkus.qrs.runtime.core.parameters.ContextParamExtractor;
import io.quarkus.qrs.runtime.core.parameters.FormParamExtractor;
import io.quarkus.qrs.runtime.core.parameters.HeaderParamExtractor;
import io.quarkus.qrs.runtime.core.parameters.ParameterExtractor;
import io.quarkus.qrs.runtime.core.parameters.PathParamExtractor;
import io.quarkus.qrs.runtime.core.parameters.QueryParamExtractor;
import io.quarkus.qrs.runtime.core.serialization.DynamicEntityWriter;
import io.quarkus.qrs.runtime.core.serialization.FixedEntityWriter;
import io.quarkus.qrs.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.qrs.runtime.handlers.BlockingHandler;
import io.quarkus.qrs.runtime.handlers.ClassRoutingHandler;
import io.quarkus.qrs.runtime.handlers.CompletionStageResponseHandler;
import io.quarkus.qrs.runtime.handlers.InputHandler;
import io.quarkus.qrs.runtime.handlers.InstanceHandler;
import io.quarkus.qrs.runtime.handlers.InvocationHandler;
import io.quarkus.qrs.runtime.handlers.MediaTypeMapper;
import io.quarkus.qrs.runtime.handlers.ParameterHandler;
import io.quarkus.qrs.runtime.handlers.QrsInitialHandler;
import io.quarkus.qrs.runtime.handlers.ReadBodyHandler;
import io.quarkus.qrs.runtime.handlers.RequestDeserializeHandler;
import io.quarkus.qrs.runtime.handlers.ResourceLocatorHandler;
import io.quarkus.qrs.runtime.handlers.ResourceRequestInterceptorHandler;
import io.quarkus.qrs.runtime.handlers.ResourceResponseInterceptorHandler;
import io.quarkus.qrs.runtime.handlers.ResponseHandler;
import io.quarkus.qrs.runtime.handlers.ResponseWriterHandler;
import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.handlers.UniResponseHandler;
import io.quarkus.qrs.runtime.headers.FixedProducesHandler;
import io.quarkus.qrs.runtime.headers.VariableProducesHandler;
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
import io.quarkus.qrs.runtime.util.ServerMediaType;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QrsRecorder {

    private static final Logger log = Logger.getLogger(QrsRecorder.class);

    private static final Map<String, Class<?>> primitiveTypes;
    public static final Supplier<Executor> EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };

    private static volatile QrsDeployment currentDeployment;

    static {
        Map<String, Class<?>> prims = new HashMap<>();
        prims.put(byte.class.getName(), byte.class);
        prims.put(boolean.class.getName(), boolean.class);
        prims.put(char.class.getName(), char.class);
        prims.put(short.class.getName(), short.class);
        prims.put(int.class.getName(), int.class);
        prims.put(float.class.getName(), float.class);
        prims.put(double.class.getName(), double.class);
        prims.put(long.class.getName(), long.class);
        primitiveTypes = Collections.unmodifiableMap(prims);
    }

    public static QrsDeployment getCurrentDeployment() {
        return currentDeployment;
    }

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
            List<ResourceClass> resourceClasses, List<ResourceClass> locatableResourceClasses,
            ShutdownContext shutdownContext, QrsConfig qrsConfig) {
        DynamicEntityWriter dynamicEntityWriter = new DynamicEntityWriter(serialisers);
        //pre matching interceptors are run first
        List<ResourceRequestInterceptor> requestInterceptors = interceptors.getRequestInterceptors();
        List<ResourceResponseInterceptor> responseInterceptors = interceptors.getResponseInterceptors();

        ResourceResponseInterceptorHandler resourceResponseInterceptorHandler = new ResourceResponseInterceptorHandler(
                responseInterceptors, shutdownContext);
        ResourceRequestInterceptorHandler requestInterceptorsHandler = new ResourceRequestInterceptorHandler(
                requestInterceptors, shutdownContext, false);
        ResourceRequestInterceptorHandler preMatchHandler = null;
        if (!interceptors.getResourcePreMatchRequestInterceptors().isEmpty()) {
            preMatchHandler = new ResourceRequestInterceptorHandler(interceptors.getResourcePreMatchRequestInterceptors(),
                    shutdownContext, true);
        }
        ResourceLocatorHandler resourceLocatorHandler = new ResourceLocatorHandler();
        for (ResourceClass clazz : locatableResourceClasses) {
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> templates = new HashMap<>();
            URITemplate classPathTemplate = clazz.getPath() == null ? null : new URITemplate(clazz.getPath(), true);
            for (ResourceMethod method : clazz.getMethods()) {
                RuntimeResource runtimeResource = buildResourceMethod(serialisers, qrsConfig, requestInterceptors,
                        responseInterceptors,
                        resourceResponseInterceptorHandler, requestInterceptorsHandler, clazz, resourceLocatorHandler, method,
                        true, classPathTemplate, dynamicEntityWriter);

                buildMethodMapper(templates, method, runtimeResource);
            }
            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = buildClassMapper(templates);
            resourceLocatorHandler.addResource(loadClass(clazz.getClassName()), mappersByMethod);
        }
        List<RequestMapper.RequestPath<QrsInitialHandler.InitialMatch>> classMappers = new ArrayList<>();
        for (ResourceClass clazz : resourceClasses) {
            URITemplate classTemplate = new URITemplate(clazz.getPath(), true);
            int classTemplateNameCount = classTemplate.countPathParamNames();
            int maxMethodTemplateNameCount = 0;
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers = new HashMap<>();
            for (ResourceMethod method : clazz.getMethods()) {
                RuntimeResource runtimeResource = buildResourceMethod(serialisers, qrsConfig, requestInterceptors,
                        responseInterceptors,
                        resourceResponseInterceptorHandler, requestInterceptorsHandler, clazz, resourceLocatorHandler, method,
                        false, classTemplate, dynamicEntityWriter);

                buildMethodMapper(perClassMappers, method, runtimeResource);
                maxMethodTemplateNameCount = Math.max(maxMethodTemplateNameCount,
                        runtimeResource.getPath().countPathParamNames());
            }

            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = buildClassMapper(perClassMappers);
            ClassRoutingHandler classRoutingHandler = new ClassRoutingHandler(mappersByMethod, classTemplateNameCount);
            classMappers.add(new RequestMapper.RequestPath<>(true, classTemplate,
                    new QrsInitialHandler.InitialMatch(new RestHandler[] { classRoutingHandler },
                            maxMethodTemplateNameCount + classTemplateNameCount)));
        }

        List<RestHandler> abortHandlingChain = new ArrayList<>();

        if (!responseInterceptors.isEmpty()) {
            abortHandlingChain.add(resourceResponseInterceptorHandler);
        }
        abortHandlingChain.add(new ResponseHandler());
        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));
        QrsDeployment deployment = new QrsDeployment(exceptionMapping, serialisers,
                abortHandlingChain.toArray(new RestHandler[0]), dynamicEntityWriter);

        currentDeployment = deployment;
        return new QrsInitialHandler(new RequestMapper<>(classMappers), deployment, preMatchHandler);
    }

    public void buildMethodMapper(
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers,
            ResourceMethod method, RuntimeResource runtimeResource) {
        TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> templateMap = perClassMappers
                .get(method.getHttpMethod());
        if (templateMap == null) {
            perClassMappers.put(method.getHttpMethod(), templateMap = new TreeMap<>());
        }
        List<RequestMapper.RequestPath<RuntimeResource>> list = templateMap.get(runtimeResource.getPath());
        if (list == null) {
            templateMap.put(runtimeResource.getPath(), list = new ArrayList<>());
        }
        list.add(new RequestMapper.RequestPath<>(method.getHttpMethod() == null, runtimeResource.getPath(),
                runtimeResource));
    }

    public Map<String, RequestMapper<RuntimeResource>> buildClassMapper(
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers) {
        Map<String, RequestMapper<RuntimeResource>> mappersByMethod = new HashMap<>();
        SortedMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> nullMethod = perClassMappers.get(null);
        if (nullMethod == null) {
            nullMethod = Collections.emptySortedMap();
        }
        for (Map.Entry<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> i : perClassMappers
                .entrySet()) {
            for (Map.Entry<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> nm : nullMethod.entrySet()) {
                TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> templateMap = i.getValue();
                if (!templateMap.containsKey(nm.getKey())) {
                    //resource methods take precedence
                    //just skip sub resource locators for now
                    //may need to be revisited if we want to pass the TCK 100%
                    templateMap.put(nm.getKey(), nm.getValue());
                }
            }
            //now we have all our possible resources
            List<RequestMapper.RequestPath<RuntimeResource>> result = new ArrayList<>();
            for (Map.Entry<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> entry : i.getValue().entrySet()) {
                if (entry.getValue().size() == 1) {
                    //simple case, only one match
                    result.addAll(entry.getValue());
                } else {
                    List<RuntimeResource> resources = new ArrayList<>();
                    for (RequestMapper.RequestPath<RuntimeResource> val : entry.getValue()) {
                        resources.add(val.value);
                    }
                    MediaTypeMapper mapper = new MediaTypeMapper(resources);
                    //now we just create a fake RuntimeResource
                    //we could add another layer of indirection, however this is not a common case
                    //so we don't want to add any extra latency into the common case
                    RuntimeResource fake = new RuntimeResource(i.getKey(), entry.getKey(), null, null, null, null, null,
                            new RestHandler[] { mapper }, null, new Class[0], null, false, null, null);
                    result.add(new RequestMapper.RequestPath<>(false, fake.getPath(), fake));
                }
            }
            mappersByMethod.put(i.getKey(), new RequestMapper<>(result));
        }
        return mappersByMethod;
    }

    public RuntimeResource buildResourceMethod(Serialisers serialisers,
            QrsConfig qrsConfig,
            List<ResourceRequestInterceptor> requestInterceptors,
            List<ResourceResponseInterceptor> responseInterceptors,
            ResourceResponseInterceptorHandler resourceResponseInterceptorHandler,
            ResourceRequestInterceptorHandler requestInterceptorsHandler,
            ResourceClass clazz, ResourceLocatorHandler resourceLocatorHandler,
            ResourceMethod method, boolean locatableResource, URITemplate classPathTemplate,
            DynamicEntityWriter dynamicEntityWriter) {
        URITemplate methodPathTemplate = new URITemplate(method.getPath(), false);

        Map<String, Integer> pathParameterIndexes = buildParamIndexMap(classPathTemplate, methodPathTemplate);
        List<RestHandler> handlers = new ArrayList<>();
        MediaType consumesMediaType = method.getConsumes() == null ? null : MediaType.valueOf(method.getConsumes()[0]);

        if (!requestInterceptors.isEmpty()) {
            handlers.add(requestInterceptorsHandler);
        }

        EndpointInvoker invoker = method.getInvoker().get();
        if (!locatableResource) {
            handlers.add(new InstanceHandler(clazz.getFactory()));
        }
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
            } else if (param.parameterType == ParameterType.BODY) {
                handlers.add(new InputHandler(qrsConfig.inputBufferSize.asLongValue(), EXECUTOR_SUPPLIER));
                handlers.add(new RequestDeserializeHandler(loadClass(param.type), consumesMediaType, serialisers));
            }
        }
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter param = parameters[i];
            ParameterExtractor extractor;
            boolean single = param.isSingle();
            switch (param.parameterType) {
                case HEADER:
                    extractor = new HeaderParamExtractor(param.name, single);
                    break;
                case FORM:
                    extractor = new FormParamExtractor(param.name, single);
                    break;
                case PATH:
                    extractor = new PathParamExtractor(pathParameterIndexes.get(param.name));
                    break;
                case CONTEXT:
                    extractor = new ContextParamExtractor(param.type);
                    break;
                case ASYNC_RESPONSE:
                    extractor = new AsyncResponseExtractor();
                    break;
                case QUERY:
                    extractor = new QueryParamExtractor(param.name, single);
                    break;
                case BODY:
                    extractor = new BodyParamExtractor();
                    break;
                default:
                    extractor = new QueryParamExtractor(param.name, single);
                    break;
            }
            handlers.add(new ParameterHandler(i, param.getDefaultValue(), extractor,
                    param.converter == null ? null : param.converter.get()));
        }
        if (method.isBlocking()) {
            handlers.add(new BlockingHandler(EXECUTOR_SUPPLIER));
        }
        handlers.add(new InvocationHandler(invoker));

        Type returnType = TypeSignatureParser.parse(method.getReturnType());
        Type nonAsyncReturnType = getNonAsyncReturnType(returnType);
        Class<Object> rawNonAsyncReturnType = (Class<Object>) getRawType(nonAsyncReturnType);

        // FIXME: those two should not be in sequence unless we intend to support CompletionStage<Uni<String>>
        handlers.add(new CompletionStageResponseHandler());
        handlers.add(new UniResponseHandler());
        ServerMediaType serverMediaType = null;
        if (method.getHttpMethod() == null) {
            //this is a resource locator method
            handlers.add(resourceLocatorHandler);
        } else if (!Response.class.isAssignableFrom(rawNonAsyncReturnType)) {
            //try and statically determine the media type and response writer
            //we can't do this for all cases, but we can do it for the most common ones
            //in practice this should work for the majority of endpoints
            if (method.getProduces() != null && method.getProduces().length > 0) {
                serverMediaType = new ServerMediaType(method.getProduces(), StandardCharsets.UTF_8.name());
                //the method can only produce a single content type, which is the most common case
                if (method.getProduces().length == 1) {
                    MediaType mediaType = MediaType.valueOf(method.getProduces()[0]);
                    //its a wildcard type, makes it hard to determine statically
                    if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
                        handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                    } else {
                        List<ResourceWriter> buildTimeWriters = serialisers.findBuildTimeWriters(rawNonAsyncReturnType,
                                method.getProduces());
                        if (buildTimeWriters == null) {
                            //if this is null this means that the type cannot be resolved at build time
                            //this happens when the method returns a generic type (e.g. Object), so there
                            //are more specific mappers that could be invoked depending on the actual return value
                            handlers.add(new FixedProducesHandler(mediaType, dynamicEntityWriter));
                        } else if (buildTimeWriters.isEmpty()) {
                            //we could not find any writers that can write a response to this endpoint
                            log.warn("Cannot find any combination of response writers for the method " + clazz.getClassName()
                                    + "#" + method.getName() + "(" + Arrays.toString(method.getParameters()) + ")");
                            handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                        } else if (buildTimeWriters.size() == 1) {
                            //only a single handler that can handle the response
                            //this is a very common case
                            handlers.add(new FixedProducesHandler(mediaType, new FixedEntityWriter(
                                    buildTimeWriters.get(0).getInstance(), mediaType)));
                        } else {
                            //multiple writers, we try them in order
                            List<MessageBodyWriter<?>> list = new ArrayList<>();
                            for (ResourceWriter i : buildTimeWriters) {
                                list.add(i.getInstance());
                            }
                            handlers.add(new FixedProducesHandler(mediaType,
                                    new FixedEntityWriterArray(list.toArray(new MessageBodyWriter[0]))));
                        }
                    }
                } else {
                    //there are multiple possibilities
                    //we could optimise this more in future
                    handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                }
            }
        }

        handlers.add(new ResponseHandler());

        if (!responseInterceptors.isEmpty()) {
            handlers.add(resourceResponseInterceptorHandler);
        }
        handlers.add(new ResponseWriterHandler(dynamicEntityWriter));

        Class<Object> resourceClass = loadClass(clazz.getClassName());
        return new RuntimeResource(method.getHttpMethod(), methodPathTemplate,
                classPathTemplate,
                method.getProduces() == null ? null : serverMediaType,
                consumesMediaType, invoker,
                clazz.getFactory(), handlers.toArray(new RestHandler[0]), method.getName(), parameterTypes,
                nonAsyncReturnType, method.isBlocking(), resourceClass,
                new LazyMethod(method.getName(), resourceClass, parameterTypes));
    }

    public Map<String, Integer> buildParamIndexMap(URITemplate classPathTemplate, URITemplate methodPathTemplate) {
        Map<String, Integer> pathParameterIndexes = new HashMap<>();
        int pathCount = 0;
        if (classPathTemplate != null) {
            for (URITemplate.TemplateComponent i : classPathTemplate.components) {
                if (i.name != null) {
                    pathParameterIndexes.put(i.name, pathCount++);
                } else if (i.names != null) {
                    for (String nm : i.names) {
                        pathParameterIndexes.put(nm, pathCount++);
                    }
                }
            }
        }
        for (URITemplate.TemplateComponent i : methodPathTemplate.components) {
            if (i.name != null) {
                pathParameterIndexes.put(i.name, pathCount++);
            } else if (i.names != null) {
                for (String nm : i.names) {
                    pathParameterIndexes.put(nm, pathCount++);
                }
            }
        }
        return pathParameterIndexes;
    }

    private Class<?> getRawType(Type type) {
        if (type instanceof Class)
            return (Class<?>) type;
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            return (Class<?>) ptype.getRawType();
        }
        throw new UnsupportedOperationException("Endpoint return type not supported yet: " + type);
    }

    private Type getNonAsyncReturnType(Type returnType) {
        if (returnType instanceof Class)
            return returnType;
        if (returnType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) returnType;
            if (type.getRawType() == CompletionStage.class) {
                return type.getActualTypeArguments()[0];
            }
            if (type.getRawType() == Uni.class) {
                return type.getActualTypeArguments()[0];
            }
            return returnType;
        }
        throw new UnsupportedOperationException("Endpoint return type not supported yet: " + returnType);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> loadClass(String name) {
        if (primitiveTypes.containsKey(name)) {
            return (Class<T>) primitiveTypes.get(name);
        }
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
            ResourceWriter writer) {
        serialisers.addWriter(loadClass(entityClassName), writer);
    }

    public void registerReader(Serialisers serialisers, String entityClassName,
            ResourceReader reader) {
        serialisers.addReader(loadClass(entityClassName), reader);
    }
}
