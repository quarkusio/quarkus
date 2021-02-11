package io.quarkus.resteasy.reactive.server.deployment;

import static java.util.stream.Collectors.toList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.core.SingletonBeanFactory;
import org.jboss.resteasy.reactive.common.model.InjectableBean;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceDynamicFeature;
import org.jboss.resteasy.reactive.common.model.ResourceFeature;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaderWriter;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.DefaultProducesHandler;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.util.Encode;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.model.DynamicFeatures;
import org.jboss.resteasy.reactive.server.model.Features;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.ClientProxyUnwrapper;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.FactoryUtils;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusFactoryCreator;
import io.quarkus.resteasy.reactive.common.deployment.ResourceInterceptorsBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.SerializersUtil;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveInitialiser;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRuntimeRecorder;
import io.quarkus.resteasy.reactive.server.runtime.ServerVertxBufferMessageBodyWriter;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.AuthenticationCompletionExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.AuthenticationFailedExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.AuthenticationRedirectExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.ForbiddenExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.security.SecurityContextOverrideHandler;
import io.quarkus.resteasy.reactive.spi.CustomExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.JaxrsFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.BasicRoute;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class ResteasyReactiveProcessor {

    private static final String QUARKUS_INIT_CLASS = "io.quarkus.rest.runtime.__QuarkusInit";

    @BuildStep
    public FeatureBuildItem buildSetup() {
        return new FeatureBuildItem(Feature.RESTEASY_REACTIVE);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.RESTEASY_REACTIVE);
    }

    @BuildStep
    void vertxIntegration(BuildProducer<MessageBodyWriterBuildItem> writerBuildItemBuildProducer) {
        writerBuildItemBuildProducer.produce(new MessageBodyWriterBuildItem(ServerVertxBufferMessageBodyWriter.class.getName(),
                Buffer.class.getName(), Collections.singletonList(MediaType.WILDCARD), RuntimeType.SERVER, true));
    }

    @BuildStep
    void generateCustomProducer(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }

        Map<DotName, MethodInfo> resourcesThatNeedCustomProducer = resourceScanningResultBuildItem.get().getResult()
                .getResourcesThatNeedCustomProducer();
        Set<String> beanParams = resourceScanningResultBuildItem.get().getResult()
                .getBeanParams();
        if (!resourcesThatNeedCustomProducer.isEmpty() || !beanParams.isEmpty()) {
            CustomResourceProducersGenerator.generate(resourcesThatNeedCustomProducer, beanParams,
                    generatedBeanBuildItemBuildProducer,
                    additionalBeanBuildItemBuildProducer);
        }
    }

    @BuildStep
    void handleClassLevelExceptionMappers(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ClassLevelExceptionMappersBuildItem> classLevelExceptionMappers) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }
        List<MethodInfo> methodExceptionMapper = resourceScanningResultBuildItem.get().getResult()
                .getClassLevelExceptionMappers();
        if (methodExceptionMapper.isEmpty()) {
            return;
        }
        GeneratedClassGizmoAdaptor classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        final Map<DotName, Map<String, String>> resultingMappers = new HashMap<>(methodExceptionMapper.size());
        for (MethodInfo methodInfo : methodExceptionMapper) {
            Map<String, String> generationResult = ServerExceptionMapperGenerator.generatePerClassMapper(methodInfo,
                    classOutput);
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, false, generationResult.values().toArray(new String[0])));
            Map<String, String> classMappers;
            DotName classDotName = methodInfo.declaringClass().name();
            if (resultingMappers.containsKey(classDotName)) {
                classMappers = resultingMappers.get(classDotName);
            } else {
                classMappers = new HashMap<>();
                resultingMappers.put(classDotName, classMappers);
            }
            classMappers.putAll(generationResult);
        }
        classLevelExceptionMappers.produce(new ClassLevelExceptionMappersBuildItem(resultingMappers));
    }

    @BuildStep
    void registerCustomExceptionMappers(BuildProducer<CustomExceptionMapperBuildItem> customExceptionMapper) {
        customExceptionMapper.produce(new CustomExceptionMapperBuildItem(AuthenticationFailedExceptionMapper.class.getName()));
        customExceptionMapper.produce(new CustomExceptionMapperBuildItem(UnauthorizedExceptionMapper.class.getName()));
    }

    @BuildStep
    public void unremoveableBeans(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<UnremovableBeanBuildItem> unremoveableBeans) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }
        Set<String> beanParams = resourceScanningResultBuildItem.get().getResult()
                .getBeanParams();
        unremoveableBeans.produce(UnremovableBeanBuildItem.beanClassNames(beanParams.toArray(new String[0])));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void setupEndpoints(Capabilities capabilities, BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            ResteasyReactiveConfig config,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            ResteasyReactiveRecorder recorder,
            RecorderContext recorderContext,
            ShutdownContextBuildItem shutdownContext,
            HttpBuildTimeConfig vertxConfig,
            List<DynamicFeatureBuildItem> dynamicFeatures,
            List<MessageBodyReaderBuildItem> additionalMessageBodyReaders,
            List<MessageBodyWriterBuildItem> additionalMessageBodyWriters,
            List<JaxrsFeatureBuildItem> features,
            List<ServerDefaultProducesHandlerBuildItem> serverDefaultProducesHandlers,
            Optional<RequestContextFactoryBuildItem> requestContextFactoryBuildItem,
            Optional<ClassLevelExceptionMappersBuildItem> classLevelExceptionMappers,
            BuildProducer<ResteasyReactiveDeploymentBuildItem> quarkusRestDeploymentBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<RouteBuildItem> routes,
            ApplicationResultBuildItem applicationResultBuildItem,
            ResourceInterceptorsBuildItem resourceInterceptorsBuildItem,
            ExceptionMappersBuildItem exceptionMappersBuildItem,
            ParamConverterProvidersBuildItem paramConverterProvidersBuildItem,
            ContextResolversBuildItem contextResolversBuildItem,
            List<MethodScannerBuildItem> methodScanners, ResteasyReactiveServerConfig serverConfig)
            throws NoSuchMethodException {

        if (!resourceScanningResultBuildItem.isPresent()) {
            // no detected @Path, bail out
            return;
        }

        if (capabilities.isPresent(Capability.RESTEASY)) {
            throw new IllegalStateException(
                    "The 'quarkus-resteasy-reactive' and 'quarkus-resteasy' extensions cannot be used at the same time.");
        }

        recorderContext.registerNonDefaultConstructor(
                MediaType.class.getDeclaredConstructor(String.class, String.class, String.class),
                mediaType -> Stream.of(mediaType.getType(), mediaType.getSubtype(), mediaType.getParameters())
                        .collect(toList()));

        IndexView index = beanArchiveIndexBuildItem.getIndex();

        ResourceScanningResult result = resourceScanningResultBuildItem.get().getResult();
        Map<DotName, ClassInfo> scannedResources = result.getScannedResources();
        Map<DotName, String> scannedResourcePaths = result.getScannedResourcePaths();
        Map<DotName, ClassInfo> possibleSubResources = result.getPossibleSubResources();
        Map<DotName, String> pathInterfaces = result.getPathInterfaces();

        ApplicationScanningResult appResult = applicationResultBuildItem.getResult();
        Set<String> allowedClasses = appResult.getAllowedClasses();
        Set<String> singletonClasses = appResult.getSingletonClasses();
        boolean filterClasses = appResult.isFilterClasses();
        Application application = appResult.getApplication();

        Map<String, String> existingConverters = new HashMap<>();
        List<ResourceClass> resourceClasses = new ArrayList<>();
        List<ResourceClass> subResourceClasses = new ArrayList<>();
        AdditionalReaders additionalReaders = new AdditionalReaders();
        AdditionalWriters additionalWriters = new AdditionalWriters();
        Map<String, InjectableBean> injectableBeans = new HashMap<>();
        QuarkusServerEndpointIndexer serverEndpointIndexer;

        ResourceInterceptors interceptors = resourceInterceptorsBuildItem.getResourceInterceptors();
        ExceptionMapping exceptionMapping = exceptionMappersBuildItem.getExceptionMapping();
        ContextResolvers contextResolvers = contextResolversBuildItem.getContextResolvers();
        ParamConverterProviders paramConverterProviders = paramConverterProvidersBuildItem.getParamConverterProviders();
        Function<String, BeanFactory<?>> factoryFunction = s -> FactoryUtils.factory(s, singletonClasses, recorder,
                beanContainerBuildItem);
        interceptors.initializeDefaultFactories(factoryFunction);
        exceptionMapping.initializeDefaultFactories(factoryFunction);
        contextResolvers.initializeDefaultFactories(factoryFunction);
        paramConverterProviders.initializeDefaultFactories(factoryFunction);
        paramConverterProviders.sort();
        interceptors.sort();

        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                QUARKUS_INIT_CLASS, null, Object.class.getName(), ResteasyReactiveInitialiser.class.getName());
                MethodCreator initConverters = c.getMethodCreator("init", void.class, Deployment.class)) {

            QuarkusServerEndpointIndexer.Builder serverEndpointIndexerBuilder = new QuarkusServerEndpointIndexer.Builder()
                    .addMethodScanners(
                            methodScanners.stream().map(MethodScannerBuildItem::getMethodScanner).collect(Collectors.toList()))
                    .setIndex(index)
                    .setFactoryCreator(new QuarkusFactoryCreator(recorder, beanContainerBuildItem.getValue()))
                    .setEndpointInvokerFactory(new QuarkusInvokerFactory(generatedClassBuildItemBuildProducer, recorder))
                    .setGeneratedClassBuildItemBuildProducer(generatedClassBuildItemBuildProducer)
                    .setBytecodeTransformerBuildProducer(bytecodeTransformerBuildItemBuildProducer)
                    .setReflectiveClassProducer(reflectiveClassBuildItemBuildProducer)
                    .setExistingConverters(existingConverters).setScannedResourcePaths(scannedResourcePaths)
                    .setConfig(new org.jboss.resteasy.reactive.common.ResteasyReactiveConfig(
                            config.inputBufferSize.asLongValue(), config.singleDefaultProduces, config.defaultProduces))
                    .setAdditionalReaders(additionalReaders)
                    .setHttpAnnotationToMethod(result.getHttpAnnotationToMethod())
                    .setInjectableBeans(injectableBeans)
                    .setAdditionalWriters(additionalWriters)
                    .setDefaultBlocking(appResult.isBlocking())
                    .setHasRuntimeConverters(!paramConverterProviders.getParamConverterProviders().isEmpty())
                    .setClassLevelExceptionMappers(
                            classLevelExceptionMappers.isPresent() ? classLevelExceptionMappers.get().getMappers()
                                    : Collections.emptyMap())
                    .setResourceMethodCallback(new Consumer<Map.Entry<MethodInfo, ResourceMethod>>() {
                        @Override
                        public void accept(Map.Entry<MethodInfo, ResourceMethod> entry) {
                            MethodInfo method = entry.getKey();
                            String source = ResteasyReactiveProcessor.class.getSimpleName() + " > " + method.declaringClass()
                                    + "[" + method + "]";

                            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                                    .type(method.returnType())
                                    .index(index)
                                    .ignoreTypePredicate(ResteasyReactiveServerDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                                    .ignoreFieldPredicate(ResteasyReactiveServerDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                                    .ignoreMethodPredicate(
                                            ResteasyReactiveServerDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                                    .source(source)
                                    .build());

                            for (short i = 0; i < method.parameters().size(); i++) {
                                Type parameterType = method.parameters().get(i);
                                if (!hasAnnotation(method, i, ResteasyReactiveServerDotNames.CONTEXT)) {
                                    reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                                            .type(parameterType)
                                            .index(index)
                                            .ignoreTypePredicate(
                                                    ResteasyReactiveServerDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                                            .ignoreFieldPredicate(
                                                    ResteasyReactiveServerDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                                            .ignoreMethodPredicate(
                                                    ResteasyReactiveServerDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                                            .source(source)
                                            .build());
                                }
                            }
                        }

                        private boolean hasAnnotation(MethodInfo method, short paramPosition, DotName annotation) {
                            for (AnnotationInstance annotationInstance : method.annotations()) {
                                AnnotationTarget target = annotationInstance.target();
                                if (target != null && target.kind() == AnnotationTarget.Kind.METHOD_PARAMETER
                                        && target.asMethodParameter().position() == paramPosition
                                        && annotationInstance.name().equals(annotation)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    })
                    .setInitConverters(initConverters);

            if (!serverDefaultProducesHandlers.isEmpty()) {
                List<DefaultProducesHandler> handlers = new ArrayList<>(serverDefaultProducesHandlers.size());
                for (ServerDefaultProducesHandlerBuildItem bi : serverDefaultProducesHandlers) {
                    handlers.add(bi.getDefaultProducesHandler());
                }
                serverEndpointIndexerBuilder
                        .setDefaultProducesHandler(new DefaultProducesHandler.DelegatingDefaultProducesHandler(handlers));
            }
            serverEndpointIndexer = serverEndpointIndexerBuilder.build();

            for (ClassInfo i : scannedResources.values()) {
                if (filterClasses && !allowedClasses.contains(i.name().toString())) {
                    continue;
                }
                ResourceClass endpoints = serverEndpointIndexer.createEndpoints(i);
                if (singletonClasses.contains(i.name().toString())) {
                    endpoints.setFactory(new SingletonBeanFactory<>(i.name().toString()));
                }
                if (endpoints != null) {
                    resourceClasses.add(endpoints);
                }
            }
            //now index possible sub resources. These are all classes that have method annotations
            //that are not annotated @Path
            Deque<ClassInfo> toScan = new ArrayDeque<>();
            for (DotName methodAnnotation : result.getHttpAnnotationToMethod().keySet()) {
                for (AnnotationInstance instance : index.getAnnotations(methodAnnotation)) {
                    MethodInfo method = instance.target().asMethod();
                    ClassInfo classInfo = method.declaringClass();
                    toScan.add(classInfo);
                }
            }
            while (!toScan.isEmpty()) {
                ClassInfo classInfo = toScan.poll();
                if (scannedResources.containsKey(classInfo.name()) ||
                        pathInterfaces.containsKey(classInfo.name()) ||
                        possibleSubResources.containsKey(classInfo.name())) {
                    continue;
                }
                possibleSubResources.put(classInfo.name(), classInfo);
                ResourceClass endpoints = serverEndpointIndexer.createEndpoints(classInfo);
                if (endpoints != null) {
                    subResourceClasses.add(endpoints);
                }
                //we need to also look for all sub classes and interfaces
                //they may have type variables that need to be handled
                toScan.addAll(index.getKnownDirectImplementors(classInfo.name()));
                toScan.addAll(index.getKnownDirectSubclasses(classInfo.name()));
            }

            Features feats = new Features();
            for (JaxrsFeatureBuildItem feature : features) {
                ResourceFeature resourceFeature = new ResourceFeature();
                resourceFeature
                        .setFactory(
                                FactoryUtils.factory(feature.getClassName(), singletonClasses, recorder,
                                        beanContainerBuildItem));
                feats.addFeature(resourceFeature);
            }

            DynamicFeatures dynamicFeats = new DynamicFeatures();
            for (DynamicFeatureBuildItem additionalDynamicFeature : dynamicFeatures) {
                ResourceDynamicFeature resourceFeature = new ResourceDynamicFeature();
                resourceFeature.setFactory(
                        recorder.factory(additionalDynamicFeature.getClassName(), beanContainerBuildItem.getValue()));
                dynamicFeats.addFeature(resourceFeature);
            }

            ServerSerialisers serialisers = new ServerSerialisers();
            SerializersUtil.setupSerializers(recorder, reflectiveClass, additionalMessageBodyReaders,
                    additionalMessageBodyWriters, beanContainerBuildItem, applicationResultBuildItem, serialisers,
                    RuntimeType.SERVER);
            // built-ins

            for (Serialisers.BuiltinWriter builtinWriter : ServerSerialisers.BUILTIN_WRITERS) {
                registerWriter(recorder, serialisers, builtinWriter.entityClass, builtinWriter.writerClass,
                        beanContainerBuildItem.getValue(),
                        builtinWriter.mediaType);
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, builtinWriter.writerClass.getName()));
            }
            for (Serialisers.BuiltinReader builtinReader : ServerSerialisers.BUILTIN_READERS) {
                registerReader(recorder, serialisers, builtinReader.entityClass, builtinReader.readerClass,
                        beanContainerBuildItem.getValue(),
                        builtinReader.mediaType, builtinReader.constraint);
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, builtinReader.readerClass.getName()));
            }

            for (AdditionalReaderWriter.Entry additionalReader : additionalReaders.get()) {
                Class readerClass = additionalReader.getHandlerClass();
                registerReader(recorder, serialisers, additionalReader.getEntityClass(), readerClass,
                        beanContainerBuildItem.getValue(), additionalReader.getMediaType(), additionalReader.getConstraint());
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, readerClass.getName()));
            }

            for (AdditionalReaderWriter.Entry entry : additionalWriters.get()) {
                Class writerClass = entry.getHandlerClass();
                registerWriter(recorder, serialisers, entry.getEntityClass(), writerClass,
                        beanContainerBuildItem.getValue(), entry.getMediaType());
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, writerClass.getName()));
            }

            initConverters.returnValue(null);
            BeanFactory<ResteasyReactiveInitialiser> initClassFactory = recorder.factory(QUARKUS_INIT_CLASS,
                    beanContainerBuildItem.getValue());

            String applicationPath = determineApplicationPath(index, serverConfig.path);
            // spec allows the path contain encoded characters
            if ((applicationPath != null) && applicationPath.contains("%")) {
                applicationPath = Encode.decodePath(applicationPath);
            }

            String deploymentPath = sanitizeApplicationPath(applicationPath);
            // Handler used for both the default and non-default deployment path (specified as application path or resteasyConfig.path)
            // Routes use the order VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1 to ensure the default route is called before the resteasy one
            Class<? extends Application> applicationClass = application == null ? Application.class : application.getClass();
            RuntimeValue<Deployment> deployment = recorder.createDeployment(new DeploymentInfo()
                    .setInterceptors(interceptors.sort())
                    .setConfig(new org.jboss.resteasy.reactive.common.ResteasyReactiveConfig(
                            config.inputBufferSize.asLongValue(), config.singleDefaultProduces, config.defaultProduces))
                    .setExceptionMapping(exceptionMapping)
                    .setCtxResolvers(contextResolvers)
                    .setFeatures(feats)
                    .setClientProxyUnwrapper(new ClientProxyUnwrapper())
                    .setApplicationSupplier(recorder.handleApplication(applicationClass, singletonClasses.isEmpty()))
                    .setFactoryCreator(recorder.factoryCreator(beanContainerBuildItem.getValue()))
                    .setDynamicFeatures(dynamicFeats)
                    .setSerialisers(serialisers)
                    .setApplicationPath(applicationPath)
                    .setResourceClasses(resourceClasses)
                    .setLocatableResourceClasses(subResourceClasses)
                    .setParamConverterProviders(paramConverterProviders),
                    beanContainerBuildItem.getValue(), shutdownContext, vertxConfig,
                    requestContextFactoryBuildItem.map(RequestContextFactoryBuildItem::getFactory).orElse(null),
                    initClassFactory);

            quarkusRestDeploymentBuildItemBuildProducer
                    .produce(new ResteasyReactiveDeploymentBuildItem(deployment, deploymentPath));
            if (!requestContextFactoryBuildItem.isPresent()) {
                Handler<RoutingContext> handler = recorder.handler(deployment);

                // Exact match for resources matched to the root path
                routes.produce(new RouteBuildItem(
                        new BasicRoute(deploymentPath, VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1), handler));
                String matchPath = deploymentPath;
                if (matchPath.endsWith("/")) {
                    matchPath += "*";
                } else {
                    matchPath += "/*";
                }
                // Match paths that begin with the deployment path
                routes.produce(
                        new RouteBuildItem(new BasicRoute(matchPath, VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1), handler));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void applyRuntimeConfig(ResteasyReactiveRuntimeRecorder recorder,
            Optional<ResteasyReactiveDeploymentBuildItem> deployment,
            HttpConfiguration httpConfiguration) {
        if (!deployment.isPresent()) {
            return;
        }
        recorder.configure(deployment.get().getDeployment(), httpConfiguration);
    }

    @BuildStep
    public void securityExceptionMappers(BuildProducer<ExceptionMapperBuildItem> exceptionMapperBuildItemBuildProducer) {
        // built-ins
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                AuthenticationCompletionExceptionMapper.class.getName(),
                AuthenticationCompletionException.class.getName(),
                Priorities.USER, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                AuthenticationFailedExceptionMapper.class.getName(),
                AuthenticationFailedException.class.getName(),
                Priorities.USER + 1, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                AuthenticationRedirectExceptionMapper.class.getName(),
                AuthenticationRedirectException.class.getName(),
                Priorities.USER, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                ForbiddenExceptionMapper.class.getName(),
                ForbiddenException.class.getName(),
                Priorities.USER + 1, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                UnauthorizedExceptionMapper.class.getName(),
                UnauthorizedException.class.getName(),
                Priorities.USER + 1, false));
    }

    @BuildStep
    MethodScannerBuildItem integrateSecurityOverrideSupport() {
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, Map<String, Object> methodContext) {
                return Collections.singletonList(new SecurityContextOverrideHandler.Customizer());
            }
        });
    }

    private String determineApplicationPath(IndexView index, String defaultPath) {
        Collection<AnnotationInstance> applicationPaths = index.getAnnotations(ResteasyReactiveDotNames.APPLICATION_PATH);
        if (applicationPaths.isEmpty()) {
            return defaultPath;
        }
        // currently we only examine the first class that is annotated with @ApplicationPath so best
        // fail if the user code has multiple such annotations instead of surprising the user
        // at runtime
        if (applicationPaths.size() > 1) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (AnnotationInstance annotationInstance : applicationPaths) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(annotationInstance.target().asClass().name().toString());
            }
            throw new RuntimeException("Multiple classes ( " + sb.toString()
                    + ") have been annotated with @ApplicationPath which is currently not supported");
        }
        String applicationPath = null;
        AnnotationValue applicationPathValue = applicationPaths.iterator().next().value();
        if ((applicationPathValue != null)) {
            applicationPath = applicationPathValue.asString();
        }
        return applicationPath;
    }

    private String sanitizeApplicationPath(String applicationPath) {
        if ((applicationPath == null) || applicationPath.isEmpty() || "/".equals(applicationPath)) {
            return "/";
        }
        applicationPath = applicationPath.trim();
        if (applicationPath.equals("/"))
            applicationPath = "";
        // add leading slash
        if (!applicationPath.startsWith("/"))
            applicationPath = "/" + applicationPath;
        // remove trailing slash
        if (applicationPath.endsWith("/"))
            applicationPath = applicationPath.substring(0, applicationPath.length() - 1);
        return applicationPath;
    }

    private void registerWriter(ResteasyReactiveRecorder recorder, ServerSerialisers serialisers, Class<?> entityClass,
            Class<? extends MessageBodyWriter<?>> writerClass, BeanContainer beanContainer,
            String mediaType) {
        ResourceWriter writer = new ResourceWriter();
        writer.setFactory(recorder.factory(writerClass.getName(), beanContainer));
        writer.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerWriter(serialisers, entityClass.getName(), writer);
    }

    private void registerReader(ResteasyReactiveRecorder recorder, ServerSerialisers serialisers, Class<?> entityClass,
            Class<? extends MessageBodyReader<?>> readerClass, BeanContainer beanContainer, String mediaType,
            RuntimeType constraint) {
        ResourceReader reader = new ResourceReader();
        reader.setFactory(recorder.factory(readerClass.getName(), beanContainer));
        reader.setMediaTypeStrings(Collections.singletonList(mediaType));
        reader.setConstraint(constraint);
        recorder.registerReader(serialisers, entityClass.getName(), reader);
    }

}
