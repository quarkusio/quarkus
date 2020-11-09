package io.quarkus.rest.server.runtime;

import java.io.Closeable;
import java.lang.reflect.Method;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.core.SingletonBeanFactory;
import org.jboss.resteasy.reactive.common.core.ThreadSetupAction;
import org.jboss.resteasy.reactive.common.jaxrs.QuarkusRestConfiguration;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceContextResolver;
import org.jboss.resteasy.reactive.common.model.ResourceDynamicFeature;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.common.model.ResourceFeature;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.ContextResolvers;
import org.jboss.resteasy.reactive.server.core.DynamicFeatures;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.Features;
import org.jboss.resteasy.reactive.server.core.LazyMethod;
import org.jboss.resteasy.reactive.server.core.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.core.QuarkusRestDeployment;
import org.jboss.resteasy.reactive.server.core.QuarkusRestDeploymentInfo;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.RuntimeInterceptorDeployment;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.parameters.AsyncResponseExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.BeanParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.BodyParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.ContextParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.CookieParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.FormParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.HeaderParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.MatrixParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.NullParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.PathParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.QueryParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.core.serialization.DynamicEntityWriter;
import org.jboss.resteasy.reactive.server.core.serialization.FixedEntityWriter;
import org.jboss.resteasy.reactive.server.core.serialization.FixedEntityWriterArray;
import org.jboss.resteasy.reactive.server.handlers.AbortChainHandler;
import org.jboss.resteasy.reactive.server.handlers.BlockingHandler;
import org.jboss.resteasy.reactive.server.handlers.ClassRoutingHandler;
import org.jboss.resteasy.reactive.server.handlers.CompletionStageResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ExceptionHandler;
import org.jboss.resteasy.reactive.server.handlers.FixedProducesHandler;
import org.jboss.resteasy.reactive.server.handlers.InputHandler;
import org.jboss.resteasy.reactive.server.handlers.InstanceHandler;
import org.jboss.resteasy.reactive.server.handlers.InvocationHandler;
import org.jboss.resteasy.reactive.server.handlers.MediaTypeMapper;
import org.jboss.resteasy.reactive.server.handlers.MultiResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ParameterHandler;
import org.jboss.resteasy.reactive.server.handlers.QuarkusRestInitialHandler;
import org.jboss.resteasy.reactive.server.handlers.ReadBodyHandler;
import org.jboss.resteasy.reactive.server.handlers.RequestDeserializeHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceLocatorHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseWriterHandler;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.handlers.SseResponseWriterHandler;
import org.jboss.resteasy.reactive.server.handlers.UniResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.VariableProducesHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestFeatureContext;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.jboss.resteasy.reactive.server.model.ServerMethodParameter;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestMessageBodyWriter;
import org.jboss.resteasy.reactive.server.util.RuntimeResourceVisitor;
import org.jboss.resteasy.reactive.server.util.ScoreSystem;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.EndpointInvoker;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.resteasy.reactive.common.runtime.ArcBeanFactory;
import io.quarkus.resteasy.reactive.common.runtime.ArcThreadSetupAction;
import io.quarkus.resteasy.reactive.common.runtime.QuarkusRestCommonRecorder;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QuarkusRestRecorder extends QuarkusRestCommonRecorder {

    private static final Logger log = Logger.getLogger(QuarkusRestRecorder.class);

    public static final Supplier<Executor> EXECUTOR_SUPPLIER = new Supplier<Executor>() {
        @Override
        public Executor get() {
            return ExecutorRecorder.getCurrent();
        }
    };

    public static final ServerRestHandler[] EMPTY_REST_HANDLER_ARRAY = new ServerRestHandler[0];

    private static volatile QuarkusRestDeployment currentDeployment;

    public static QuarkusRestDeployment getCurrentDeployment() {
        return currentDeployment;
    }

    public Handler<RoutingContext> handler(QuarkusRestDeploymentInfo info,
            BeanContainer beanContainer,
            ShutdownContext shutdownContext, HttpBuildTimeConfig vertxConfig,
            String applicationPath, BeanFactory<QuarkusRestInitialiser> initClassFactory) {

        ResourceInterceptors interceptors = info.getInterceptors();
        ServerSerialisers serialisers = info.getSerialisers();
        Features features = info.getFeatures();
        ExceptionMapping exceptionMapping = info.getExceptionMapping();
        List<ResourceClass> resourceClasses = info.getResourceClasses();
        List<ResourceClass> locatableResourceClasses = info.getLocatableResourceClasses();
        ParamConverterProviders paramConverterProviders = info.getParamConverterProviders();
        BlockingOperationSupport.setIoThreadDetector(new BlockingOperationSupport.IOThreadDetector() {
            @Override
            public boolean isBlockingAllowed() {
                return BlockingOperationControl.isBlockingAllowed();
            }
        });

        Supplier<Application> applicationSupplier = info.getApplicationSupplier();

        DynamicEntityWriter dynamicEntityWriter = new DynamicEntityWriter(serialisers);

        QuarkusRestConfiguration quarkusRestConfiguration = configureFeatures(features, interceptors, exceptionMapping,
                beanContainer);

        Consumer<Closeable> closeTaskHandler = new Consumer<Closeable>() {
            @Override
            public void accept(Closeable closeable) {
                shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(closeable));
            }
        };
        RuntimeInterceptorDeployment interceptorDeployment = new RuntimeInterceptorDeployment(info, quarkusRestConfiguration,
                closeTaskHandler);

        ResourceLocatorHandler resourceLocatorHandler = new ResourceLocatorHandler(new Function<Class<?>, Object>() {
            @Override
            public Object apply(Class<?> aClass) {
                try {
                    return beanContainer.instance(aClass);
                } catch (Exception e) {
                    throw new RuntimeException("Could not instantiate resource bean " + aClass
                            + " make sure it has a bean defining annotation", e);
                }
            }
        });
        List<ResourceClass> possibleSubResource = new ArrayList<>(locatableResourceClasses);
        possibleSubResource.addAll(resourceClasses); //the TCK uses normal resources also as sub resources
        for (ResourceClass clazz : possibleSubResource) {
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> templates = new HashMap<>();
            URITemplate classPathTemplate = clazz.getPath() == null ? null : new URITemplate(clazz.getPath(), true);
            for (ResourceMethod method : clazz.getMethods()) {
                //TODO: add DynamicFeature for these
                RuntimeResource runtimeResource = buildResourceMethod(serialisers, info.getConfig(),
                        interceptorDeployment.forMethod(clazz, method),
                        clazz,
                        resourceLocatorHandler, method,
                        true, classPathTemplate, dynamicEntityWriter, beanContainer, paramConverterProviders);

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

                RuntimeResource runtimeResource = buildResourceMethod(serialisers, info.getConfig(),
                        interceptorDeployment.forMethod(clazz, method),
                        clazz, resourceLocatorHandler, method,
                        false, classTemplate, dynamicEntityWriter, beanContainer, paramConverterProviders);

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
                    new QuarkusRestInitialHandler.InitialMatch(new ServerRestHandler[] { classRoutingHandler },
                            maxMethodTemplateNameCount + classTemplateNameCount)));
        }

        List<ServerRestHandler> abortHandlingChain = new ArrayList<>();

        if (interceptorDeployment.getGlobalInterceptorHandler() != null) {
            abortHandlingChain.add(interceptorDeployment.getGlobalInterceptorHandler());
        }
        abortHandlingChain.add(new ExceptionHandler());
        if (!interceptors.getContainerResponseFilters().getGlobalResourceInterceptors().isEmpty()) {
            abortHandlingChain.addAll(interceptorDeployment.getGlobalResponseInterceptorHandlers());
        }
        abortHandlingChain.add(new ResponseHandler());
        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));
        // sanitise the prefix for our usage to make it either an empty string, or something which starts with a / and does not
        // end with one
        String prefix = vertxConfig.rootPath;
        if (prefix != null) {
            prefix = sanitizePathPrefix(prefix);
        } else {
            prefix = "";
        }
        if ((applicationPath != null) && !applicationPath.isEmpty()) {
            prefix = prefix + sanitizePathPrefix(applicationPath);
        }
        QuarkusRestDeployment deployment = new QuarkusRestDeployment(exceptionMapping, info.getCtxResolvers(), serialisers,
                abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY), dynamicEntityWriter,
                prefix, paramConverterProviders, quarkusRestConfiguration, applicationSupplier,
                new ArcThreadSetupAction(Arc.container().requestContext()),
                new RequestContextFactory() {
                    @Override
                    public ResteasyReactiveRequestContext createContext(QuarkusRestDeployment deployment,
                            QuarkusRestProviders providers, RoutingContext context, ThreadSetupAction requestContext,
                            ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain) {
                        return new QuarkusRequestContext(deployment, providers, context, requestContext, handlerChain,
                                abortHandlerChain);
                    }
                });

        initClassFactory.createInstance().getInstance().init(deployment);

        currentDeployment = deployment;

        //pre matching interceptors are run first
        List<ResourceRequestFilterHandler> preMatchHandlers = null;
        if (!interceptors.getContainerRequestFilters().getPreMatchInterceptors().isEmpty()) {
            preMatchHandlers = new ArrayList<>(interceptorDeployment.getPreMatchContainerRequestFilters().size());
            for (ContainerRequestFilter containerRequestFilter : interceptorDeployment.getPreMatchContainerRequestFilters()
                    .values()) {
                preMatchHandlers.add(new ResourceRequestFilterHandler(containerRequestFilter, true));
            }
        }

        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            NotFoundExceptionMapper.classMappers = classMappers;
            RuntimeResourceVisitor.visitRuntimeResources(classMappers, ScoreSystem.ScoreVisitor);
        }

        return new QuarkusRestInitialHandler(new RequestMapper<>(classMappers), deployment, preMatchHandlers);
    }

    // TODO: don't use reflection to instantiate Application
    public Supplier<Application> handleApplication(final Class<? extends Application> applicationClass,
            final boolean singletonClassesEmpty) {
        Supplier<Application> applicationSupplier;
        if (singletonClassesEmpty) {
            applicationSupplier = new Supplier<Application>() {
                @Override
                public Application get() {
                    try {
                        return applicationClass.getConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } else {
            try {
                final Application application = applicationClass.getConstructor().newInstance();
                for (Object i : application.getSingletons()) {
                    SingletonBeanFactory.setInstance(i.getClass().getName(), i);
                }
                applicationSupplier = new Supplier<Application>() {
                    @Override
                    public Application get() {
                        return application;
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return applicationSupplier;
    }

    private String sanitizePathPrefix(String prefix) {
        prefix = prefix.trim();
        if (prefix.equals("/"))
            prefix = "";
        // add leading slash
        if (!prefix.startsWith("/"))
            prefix = "/" + prefix;
        // remove trailing slash
        if (prefix.endsWith("/"))
            prefix = prefix.substring(0, prefix.length() - 1);
        return prefix;
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
                configuration, new Function<Class<?>, BeanFactory<?>>() {
                    @Override
                    public BeanFactory<?> apply(Class aClass) {
                        return new ArcBeanFactory<>(aClass, beanContainer);
                    }
                });
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
                            new ServerRestHandler[] { mapper }, null, new Class[0], null, false, null, null, null, null, null,
                            Collections.emptyMap());
                    result.add(new RequestMapper.RequestPath<>(false, fake.getPath(), fake));
                }
            }
            mappersByMethod.put(i.getKey(), new RequestMapper<>(result));
        }
        return mappersByMethod;
    }

    public RuntimeResource buildResourceMethod(ServerSerialisers serialisers,
            ResteasyReactiveConfig quarkusRestConfig,
            RuntimeInterceptorDeployment.MethodInterceptorContext interceptorDeployment,
            ResourceClass clazz,
            ResourceLocatorHandler resourceLocatorHandler,
            ResourceMethod method, boolean locatableResource, URITemplate classPathTemplate,
            DynamicEntityWriter dynamicEntityWriter, BeanContainer beanContainer,
            ParamConverterProviders paramConverterProviders) {
        URITemplate methodPathTemplate = new URITemplate(method.getPath(), false);
        List<ServerRestHandler> abortHandlingChain = new ArrayList<>();
        MultivaluedMap<ScoreSystem.Category, ScoreSystem.Diagnostic> score = new QuarkusMultivaluedHashMap<>();

        Map<String, Integer> pathParameterIndexes = buildParamIndexMap(classPathTemplate, methodPathTemplate);
        List<ServerRestHandler> handlers = new ArrayList<>();
        MediaType sseElementType = null;
        if (method.getSseElementType() != null) {
            sseElementType = MediaType.valueOf(method.getSseElementType());
        }
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
        handlers.addAll(interceptorDeployment.setupInterceptorHandler());
        //at this point the handler chain only has interceptors
        //which we also want in the abort handler chain
        abortHandlingChain.addAll(handlers);

        handlers.addAll(interceptorDeployment.setupRequestFilterHandler());

        Class<?>[] parameterTypes = new Class[method.getParameters().length];
        for (int i = 0; i < method.getParameters().length; ++i) {
            parameterTypes[i] = loadClass(method.getParameters()[i].declaredType);
        }
        // some parameters need the body to be read
        MethodParameter[] parameters = method.getParameters();
        // body can only be in a parameter
        MethodParameter bodyParameter = null;
        int bodyParameterIndex = -1;
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter param = parameters[i];
            if (param.parameterType == ParameterType.BODY) {
                bodyParameter = param;
                bodyParameterIndex = i;
                break;
            }
        }
        // form params can be everywhere (field, beanparam, param)
        if (method.isFormParamRequired()) {
            // read the body as multipart in one go
            handlers.add(new ReadBodyHandler(bodyParameter != null));
        } else if (bodyParameter != null) {
            // allow the body to be read by chunks
            handlers.add(new InputHandler(quarkusRestConfig.getInputBufferSize(), EXECUTOR_SUPPLIER));
        }
        // if we need the body, let's deserialise it
        if (bodyParameter != null) {
            handlers.add(new RequestDeserializeHandler(loadClass(bodyParameter.type),
                    consumesMediaTypes.isEmpty() ? null : consumesMediaTypes.get(0), serialisers, bodyParameterIndex));
        }

        // given that we may inject form params in the endpoint we need to make sure we read the body before
        // we create/inject our endpoint
        EndpointInvoker invoker = method.getInvoker().get();
        if (!locatableResource) {
            if (clazz.isPerRequestResource()) {
                handlers.add(new PerRequestInstanceHandler(clazz.getFactory()));
                score.add(ScoreSystem.Category.Resource, ScoreSystem.Diagnostic.ResourcePerRequest);
            } else {
                handlers.add(new InstanceHandler(clazz.getFactory()));
                score.add(ScoreSystem.Category.Resource, ScoreSystem.Diagnostic.ResourceSingleton);
            }
        }

        Class<Object> resourceClass = loadClass(clazz.getClassName());
        LazyMethod lazyMethod = new LazyMethod(method.getName(), resourceClass, parameterTypes);

        for (int i = 0; i < parameters.length; i++) {
            ServerMethodParameter param = (ServerMethodParameter) parameters[i];
            boolean single = param.isSingle();
            ParameterExtractor extractor = parameterExtractor(pathParameterIndexes, param.parameterType, param.type, param.name,
                    single, beanContainer, param.encoded);
            ParameterConverter converter = null;
            boolean userProviderConvertersExist = !paramConverterProviders.getParamConverterProviders().isEmpty();
            if (param.converter != null) {
                converter = param.converter.get();
                if (userProviderConvertersExist) {
                    Method javaMethod = lazyMethod.getMethod();
                    // Workaround our lack of support for generic params by not doing this init if there are not runtime
                    // param converter providers
                    converter.init(paramConverterProviders, javaMethod.getParameterTypes()[i],
                            javaMethod.getGenericParameterTypes()[i],
                            javaMethod.getParameterAnnotations()[i]);
                    // make sure we give the user provided resolvers the chance to convert
                    converter = new RuntimeResolvedConverter(converter);
                    converter.init(paramConverterProviders, javaMethod.getParameterTypes()[i],
                            javaMethod.getGenericParameterTypes()[i],
                            javaMethod.getParameterAnnotations()[i]);
                }
            }

            handlers.add(new ParameterHandler(i, param.getDefaultValue(), extractor,
                    converter, param.parameterType,
                    param.isObtainedAsCollection()));
        }
        if (method.isBlocking()) {
            handlers.add(new BlockingHandler(EXECUTOR_SUPPLIER));
            score.add(ScoreSystem.Category.Execution, ScoreSystem.Diagnostic.ExecutionBlocking);
        } else {
            score.add(ScoreSystem.Category.Execution, ScoreSystem.Diagnostic.ExecutionNonBlocking);
        }
        handlers.add(new InvocationHandler(invoker, method.isCDIRequestScopeRequired()));

        Type returnType = TypeSignatureParser.parse(method.getReturnType());
        Class<?> rawReturnType = getRawType(returnType);
        Type nonAsyncReturnType = getNonAsyncReturnType(returnType);
        Class<?> rawNonAsyncReturnType = getRawType(nonAsyncReturnType);

        if (CompletionStage.class.isAssignableFrom(rawReturnType)) {
            handlers.add(new CompletionStageResponseHandler());
        } else if (Uni.class.isAssignableFrom(rawReturnType)) {
            handlers.add(new UniResponseHandler());
        } else if (Multi.class.isAssignableFrom(rawReturnType)) {
            handlers.add(new MultiResponseHandler());
        }
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
                        score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                    } else if (rawNonAsyncReturnType != Void.class
                            && rawNonAsyncReturnType != void.class) {
                        List<MessageBodyWriter<?>> buildTimeWriters = serialisers.findBuildTimeWriters(rawNonAsyncReturnType,
                                RuntimeType.SERVER, method.getProduces());
                        if (buildTimeWriters == null) {
                            //if this is null this means that the type cannot be resolved at build time
                            //this happens when the method returns a generic type (e.g. Object), so there
                            //are more specific mappers that could be invoked depending on the actual return value
                            handlers.add(new FixedProducesHandler(mediaType, dynamicEntityWriter));
                            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                        } else if (buildTimeWriters.isEmpty()) {
                            //we could not find any writers that can write a response to this endpoint
                            log.warn("Cannot find any combination of response writers for the method " + clazz.getClassName()
                                    + "#" + method.getName() + "(" + Arrays.toString(method.getParameters()) + ")");
                            handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                        } else if (buildTimeWriters.size() == 1) {
                            //only a single handler that can handle the response
                            //this is a very common case
                            MessageBodyWriter<?> writer = buildTimeWriters.get(0);
                            handlers.add(new FixedProducesHandler(mediaType, new FixedEntityWriter(
                                    writer, serialisers)));
                            if (writer instanceof QuarkusRestMessageBodyWriter)
                                score.add(ScoreSystem.Category.Writer,
                                        ScoreSystem.Diagnostic.WriterBuildTimeDirect(writer));
                            else
                                score.add(ScoreSystem.Category.Writer,
                                        ScoreSystem.Diagnostic.WriterBuildTime(writer));
                        } else {
                            //multiple writers, we try them in the proper order which had already been created
                            handlers.add(new FixedProducesHandler(mediaType,
                                    new FixedEntityWriterArray(buildTimeWriters.toArray(new MessageBodyWriter[0]),
                                            serialisers)));
                            score.add(ScoreSystem.Category.Writer,
                                    ScoreSystem.Diagnostic.WriterBuildTimeMultiple(buildTimeWriters));
                        }
                    } else {
                        score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterNotRequired);
                    }
                } else {
                    //there are multiple possibilities
                    //we could optimise this more in future
                    handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                    score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                }
            } else {
                score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
            }
        } else {
            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
        }

        //the response filter handlers, they need to be added to both the abort and
        //normal chains. At the moment this only has one handler added to it but
        //in future there will be one per filter
        List<ServerRestHandler> responseFilterHandlers = new ArrayList<>();
        if (method.isSse()) {
            handlers.add(new SseResponseWriterHandler());
        } else {
            handlers.add(new ResponseHandler());

            responseFilterHandlers.addAll(interceptorDeployment.setupResponseFilterHandler());
            handlers.addAll(responseFilterHandlers);
            handlers.add(new ResponseWriterHandler(dynamicEntityWriter));
        }
        abortHandlingChain.add(new ExceptionHandler());
        abortHandlingChain.add(new ResponseHandler());
        abortHandlingChain.addAll(responseFilterHandlers);

        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));
        handlers.add(0, new AbortChainHandler(abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY)));

        RuntimeResource runtimeResource = new RuntimeResource(method.getHttpMethod(), methodPathTemplate,
                classPathTemplate,
                method.getProduces() == null ? null : serverMediaType,
                consumesMediaTypes, invoker,
                clazz.getFactory(), handlers.toArray(EMPTY_REST_HANDLER_ARRAY), method.getName(), parameterTypes,
                nonAsyncReturnType, method.isBlocking(), resourceClass,
                lazyMethod,
                pathParameterIndexes, score, sseElementType, clazz.resourceExceptionMapper());
        return runtimeResource;
    }

    public ParameterExtractor parameterExtractor(Map<String, Integer> pathParameterIndexes, ParameterType type, String javaType,
            String name,
            boolean single, BeanContainer beanContainer, boolean encoded) {
        ParameterExtractor extractor;
        switch (type) {
            case HEADER:
                return new HeaderParamExtractor(name, single);
            case COOKIE:
                return new CookieParamExtractor(name);
            case FORM:
                return new FormParamExtractor(name, single, encoded);
            case PATH:
                Integer index = pathParameterIndexes.get(name);
                if (index == null) {
                    extractor = new NullParamExtractor();
                } else {
                    extractor = new PathParamExtractor(index, encoded);
                }
                return extractor;
            case CONTEXT:
                return new ContextParamExtractor(javaType);
            case ASYNC_RESPONSE:
                return new AsyncResponseExtractor();
            case QUERY:
                extractor = new QueryParamExtractor(name, single, encoded);
                return extractor;
            case BODY:
                return new BodyParamExtractor();
            case MATRIX:
                extractor = new MatrixParamExtractor(name, single, encoded);
                return extractor;
            case BEAN:
                return new BeanParamExtractor(factory(javaType, beanContainer));
            default:
                return new QueryParamExtractor(name, single, encoded);
        }
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
            // NOTE: same code in EndpointIndexer.getNonAsyncReturnType
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

    public Function<Class<?>, BeanFactory<?>> factoryCreator(BeanContainer container) {
        return new Function<Class<?>, BeanFactory<?>>() {
            @Override
            public BeanFactory<?> apply(Class<?> aClass) {
                return new ArcBeanFactory<>(aClass, container);
            }
        };
    }
}
