package io.quarkus.rest.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.rest.runtime.client.ClientProxies;
import io.quarkus.rest.runtime.core.ArcBeanFactory;
import io.quarkus.rest.runtime.core.ContextResolvers;
import io.quarkus.rest.runtime.core.DynamicFeatures;
import io.quarkus.rest.runtime.core.ExceptionMapping;
import io.quarkus.rest.runtime.core.Features;
import io.quarkus.rest.runtime.core.GenericTypeMapping;
import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.core.parameters.AsyncResponseExtractor;
import io.quarkus.rest.runtime.core.parameters.BeanParamExtractor;
import io.quarkus.rest.runtime.core.parameters.BodyParamExtractor;
import io.quarkus.rest.runtime.core.parameters.ContextParamExtractor;
import io.quarkus.rest.runtime.core.parameters.CookieParamExtractor;
import io.quarkus.rest.runtime.core.parameters.FormParamExtractor;
import io.quarkus.rest.runtime.core.parameters.HeaderParamExtractor;
import io.quarkus.rest.runtime.core.parameters.MatrixParamExtractor;
import io.quarkus.rest.runtime.core.parameters.ParameterExtractor;
import io.quarkus.rest.runtime.core.parameters.PathParamExtractor;
import io.quarkus.rest.runtime.core.parameters.QueryParamExtractor;
import io.quarkus.rest.runtime.core.serialization.DynamicEntityWriter;
import io.quarkus.rest.runtime.core.serialization.FixedEntityWriter;
import io.quarkus.rest.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.rest.runtime.handlers.AbortChainHandler;
import io.quarkus.rest.runtime.handlers.BlockingHandler;
import io.quarkus.rest.runtime.handlers.ClassRoutingHandler;
import io.quarkus.rest.runtime.handlers.CompletionStageResponseHandler;
import io.quarkus.rest.runtime.handlers.InputHandler;
import io.quarkus.rest.runtime.handlers.InstanceHandler;
import io.quarkus.rest.runtime.handlers.InterceptorHandler;
import io.quarkus.rest.runtime.handlers.InvocationHandler;
import io.quarkus.rest.runtime.handlers.MediaTypeMapper;
import io.quarkus.rest.runtime.handlers.MultiResponseHandler;
import io.quarkus.rest.runtime.handlers.ParameterHandler;
import io.quarkus.rest.runtime.handlers.PerRequestInstanceHandler;
import io.quarkus.rest.runtime.handlers.QuarkusRestInitialHandler;
import io.quarkus.rest.runtime.handlers.ReadBodyHandler;
import io.quarkus.rest.runtime.handlers.RequestDeserializeHandler;
import io.quarkus.rest.runtime.handlers.ResourceLocatorHandler;
import io.quarkus.rest.runtime.handlers.ResourceRequestInterceptorHandler;
import io.quarkus.rest.runtime.handlers.ResourceResponseInterceptorHandler;
import io.quarkus.rest.runtime.handlers.ResponseHandler;
import io.quarkus.rest.runtime.handlers.ResponseWriterHandler;
import io.quarkus.rest.runtime.handlers.RestHandler;
import io.quarkus.rest.runtime.handlers.SseResponseWriterHandler;
import io.quarkus.rest.runtime.handlers.UniResponseHandler;
import io.quarkus.rest.runtime.headers.FixedProducesHandler;
import io.quarkus.rest.runtime.headers.VariableProducesHandler;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestDynamicFeatureContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestFeatureContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResourceMethod;
import io.quarkus.rest.runtime.mapping.RequestMapper;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.mapping.URITemplate;
import io.quarkus.rest.runtime.model.MethodParameter;
import io.quarkus.rest.runtime.model.ParameterType;
import io.quarkus.rest.runtime.model.ResourceClass;
import io.quarkus.rest.runtime.model.ResourceContextResolver;
import io.quarkus.rest.runtime.model.ResourceDynamicFeature;
import io.quarkus.rest.runtime.model.ResourceExceptionMapper;
import io.quarkus.rest.runtime.model.ResourceFeature;
import io.quarkus.rest.runtime.model.ResourceInterceptors;
import io.quarkus.rest.runtime.model.ResourceMethod;
import io.quarkus.rest.runtime.model.ResourceReader;
import io.quarkus.rest.runtime.model.ResourceReaderInterceptor;
import io.quarkus.rest.runtime.model.ResourceRequestInterceptor;
import io.quarkus.rest.runtime.model.ResourceResponseInterceptor;
import io.quarkus.rest.runtime.model.ResourceWriter;
import io.quarkus.rest.runtime.model.ResourceWriterInterceptor;
import io.quarkus.rest.runtime.spi.BeanFactory;
import io.quarkus.rest.runtime.spi.EndpointInvoker;
import io.quarkus.rest.runtime.util.ServerMediaType;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QuarkusRestRecorder {

    private static final Logger log = Logger.getLogger(QuarkusRestRecorder.class);

    private static final Map<String, Class<?>> primitiveTypes;
    public static final Supplier<Executor> EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };

    private static final LinkedHashMap<ResourceRequestInterceptor, ContainerRequestFilter> EMPTY_INTERCEPTOR_REQUEST_MAP = new LinkedHashMap<>();
    private static final LinkedHashMap<ResourceResponseInterceptor, ContainerResponseFilter> EMPTY_INTERCEPTOR_RESPONSE_MAP = new LinkedHashMap<>();
    private static final LinkedHashMap<ResourceReaderInterceptor, ReaderInterceptor> EMPTY_INTERCEPTOR_READER_MAP = new LinkedHashMap<>();
    private static final LinkedHashMap<ResourceWriterInterceptor, WriterInterceptor> EMPTY_INTERCEPTOR_WRITER_MAP = new LinkedHashMap<>();
    public static final RestHandler[] EMPTY_REST_HANDLER_ARRAY = new RestHandler[0];

    private static volatile QuarkusRestDeployment currentDeployment;

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

    public static QuarkusRestDeployment getCurrentDeployment() {
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
            ContextResolvers ctxResolvers, Features features, DynamicFeatures dynamicFeatures, Serialisers serialisers,
            List<ResourceClass> resourceClasses, List<ResourceClass> locatableResourceClasses, BeanContainer beanContainer,
            ShutdownContext shutdownContext, QuarkusRestConfig quarkusRestConfig, HttpBuildTimeConfig vertxConfig,
            Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations, GenericTypeMapping genericTypeMapping) {
        DynamicEntityWriter dynamicEntityWriter = new DynamicEntityWriter(serialisers);

        QuarkusRestConfiguration quarkusRestConfiguration = configureFeatures(features, interceptors, exceptionMapping,
                beanContainer);
        boolean dynamicFeaturesExist = !dynamicFeatures.getResourceDynamicFeatures().isEmpty();

        Map<ResourceRequestInterceptor, ContainerRequestFilter> globalRequestInterceptorsMap = createContainerRequestFilterInstances(
                interceptors.getGlobalRequestInterceptors(), shutdownContext);

        Map<ResourceResponseInterceptor, ContainerResponseFilter> globalResponseInterceptorsMap = createContainerResponseFilterInstances(
                interceptors.getGlobalResponseInterceptors(), shutdownContext);

        Map<ResourceRequestInterceptor, ContainerRequestFilter> nameRequestInterceptorsMap = createContainerRequestFilterInstances(
                interceptors.getNameRequestInterceptors(), shutdownContext);

        Map<ResourceResponseInterceptor, ContainerResponseFilter> nameResponseInterceptorsMap = createContainerResponseFilterInstances(
                interceptors.getNameResponseInterceptors(), shutdownContext);

        Map<ResourceReaderInterceptor, ReaderInterceptor> globalReaderInterceptorsMap = createReaderInterceptorInstances(
                interceptors.getGlobalResourceReaderInterceptors(), shutdownContext);

        Map<ResourceWriterInterceptor, WriterInterceptor> globalWriterInterceptorsMap = createWriterInterceptorInstances(
                interceptors.getGlobalResourceWriterInterceptors(), shutdownContext);

        Map<ResourceReaderInterceptor, ReaderInterceptor> nameReaderInterceptorsMap = createReaderInterceptorInstances(
                interceptors.getNameResourceReaderInterceptors(), shutdownContext);

        Map<ResourceWriterInterceptor, WriterInterceptor> nameWriterInterceptorsMap = createWriterInterceptorInstances(
                interceptors.getNameResourceWriterInterceptors(), shutdownContext);

        ResourceResponseInterceptorHandler globalResourceResponseInterceptorHandler = new ResourceResponseInterceptorHandler(
                globalResponseInterceptorsMap.values());
        ResourceRequestInterceptorHandler globalRequestInterceptorsHandler = new ResourceRequestInterceptorHandler(
                globalRequestInterceptorsMap.values(), false);

        InterceptorHandler globalInterceptorHandler = null;
        if (!globalReaderInterceptorsMap.isEmpty() ||
                !globalWriterInterceptorsMap.isEmpty()) {
            WriterInterceptor[] writers = null;
            ReaderInterceptor[] readers = null;
            if (!globalReaderInterceptorsMap.isEmpty()) {
                readers = new ReaderInterceptor[globalReaderInterceptorsMap.size()];
                int idx = 0;
                for (ReaderInterceptor i : globalReaderInterceptorsMap.values()) {
                    readers[idx++] = i;
                }
            }
            if (!globalWriterInterceptorsMap.isEmpty()) {
                writers = new WriterInterceptor[globalWriterInterceptorsMap.size()];
                int idx = 0;
                for (WriterInterceptor i : globalWriterInterceptorsMap.values()) {
                    writers[idx++] = i;
                }
            }
            globalInterceptorHandler = new InterceptorHandler(writers, readers);

        }

        ResourceLocatorHandler resourceLocatorHandler = new ResourceLocatorHandler();
        List<ResourceClass> possibleSubResource = new ArrayList<>(locatableResourceClasses);
        possibleSubResource.addAll(resourceClasses); //the TCK uses normal resources also as sub resources
        for (ResourceClass clazz : possibleSubResource) {
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> templates = new HashMap<>();
            URITemplate classPathTemplate = clazz.getPath() == null ? null : new URITemplate(clazz.getPath(), true);
            for (ResourceMethod method : clazz.getMethods()) {
                //TODO: add DynamicFeature for these
                RuntimeResource runtimeResource = buildResourceMethod(serialisers, quarkusRestConfig,
                        globalRequestInterceptorsMap,
                        globalResponseInterceptorsMap,
                        globalRequestInterceptorsHandler, globalResourceResponseInterceptorHandler,
                        nameRequestInterceptorsMap, nameResponseInterceptorsMap,
                        Collections.emptyMap(), Collections.emptyMap(),
                        globalReaderInterceptorsMap,
                        globalWriterInterceptorsMap,
                        nameReaderInterceptorsMap,
                        nameWriterInterceptorsMap, globalInterceptorHandler, clazz,
                        resourceLocatorHandler, method,
                        true, classPathTemplate, dynamicEntityWriter, beanContainer);

                buildMethodMapper(templates, method, runtimeResource);
            }
            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = buildClassMapper(templates);
            resourceLocatorHandler.addResource(loadClass(clazz.getClassName()), mappersByMethod);
        }

        //it is possible that multiple resource classes use the same path
        //we use this map to merge them
        Map<URITemplate, Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>>> mappers = new TreeMap<>();

        for (ResourceClass clazz : resourceClasses) {
            URITemplate classTemplate = new URITemplate(clazz.getPath(), true);
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers = mappers
                    .get(classTemplate);
            if (perClassMappers == null) {
                mappers.put(classTemplate, perClassMappers = new HashMap<>());
            }
            for (ResourceMethod method : clazz.getMethods()) {
                Map<ResourceRequestInterceptor, ContainerRequestFilter> methodSpecificRequestInterceptorsMap = Collections
                        .emptyMap();
                Map<ResourceResponseInterceptor, ContainerResponseFilter> methodSpecificResponseInterceptorsMap = Collections
                        .emptyMap();

                if (dynamicFeaturesExist) {
                    // we'll basically just use this as a way to capture the registering of filters
                    // in the global fields
                    ResourceInterceptors dynamicallyConfiguredInterceptors = new ResourceInterceptors();

                    QuarkusRestResourceMethod quarkusRestResourceMethod = new QuarkusRestResourceMethod(clazz, method); // TODO: look into using LazyMethod
                    QuarkusRestDynamicFeatureContext context = new QuarkusRestDynamicFeatureContext(
                            dynamicallyConfiguredInterceptors, quarkusRestConfiguration, beanContainer);
                    for (ResourceDynamicFeature resourceDynamicFeature : dynamicFeatures.getResourceDynamicFeatures()) {
                        DynamicFeature feature = resourceDynamicFeature.getFactory().createInstance().getInstance();
                        feature.configure(quarkusRestResourceMethod, context);
                    }

                    if (!dynamicallyConfiguredInterceptors.getGlobalRequestInterceptors().isEmpty()) {
                        methodSpecificRequestInterceptorsMap = createContainerRequestFilterInstances(
                                dynamicallyConfiguredInterceptors.getGlobalRequestInterceptors(), shutdownContext);
                    }
                    if (!dynamicallyConfiguredInterceptors.getGlobalResponseInterceptors().isEmpty()) {
                        methodSpecificResponseInterceptorsMap = createContainerResponseFilterInstances(
                                dynamicallyConfiguredInterceptors.getGlobalResponseInterceptors(), shutdownContext);
                    }
                }

                RuntimeResource runtimeResource = buildResourceMethod(serialisers, quarkusRestConfig,
                        globalRequestInterceptorsMap,
                        globalResponseInterceptorsMap,
                        globalRequestInterceptorsHandler, globalResourceResponseInterceptorHandler, nameRequestInterceptorsMap,
                        nameResponseInterceptorsMap, methodSpecificRequestInterceptorsMap,
                        methodSpecificResponseInterceptorsMap,
                        globalReaderInterceptorsMap,
                        globalWriterInterceptorsMap,
                        nameReaderInterceptorsMap,
                        nameWriterInterceptorsMap, globalInterceptorHandler,
                        clazz, resourceLocatorHandler, method,
                        false, classTemplate, dynamicEntityWriter, beanContainer);

                buildMethodMapper(perClassMappers, method, runtimeResource);
            }

        }
        List<RequestMapper.RequestPath<QuarkusRestInitialHandler.InitialMatch>> classMappers = new ArrayList<>();
        for (Map.Entry<URITemplate, Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>>> entry : mappers
                .entrySet()) {
            URITemplate classTemplate = entry.getKey();
            int classTemplateNameCount = classTemplate.countPathParamNames();
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers = entry
                    .getValue();
            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = buildClassMapper(perClassMappers);
            ClassRoutingHandler classRoutingHandler = new ClassRoutingHandler(mappersByMethod, classTemplateNameCount);

            int maxMethodTemplateNameCount = 0;
            for (TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> i : perClassMappers.values()) {
                for (URITemplate j : i.keySet()) {
                    maxMethodTemplateNameCount = Math.max(maxMethodTemplateNameCount, j.countPathParamNames());
                }
            }
            classMappers.add(new RequestMapper.RequestPath<>(true, classTemplate,
                    new QuarkusRestInitialHandler.InitialMatch(new RestHandler[] { classRoutingHandler },
                            maxMethodTemplateNameCount + classTemplateNameCount)));
        }

        List<RestHandler> abortHandlingChain = new ArrayList<>();

        if (!interceptors.getGlobalResponseInterceptors().isEmpty()) {
            abortHandlingChain.add(globalResourceResponseInterceptorHandler);
        }
        abortHandlingChain.add(new ResponseHandler());
        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));
        QuarkusRestDeployment deployment = new QuarkusRestDeployment(exceptionMapping, ctxResolvers, serialisers,
                abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY), dynamicEntityWriter,
                createClientImpls(clientImplementations),
                vertxConfig.rootPath, genericTypeMapping);

        currentDeployment = deployment;

        //pre matching interceptors are run first
        ResourceRequestInterceptorHandler preMatchHandler = null;
        if (!interceptors.getResourcePreMatchRequestInterceptors().isEmpty()) {
            Map<ResourceRequestInterceptor, ContainerRequestFilter> preMatchContainerRequestFilters = createContainerRequestFilterInstances(
                    interceptors.getResourcePreMatchRequestInterceptors(), shutdownContext);
            preMatchHandler = new ResourceRequestInterceptorHandler(preMatchContainerRequestFilters.values(), true);
        }

        return new QuarkusRestInitialHandler(new RequestMapper<>(classMappers), deployment, preMatchHandler);
    }

    private ClientProxies createClientImpls(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations) {
        Map<Class<?>, Function<WebTarget, ?>> map = new HashMap<>();
        for (Map.Entry<String, RuntimeValue<Function<WebTarget, ?>>> entry : clientImplementations.entrySet()) {
            map.put(loadClass(entry.getKey()), entry.getValue().getValue());
        }
        return new ClientProxies(map);
    }

    //TODO: this needs plenty more work to support all possible types and provide all information the FeatureContext allows
    private QuarkusRestConfiguration configureFeatures(Features features, ResourceInterceptors interceptors,
            ExceptionMapping exceptionMapping,
            BeanContainer beanContainer) {

        QuarkusRestConfiguration configuration = new QuarkusRestConfiguration(RuntimeType.SERVER);
        if (features.getResourceFeatures().isEmpty()) {
            return configuration;
        }

        QuarkusRestFeatureContext featureContext = new QuarkusRestFeatureContext(interceptors, exceptionMapping,
                configuration, beanContainer);
        List<ResourceFeature> resourceFeatures = features.getResourceFeatures();
        for (ResourceFeature resourceFeature : resourceFeatures) {
            Feature feature = resourceFeature.getFactory().createInstance().getInstance();
            boolean enabled = feature.configure(featureContext);
            if (enabled) {
                configuration.addEnabledFeature(feature);
            }
        }
        if (featureContext.isFiltersNeedSorting()) {
            interceptors.sort();
        }
        return configuration;
    }

    // we need to preserve the order of ResourceRequestInterceptor because they have been sorted according to priorities
    private LinkedHashMap<ResourceRequestInterceptor, ContainerRequestFilter> createContainerRequestFilterInstances(
            List<ResourceRequestInterceptor> interceptors, ShutdownContext shutdownContext) {

        if (interceptors.isEmpty()) {
            return EMPTY_INTERCEPTOR_REQUEST_MAP;
        }

        LinkedHashMap<ResourceRequestInterceptor, ContainerRequestFilter> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<ContainerRequestFilter>> responseBeanInstances = new ArrayList<>(interceptors.size());
        for (ResourceRequestInterceptor interceptor : interceptors) {
            BeanFactory.BeanInstance<ContainerRequestFilter> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            ContainerRequestFilter containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);

        }
        shutdownContext.addShutdownTask(
                new ShutdownContext.CloseRunnable(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances)));
        return result;
    }

    // we need to preserve the order of ResourceResponseInterceptor because they have been sorted according to priorities
    private LinkedHashMap<ResourceResponseInterceptor, ContainerResponseFilter> createContainerResponseFilterInstances(
            List<ResourceResponseInterceptor> interceptors, ShutdownContext shutdownContext) {

        if (interceptors.isEmpty()) {
            return EMPTY_INTERCEPTOR_RESPONSE_MAP;
        }

        LinkedHashMap<ResourceResponseInterceptor, ContainerResponseFilter> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<ContainerResponseFilter>> responseBeanInstances = new ArrayList<>(interceptors.size());
        for (ResourceResponseInterceptor interceptor : interceptors) {
            BeanFactory.BeanInstance<ContainerResponseFilter> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            ContainerResponseFilter containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);

        }
        shutdownContext.addShutdownTask(
                new ShutdownContext.CloseRunnable(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances)));
        return result;
    }

    private LinkedHashMap<ResourceReaderInterceptor, ReaderInterceptor> createReaderInterceptorInstances(
            List<ResourceReaderInterceptor> interceptors, ShutdownContext shutdownContext) {

        if (interceptors.isEmpty()) {
            return EMPTY_INTERCEPTOR_READER_MAP;
        }

        LinkedHashMap<ResourceReaderInterceptor, ReaderInterceptor> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<ReaderInterceptor>> responseBeanInstances = new ArrayList<>(interceptors.size());
        Collections.sort(interceptors);
        for (ResourceReaderInterceptor interceptor : interceptors) {
            BeanFactory.BeanInstance<ReaderInterceptor> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            ReaderInterceptor containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);
        }
        shutdownContext.addShutdownTask(
                new ShutdownContext.CloseRunnable(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances)));
        return result;
    }

    private LinkedHashMap<ResourceWriterInterceptor, WriterInterceptor> createWriterInterceptorInstances(
            List<ResourceWriterInterceptor> interceptors, ShutdownContext shutdownContext) {

        if (interceptors.isEmpty()) {
            return EMPTY_INTERCEPTOR_WRITER_MAP;
        }
        Collections.sort(interceptors);

        LinkedHashMap<ResourceWriterInterceptor, WriterInterceptor> result = new LinkedHashMap<>();
        List<BeanFactory.BeanInstance<WriterInterceptor>> responseBeanInstances = new ArrayList<>(interceptors.size());
        for (ResourceWriterInterceptor interceptor : interceptors) {
            BeanFactory.BeanInstance<WriterInterceptor> beanInstance = interceptor.getFactory().createInstance();
            responseBeanInstances.add(beanInstance);
            WriterInterceptor containerResponseFilter = beanInstance.getInstance();
            result.put(interceptor, containerResponseFilter);
        }
        shutdownContext.addShutdownTask(
                new ShutdownContext.CloseRunnable(new BeanFactory.BeanInstance.ClosingTask<>(responseBeanInstances)));
        return result;
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
                    RuntimeResource fake = new RuntimeResource(i.getKey(), entry.getKey(), null, null, Collections.emptyList(),
                            null, null,
                            new RestHandler[] { mapper }, null, new Class[0], null, false, null, null, null);
                    result.add(new RequestMapper.RequestPath<>(false, fake.getPath(), fake));
                }
            }
            mappersByMethod.put(i.getKey(), new RequestMapper<>(result));
        }
        return mappersByMethod;
    }

    public RuntimeResource buildResourceMethod(Serialisers serialisers,
            QuarkusRestConfig quarkusRestConfig,
            Map<ResourceRequestInterceptor, ContainerRequestFilter> globalRequestInterceptorsMap,
            Map<ResourceResponseInterceptor, ContainerResponseFilter> globalResponseInterceptorsMap,
            ResourceRequestInterceptorHandler globalRequestInterceptorsHandler,
            ResourceResponseInterceptorHandler globalResponseInterceptorHandler,
            Map<ResourceRequestInterceptor, ContainerRequestFilter> nameRequestInterceptorsMap,
            Map<ResourceResponseInterceptor, ContainerResponseFilter> nameResponseInterceptorsMap,
            Map<ResourceRequestInterceptor, ContainerRequestFilter> methodSpecificRequestInterceptorsMap,
            Map<ResourceResponseInterceptor, ContainerResponseFilter> methodSpecificResponseInterceptorsMap,
            Map<ResourceReaderInterceptor, ReaderInterceptor> globalReaderInterceptorsMap,
            Map<ResourceWriterInterceptor, WriterInterceptor> globalWriterInterceptorsMap,
            Map<ResourceReaderInterceptor, ReaderInterceptor> nameReaderInterceptorsMap,
            Map<ResourceWriterInterceptor, WriterInterceptor> nameWriterInterceptorsMap,
            InterceptorHandler globalInterceptorHandler,
            ResourceClass clazz, ResourceLocatorHandler resourceLocatorHandler,
            ResourceMethod method, boolean locatableResource, URITemplate classPathTemplate,
            DynamicEntityWriter dynamicEntityWriter, BeanContainer beanContainer) {
        URITemplate methodPathTemplate = new URITemplate(method.getPath(), false);
        List<RestHandler> abortHandlingChain = new ArrayList<>();

        Map<String, Integer> pathParameterIndexes = buildParamIndexMap(classPathTemplate, methodPathTemplate);
        List<RestHandler> handlers = new ArrayList<>();
        List<MediaType> consumesMediaTypes;
        if (method.getConsumes() == null) {
            consumesMediaTypes = Collections.emptyList();
        } else {
            consumesMediaTypes = new ArrayList<>(method.getConsumes().length);
            for (String s : method.getConsumes()) {
                consumesMediaTypes.add(MediaType.valueOf(s));
            }
        }

        //setup reader and writer interceptors first
        if (method.getNameBindingNames().isEmpty() && nameReaderInterceptorsMap.isEmpty()
                && nameWriterInterceptorsMap.isEmpty()) {
            if (globalInterceptorHandler != null) {
                handlers.add(globalInterceptorHandler);
            }
        } else if (nameReaderInterceptorsMap.isEmpty() && nameWriterInterceptorsMap.isEmpty()) {
            // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
            if (globalInterceptorHandler != null) {
                handlers.add(globalInterceptorHandler);
            }
        } else {
            TreeMap<ResourceReaderInterceptor, ReaderInterceptor> readerInterceptorsToUse = new TreeMap<>();
            readerInterceptorsToUse.putAll(globalReaderInterceptorsMap);

            TreeMap<ResourceWriterInterceptor, WriterInterceptor> writerInterceptorsToUse = new TreeMap<>();
            writerInterceptorsToUse.putAll(globalWriterInterceptorsMap);
            for (ResourceReaderInterceptor nameInterceptor : nameReaderInterceptorsMap.keySet()) {
                // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
                if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                    readerInterceptorsToUse.put(nameInterceptor, nameReaderInterceptorsMap.get(nameInterceptor));
                }
            }
            for (ResourceWriterInterceptor nameInterceptor : nameWriterInterceptorsMap.keySet()) {
                // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
                if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                    writerInterceptorsToUse.put(nameInterceptor, nameWriterInterceptorsMap.get(nameInterceptor));
                }
            }
            WriterInterceptor[] writers = null;
            ReaderInterceptor[] readers = null;
            if (!readerInterceptorsToUse.isEmpty()) {
                readers = new ReaderInterceptor[readerInterceptorsToUse.size()];
                int idx = 0;
                for (ReaderInterceptor i : readerInterceptorsToUse.values()) {
                    readers[idx++] = i;
                }
            }
            if (!writerInterceptorsToUse.isEmpty()) {
                writers = new WriterInterceptor[writerInterceptorsToUse.size()];
                int idx = 0;
                for (WriterInterceptor i : writerInterceptorsToUse.values()) {
                    writers[idx++] = i;
                }
            }
            handlers.add(new InterceptorHandler(writers, readers));
        }
        //at this point the handler chain only has interceptors
        //which we also want in the abort handler chain
        abortHandlingChain.addAll(handlers);

        // according to the spec, global request filters apply everywhere
        // and named request filters only apply to methods with exactly matching "qualifiers"
        if (method.getNameBindingNames().isEmpty() && methodSpecificRequestInterceptorsMap.isEmpty()) {
            handlers.add(globalRequestInterceptorsHandler);
        } else if (nameRequestInterceptorsMap.isEmpty() && methodSpecificRequestInterceptorsMap.isEmpty()) {
            // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
            handlers.add(globalRequestInterceptorsHandler);
        } else {
            List<ResourceRequestInterceptor> interceptorsToUse = new ArrayList<>(
                    globalRequestInterceptorsMap.size() + nameRequestInterceptorsMap.size()
                            + methodSpecificRequestInterceptorsMap.size());
            interceptorsToUse.addAll(globalRequestInterceptorsMap.keySet());
            interceptorsToUse.addAll(methodSpecificRequestInterceptorsMap.keySet());
            for (ResourceRequestInterceptor nameInterceptor : nameRequestInterceptorsMap.keySet()) {
                // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
                if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                    interceptorsToUse.add(nameInterceptor);
                }
            }
            // since we have now mixed global, name and method specific interceptors, we need to sort
            Collections.sort(interceptorsToUse);
            List<ContainerRequestFilter> filtersToUse = new ArrayList<>(interceptorsToUse.size());
            for (ResourceRequestInterceptor interceptor : interceptorsToUse) {
                Map<ResourceRequestInterceptor, ContainerRequestFilter> properMap;
                if (interceptor.getNameBindingNames().isEmpty()) {
                    if (methodSpecificRequestInterceptorsMap.containsKey(interceptor)) {
                        properMap = methodSpecificRequestInterceptorsMap;
                    } else {
                        properMap = globalRequestInterceptorsMap;
                    }
                } else {
                    properMap = nameRequestInterceptorsMap;
                }
                filtersToUse.add(properMap.get(interceptor));
            }
            handlers.add(new ResourceRequestInterceptorHandler(filtersToUse, false));
        }

        Class<?>[] parameterTypes = new Class[method.getParameters().length];
        for (int i = 0; i < method.getParameters().length; ++i) {
            parameterTypes[i] = loadClass(method.getParameters()[i].declaredType);
        }
        // some parameters need the body to be read
        MethodParameter[] parameters = method.getParameters();
        // body can only be in a parameter
        MethodParameter bodyParameter = null;
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter param = parameters[i];
            if (param.parameterType == ParameterType.BODY) {
                bodyParameter = param;
                break;
            }
        }
        // form params can be everywhere (field, beanparam, param)
        if (method.isFormParamRequired()) {
            // read the body as multipart in one go
            handlers.add(new ReadBodyHandler(bodyParameter != null));
        } else if (bodyParameter != null) {
            // allow the body to be read by chunks
            handlers.add(new InputHandler(quarkusRestConfig.inputBufferSize.asLongValue(), EXECUTOR_SUPPLIER));
        }
        // if we need the body, let's deserialise it
        if (bodyParameter != null) {
            handlers.add(new RequestDeserializeHandler(loadClass(bodyParameter.type),
                    consumesMediaTypes.isEmpty() ? null : consumesMediaTypes.get(0), serialisers));
        }

        // given that we may inject form params in the endpoint we need to make sure we read the body before
        // we create/inject our endpoint
        EndpointInvoker invoker = method.getInvoker().get();
        if (!locatableResource) {
            if (clazz.isPerRequestResource()) {
                handlers.add(new PerRequestInstanceHandler(clazz.getFactory()));
            } else {
                handlers.add(new InstanceHandler(clazz.getFactory()));
            }
        }

        for (int i = 0; i < parameters.length; i++) {
            MethodParameter param = parameters[i];
            boolean single = param.isSingle();
            ParameterExtractor extractor = parameterExtractor(pathParameterIndexes, param.parameterType, param.type, param.name,
                    single, beanContainer);
            handlers.add(new ParameterHandler(i, param.getDefaultValue(), extractor,
                    param.converter == null ? null : param.converter.get(), param.parameterType,
                    param.isObtainedAsCollection()));
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
        handlers.add(new MultiResponseHandler());
        ServerMediaType serverMediaType = null;
        if (method.getProduces() != null && method.getProduces().length > 0) {
            serverMediaType = new ServerMediaType(method.getProduces(), StandardCharsets.UTF_8.name());
        }
        if (method.getHttpMethod() == null) {
            //this is a resource locator method
            handlers.add(resourceLocatorHandler);
        } else if (!Response.class.isAssignableFrom(rawNonAsyncReturnType)) {
            //try and statically determine the media type and response writer
            //we can't do this for all cases, but we can do it for the most common ones
            //in practice this should work for the majority of endpoints
            if (method.getProduces() != null && method.getProduces().length > 0) {
                //the method can only produce a single content type, which is the most common case
                if (method.getProduces().length == 1) {
                    MediaType mediaType = MediaType.valueOf(method.getProduces()[0]);
                    //its a wildcard type, makes it hard to determine statically
                    if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
                        handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                    } else {
                        List<ResourceWriter> buildTimeWriters = serialisers.findBuildTimeWriters(rawNonAsyncReturnType,
                                RuntimeType.SERVER, method.getProduces());
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

        //the response filter handlers, they need to be added to both the abort and
        //normal chains. At the moment this only has one handler added to it but
        //in future there will be one per filter
        List<RestHandler> responseFilterHandlers = new ArrayList<>();
        if (method.isSse()) {
            handlers.add(new SseResponseWriterHandler());
        } else {
            handlers.add(new ResponseHandler());

            // according to the spec, global request filters apply everywhere
            // and named request filters only apply to methods with exactly matching "qualifiers"
            if (method.getNameBindingNames().isEmpty() && methodSpecificResponseInterceptorsMap.isEmpty()) {
                responseFilterHandlers.add(globalResponseInterceptorHandler);
            } else if (nameResponseInterceptorsMap.isEmpty() && methodSpecificResponseInterceptorsMap.isEmpty()) {
                // in this case there are no filters that match the qualifiers, so let's just reuse the global handler
                responseFilterHandlers.add(globalResponseInterceptorHandler);
            } else {
                List<ResourceResponseInterceptor> interceptorsToUse = new ArrayList<>(
                        globalResponseInterceptorsMap.size() + nameResponseInterceptorsMap.size()
                                + methodSpecificResponseInterceptorsMap.size());
                interceptorsToUse.addAll(globalResponseInterceptorsMap.keySet());
                interceptorsToUse.addAll(methodSpecificResponseInterceptorsMap.keySet());
                for (ResourceResponseInterceptor nameInterceptor : nameResponseInterceptorsMap.keySet()) {
                    // in order to the interceptor to be used, the method needs to have all the "qualifiers" that the interceptor has
                    if (method.getNameBindingNames().containsAll(nameInterceptor.getNameBindingNames())) {
                        interceptorsToUse.add(nameInterceptor);
                    }
                }
                // since we have now mixed global, name and method specific interceptors, we need to sort
                Collections.sort(interceptorsToUse);
                List<ContainerResponseFilter> filtersToUse = new ArrayList<>(interceptorsToUse.size());
                for (ResourceResponseInterceptor interceptor : interceptorsToUse) {
                    Map<ResourceResponseInterceptor, ContainerResponseFilter> properMap;
                    if (interceptor.getNameBindingNames().isEmpty()) {
                        if (methodSpecificResponseInterceptorsMap.containsKey(interceptor)) {
                            properMap = methodSpecificResponseInterceptorsMap;
                        } else {
                            properMap = globalResponseInterceptorsMap;
                        }
                    } else {
                        properMap = nameResponseInterceptorsMap;
                    }
                    filtersToUse.add(properMap.get(interceptor));
                }
                responseFilterHandlers.add(new ResourceResponseInterceptorHandler(filtersToUse));
            }
            handlers.addAll(responseFilterHandlers);
            handlers.add(new ResponseWriterHandler(dynamicEntityWriter));
        }
        abortHandlingChain.addAll(responseFilterHandlers);

        abortHandlingChain.add(new ResponseHandler());
        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));
        handlers.add(0, new AbortChainHandler(abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY)));

        Class<Object> resourceClass = loadClass(clazz.getClassName());
        return new RuntimeResource(method.getHttpMethod(), methodPathTemplate,
                classPathTemplate,
                method.getProduces() == null ? null : serverMediaType,
                consumesMediaTypes, invoker,
                clazz.getFactory(), handlers.toArray(EMPTY_REST_HANDLER_ARRAY), method.getName(), parameterTypes,
                nonAsyncReturnType, method.isBlocking(), resourceClass,
                new LazyMethod(method.getName(), resourceClass, parameterTypes),
                pathParameterIndexes);
    }

    public ParameterExtractor parameterExtractor(Map<String, Integer> pathParameterIndexes, ParameterType type, String javaType,
            String name,
            boolean single, BeanContainer beanContainer) {
        ParameterExtractor extractor;
        switch (type) {
            case HEADER:
                extractor = new HeaderParamExtractor(name, single);
                break;
            case COOKIE:
                extractor = new CookieParamExtractor(name);
                break;
            case FORM:
                extractor = new FormParamExtractor(name, single);
                break;
            case PATH:
                extractor = new PathParamExtractor(pathParameterIndexes.get(name));
                break;
            case CONTEXT:
                extractor = new ContextParamExtractor(javaType);
                break;
            case ASYNC_RESPONSE:
                extractor = new AsyncResponseExtractor();
                break;
            case QUERY:
                extractor = new QueryParamExtractor(name, single);
                break;
            case BODY:
                extractor = new BodyParamExtractor();
                break;
            case MATRIX:
                extractor = new MatrixParamExtractor(name, single);
                break;
            case BEAN:
                extractor = new BeanParamExtractor(factory(javaType, beanContainer));
                break;
            default:
                extractor = new QueryParamExtractor(name, single);
                break;
        }
        return extractor;
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
            if (type.getRawType() == Multi.class) {
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

    public void registerContextResolver(ContextResolvers contextResolvers, String string,
            ResourceContextResolver resolver) {
        contextResolvers.addContextResolver(loadClass(string), resolver);
    }

    public void registerFeature(Features features, ResourceFeature feature) {
        features.addFeature(feature);
    }

    public void registerDynamicFeature(DynamicFeatures dynamicFeatures, ResourceDynamicFeature dynamicFeature) {
        dynamicFeatures.addFeature(dynamicFeature);
    }

    public void registerWriter(Serialisers serialisers, String entityClassName,
            ResourceWriter writer) {
        serialisers.addWriter(loadClass(entityClassName), writer);
    }

    public void registerReader(Serialisers serialisers, String entityClassName,
            ResourceReader reader) {
        serialisers.addReader(loadClass(entityClassName), reader);
    }

    public void registerInvocationHandlerGenericType(GenericTypeMapping genericTypeMapping, String invocationHandlerClass,
            String resolvedType) {
        genericTypeMapping.addInvocationCallback(loadClass(invocationHandlerClass), loadClass(resolvedType));
    }
}
