package io.quarkus.rest.deployment.processor;

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
import java.util.stream.Stream;

import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
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
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.rest.common.deployment.ApplicationResultBuildItem;
import io.quarkus.rest.common.deployment.FactoryUtils;
import io.quarkus.rest.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.rest.common.deployment.SerializersUtil;
import io.quarkus.rest.common.deployment.framework.AdditionalReaders;
import io.quarkus.rest.common.deployment.framework.AdditionalWriters;
import io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames;
import io.quarkus.rest.common.runtime.QuarkusRestConfig;
import io.quarkus.rest.common.runtime.core.Serialisers;
import io.quarkus.rest.common.runtime.core.SingletonBeanFactory;
import io.quarkus.rest.common.runtime.model.InjectableBean;
import io.quarkus.rest.common.runtime.model.InterceptorContainer;
import io.quarkus.rest.common.runtime.model.PreMatchInterceptorContainer;
import io.quarkus.rest.common.runtime.model.ResourceClass;
import io.quarkus.rest.common.runtime.model.ResourceContextResolver;
import io.quarkus.rest.common.runtime.model.ResourceDynamicFeature;
import io.quarkus.rest.common.runtime.model.ResourceExceptionMapper;
import io.quarkus.rest.common.runtime.model.ResourceFeature;
import io.quarkus.rest.common.runtime.model.ResourceInterceptor;
import io.quarkus.rest.common.runtime.model.ResourceInterceptors;
import io.quarkus.rest.common.runtime.model.ResourceParamConverterProvider;
import io.quarkus.rest.common.runtime.model.ResourceReader;
import io.quarkus.rest.common.runtime.model.ResourceWriter;
import io.quarkus.rest.common.runtime.util.Encode;
import io.quarkus.rest.deployment.framework.ServerEndpointIndexer;
import io.quarkus.rest.server.runtime.QuarkusRestInitialiser;
import io.quarkus.rest.server.runtime.QuarkusRestRecorder;
import io.quarkus.rest.server.runtime.core.ContextResolvers;
import io.quarkus.rest.server.runtime.core.DynamicFeatures;
import io.quarkus.rest.server.runtime.core.ExceptionMapping;
import io.quarkus.rest.server.runtime.core.Features;
import io.quarkus.rest.server.runtime.core.ParamConverterProviders;
import io.quarkus.rest.server.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.server.runtime.core.ServerSerialisers;
import io.quarkus.rest.spi.AbstractInterceptorBuildItem;
import io.quarkus.rest.spi.BeanFactory;
import io.quarkus.rest.spi.ContainerRequestFilterBuildItem;
import io.quarkus.rest.spi.ContainerResponseFilterBuildItem;
import io.quarkus.rest.spi.ContextResolverBuildItem;
import io.quarkus.rest.spi.DynamicFeatureBuildItem;
import io.quarkus.rest.spi.ExceptionMapperBuildItem;
import io.quarkus.rest.spi.JaxrsFeatureBuildItem;
import io.quarkus.rest.spi.MessageBodyReaderBuildItem;
import io.quarkus.rest.spi.MessageBodyWriterBuildItem;
import io.quarkus.rest.spi.ParamConverterBuildItem;
import io.quarkus.rest.spi.ReaderInterceptorBuildItem;
import io.quarkus.rest.spi.WriterInterceptorBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.BasicRoute;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QuarkusRestProcessor {

    private static final String QUARKUS_INIT_CLASS = "io.quarkus.rest.runtime.__QuarkusInit";

    @BuildStep
    public FeatureBuildItem buildSetup() {
        return new FeatureBuildItem(Feature.QUARKUS_REST);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.QUARKUS_REST);
    }

    @BuildStep
    void generateCustomProducer(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }

        Map<DotName, MethodInfo> resourcesThatNeedCustomProducer = resourceScanningResultBuildItem.get()
                .getResourcesThatNeedCustomProducer();
        Set<String> beanParams = resourceScanningResultBuildItem.get()
                .getBeanParams();
        if (!resourcesThatNeedCustomProducer.isEmpty() || !beanParams.isEmpty()) {
            CustomResourceProducersGenerator.generate(resourcesThatNeedCustomProducer, beanParams,
                    generatedBeanBuildItemBuildProducer,
                    additionalBeanBuildItemBuildProducer);
        }
    }

    @BuildStep
    void handleCustomExceptionMapper(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ClassLevelExceptionMappersBuildItem> classLevelExceptionMappers) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }
        List<MethodInfo> methodExceptionMapper = resourceScanningResultBuildItem.get().getClassLevelExceptionMappers();
        if (methodExceptionMapper.isEmpty()) {
            return;
        }
        GeneratedClassGizmoAdaptor classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        final Map<DotName, Map<String, String>> resultingMappers = new HashMap<>(methodExceptionMapper.size());
        for (MethodInfo methodInfo : methodExceptionMapper) {
            Map<String, String> generationResult = ClassLevelExceptionMapperGenerator.generate(methodInfo, classOutput);
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
    @Record(ExecutionTime.STATIC_INIT)
    public void setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            QuarkusRestConfig config,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer,
            QuarkusRestRecorder recorder,
            RecorderContext recorderContext,
            ShutdownContextBuildItem shutdownContext,
            HttpBuildTimeConfig vertxConfig,
            List<ContainerRequestFilterBuildItem> containerRequestFilters,
            List<ContainerResponseFilterBuildItem> containerResponseFilters,
            List<WriterInterceptorBuildItem> writerInterceptors,
            List<ReaderInterceptorBuildItem> readerInterceptors,
            List<ExceptionMapperBuildItem> exceptionMappers,
            List<DynamicFeatureBuildItem> dynamicFeatures,
            List<MessageBodyReaderBuildItem> additionalMessageBodyReaders,
            List<MessageBodyWriterBuildItem> additionalMessageBodyWriters,
            List<JaxrsFeatureBuildItem> features,
            List<ParamConverterBuildItem> paramConverterBuildItems,
            List<ContextResolverBuildItem> contextResolvers,
            Optional<ClassLevelExceptionMappersBuildItem> classLevelExceptionMappers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RouteBuildItem> routes,
            ApplicationResultBuildItem applicationResultBuildItem) throws NoSuchMethodException {

        if (!resourceScanningResultBuildItem.isPresent()) {
            // no detected @Path, bail out
            return;
        }

        recorderContext.registerNonDefaultConstructor(
                MediaType.class.getDeclaredConstructor(String.class, String.class, String.class),
                mediaType -> Stream.of(mediaType.getType(), mediaType.getSubtype(), mediaType.getParameters())
                        .collect(toList()));

        IndexView index = beanArchiveIndexBuildItem.getIndex();

        Map<DotName, ClassInfo> scannedResources = resourceScanningResultBuildItem.get().getScannedResources();
        Map<DotName, String> scannedResourcePaths = resourceScanningResultBuildItem.get().getScannedResourcePaths();
        Map<DotName, ClassInfo> possibleSubResources = resourceScanningResultBuildItem.get().getPossibleSubResources();
        Map<DotName, String> pathInterfaces = resourceScanningResultBuildItem.get().getPathInterfaces();

        Set<String> allowedClasses = applicationResultBuildItem.getAllowedClasses();
        Set<String> singletonClasses = applicationResultBuildItem.getSingletonClasses();
        Set<String> globalNameBindings = applicationResultBuildItem.getGlobalNameBindings();
        boolean filterClasses = applicationResultBuildItem.isFilterClasses();
        Application application = applicationResultBuildItem.getApplication();
        ClassInfo selectedAppClass = applicationResultBuildItem.getSelectedAppClass();

        ParamConverterProviders converterProviders = new ParamConverterProviders();
        for (ParamConverterBuildItem paramConverter : paramConverterBuildItems) {
            ResourceParamConverterProvider converter = new ResourceParamConverterProvider();
            converter.setFactory(
                    FactoryUtils.factory(paramConverter.getClassName(), singletonClasses, recorder, beanContainerBuildItem));
            converter.setPriority(paramConverter.getPriority());
            converterProviders.addParamConverterProviders(converter);
        }
        converterProviders.sort();

        Map<String, String> existingConverters = new HashMap<>();
        List<ResourceClass> resourceClasses = new ArrayList<>();
        List<ResourceClass> subResourceClasses = new ArrayList<>();
        AdditionalReaders additionalReaders = new AdditionalReaders();
        AdditionalWriters additionalWriters = new AdditionalWriters();
        Map<String, InjectableBean> injectableBeans = new HashMap<>();
        ServerEndpointIndexer serverEndpointIndexer;
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                QUARKUS_INIT_CLASS, null, Object.class.getName(), QuarkusRestInitialiser.class.getName());
                MethodCreator initConverters = c.getMethodCreator("init", void.class, QuarkusRestDeployment.class)) {

            serverEndpointIndexer = new ServerEndpointIndexer.Builder()
                    .setIndex(index)
                    .setBeanContainer(beanContainerBuildItem.getValue())
                    .setGeneratedClassBuildItemBuildProducer(generatedClassBuildItemBuildProducer)
                    .setBytecodeTransformerBuildItemBuildProducer(bytecodeTransformerBuildItemBuildProducer)
                    .setRecorder(recorder)
                    .setExistingConverters(existingConverters).setScannedResourcePaths(scannedResourcePaths).setConfig(config)
                    .setAdditionalReaders(additionalReaders)
                    .setHttpAnnotationToMethod(resourceScanningResultBuildItem.get().getHttpAnnotationToMethod())
                    .setInjectableBeans(injectableBeans).setAdditionalWriters(additionalWriters)
                    .setDefaultBlocking(applicationResultBuildItem.isBlocking())
                    .setHasRuntimeConverters(!converterProviders.getParamConverterProviders().isEmpty())
                    .setClassLevelExceptionMappers(
                            classLevelExceptionMappers.isPresent() ? classLevelExceptionMappers.get().getMappers()
                                    : Collections.emptyMap())
                    .setInitConverters(initConverters).build();

            if (selectedAppClass != null) {
                globalNameBindings = serverEndpointIndexer.nameBindingNames(selectedAppClass);
            }

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
            for (DotName methodAnnotation : resourceScanningResultBuildItem.get().getHttpAnnotationToMethod().keySet()) {
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

            ResourceInterceptors interceptors = new ResourceInterceptors();
            for (ContainerRequestFilterBuildItem filter : containerRequestFilters) {
                registerInterceptors(beanContainerBuildItem, recorder, singletonClasses,
                        globalNameBindings, interceptors.getContainerRequestFilters(), filter);
            }

            for (ContainerResponseFilterBuildItem filterClass : containerResponseFilters) {
                registerInterceptors(beanContainerBuildItem, recorder, singletonClasses,
                        globalNameBindings, interceptors.getContainerResponseFilters(), filterClass);
            }
            for (WriterInterceptorBuildItem filterClass : writerInterceptors) {
                registerInterceptors(beanContainerBuildItem, recorder, singletonClasses,
                        globalNameBindings, interceptors.getWriterInterceptors(), filterClass);
            }
            for (ReaderInterceptorBuildItem filterClass : readerInterceptors) {
                registerInterceptors(beanContainerBuildItem, recorder, singletonClasses,
                        globalNameBindings, interceptors.getReaderInterceptors(), filterClass);
            }

            ExceptionMapping exceptionMapping = new ExceptionMapping();
            Map<DotName, ResourceExceptionMapper<Throwable>> handledExceptionToHigherPriorityMapper = new HashMap<>();
            for (ExceptionMapperBuildItem additionalExceptionMapper : exceptionMappers) {
                DotName handledExceptionDotName = DotName.createSimple(additionalExceptionMapper.getHandledExceptionName());
                int priority = Priorities.USER;
                if (additionalExceptionMapper.getPriority() != null) {
                    priority = additionalExceptionMapper.getPriority();
                }
                registerExceptionMapper(recorder, handledExceptionToHigherPriorityMapper,
                        beanContainerBuildItem,
                        additionalExceptionMapper.getClassName(),
                        handledExceptionDotName,
                        priority, singletonClasses);
            }
            for (Map.Entry<DotName, ResourceExceptionMapper<Throwable>> entry : handledExceptionToHigherPriorityMapper
                    .entrySet()) {
                recorder.registerExceptionMapper(exceptionMapping, entry.getKey().toString(), entry.getValue());
            }

            ContextResolvers ctxResolvers = new ContextResolvers();
            for (ContextResolverBuildItem resolverClass : contextResolvers) {
                ResourceContextResolver resolver = new ResourceContextResolver();
                resolver.setFactory(
                        FactoryUtils.factory(resolverClass.getClassName(), singletonClasses, recorder, beanContainerBuildItem));
                resolver.setMediaTypeStrings(resolverClass.getMediaTypes());
                recorder.registerContextResolver(ctxResolvers, resolverClass.getProvidedType(), resolver);
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

            for (AdditionalReaders.Entry additionalReader : additionalReaders.get()) {
                Class readerClass = additionalReader.getReaderClass();
                registerReader(recorder, serialisers, additionalReader.getEntityClass(), readerClass,
                        beanContainerBuildItem.getValue(), additionalReader.getMediaType(), additionalReader.getConstraint());
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, readerClass.getName()));
            }

            for (AdditionalWriters.Entry<?> entry : additionalWriters.get()) {
                Class<? extends MessageBodyWriter<?>> writerClass = entry.getWriterClass();
                registerWriter(recorder, serialisers, entry.getEntityClass(), writerClass,
                        beanContainerBuildItem.getValue(), entry.getMediaType());
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, writerClass.getName()));
            }

            initConverters.returnValue(null);
            BeanFactory<QuarkusRestInitialiser> initClassFactory = recorder.factory(QUARKUS_INIT_CLASS,
                    beanContainerBuildItem.getValue());

            String applicationPath = determineApplicationPath(index);
            // spec allows the path contain encoded characters
            if ((applicationPath != null) && applicationPath.contains("%")) {
                applicationPath = Encode.decodePath(applicationPath);
            }

            // Handler used for both the default and non-default deployment path (specified as application path or resteasyConfig.path)
            // Routes use the order VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1 to ensure the default route is called before the resteasy one
            Handler<RoutingContext> handler = recorder.handler(interceptors.sort(), exceptionMapping, ctxResolvers, feats,
                    dynamicFeats,
                    serialisers, resourceClasses, subResourceClasses,
                    beanContainerBuildItem.getValue(), shutdownContext, config, vertxConfig, applicationPath,
                    converterProviders, initClassFactory,
                    application == null ? Application.class : application.getClass(), singletonClasses.isEmpty());

            String deploymentPath = sanitizeApplicationPath(applicationPath);
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
            routes.produce(new RouteBuildItem(new BasicRoute(matchPath, VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1), handler));
        }
    }

    protected <T, B extends AbstractInterceptorBuildItem> void registerInterceptors(
            BeanContainerBuildItem beanContainerBuildItem, QuarkusRestRecorder recorder,
            Set<String> singletonClasses, Set<String> globalNameBindings,
            InterceptorContainer<T> interceptors, B filterItem) {
        ResourceInterceptor<T> interceptor = interceptors.create();
        Integer priority = filterItem.getPriority();
        if (priority != null) {
            interceptor.setPriority(priority);
        }
        interceptor
                .setFactory(
                        FactoryUtils.factory(filterItem.getClassName(), singletonClasses, recorder, beanContainerBuildItem));
        if (interceptors instanceof PreMatchInterceptorContainer
                && ((ContainerRequestFilterBuildItem) filterItem).isPreMatching()) {
            ((PreMatchInterceptorContainer<T>) interceptors).addPreMatchInterceptor(interceptor);

        } else {
            Set<String> nameBindingNames = filterItem.getNameBindingNames();
            if (nameBindingNames.isEmpty() || namePresent(nameBindingNames, globalNameBindings)) {
                interceptors.addGlobalRequestInterceptor(interceptor);
            } else {
                interceptor.setNameBindingNames(nameBindingNames);
                interceptors.addNameRequestInterceptor(interceptor);
            }
        }

    }

    private boolean namePresent(Set<String> nameBindingNames, Set<String> globalNameBindings) {
        for (String i : globalNameBindings) {
            if (nameBindingNames.contains(i)) {
                return true;
            }
        }
        return false;
    }

    private void registerExceptionMapper(QuarkusRestRecorder recorder,
            Map<DotName, ResourceExceptionMapper<Throwable>> handledExceptionToHigherPriorityMapper,
            BeanContainerBuildItem beanContainerBuildItem,
            String mapperClassName,
            DotName handledExceptionDotName, int priority, Set<String> singletonClasses) {
        ResourceExceptionMapper<Throwable> mapper = new ResourceExceptionMapper<>();
        mapper.setPriority(priority);
        mapper.setFactory(FactoryUtils.factory(mapperClassName, singletonClasses, recorder, beanContainerBuildItem));
        if (handledExceptionToHigherPriorityMapper.containsKey(handledExceptionDotName)) {
            if (mapper.getPriority() < handledExceptionToHigherPriorityMapper.get(handledExceptionDotName)
                    .getPriority()) {
                handledExceptionToHigherPriorityMapper.put(handledExceptionDotName, mapper);
            }
        } else {
            handledExceptionToHigherPriorityMapper.put(handledExceptionDotName, mapper);
        }
    }

    private String determineApplicationPath(IndexView index) {
        Collection<AnnotationInstance> applicationPaths = index.getAnnotations(QuarkusRestDotNames.APPLICATION_PATH);
        if (applicationPaths.isEmpty()) {
            return null;
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

    private void registerWriter(QuarkusRestRecorder recorder, ServerSerialisers serialisers, Class<?> entityClass,
            Class<? extends MessageBodyWriter<?>> writerClass, BeanContainer beanContainer,
            String mediaType) {
        ResourceWriter writer = new ResourceWriter();
        writer.setFactory(recorder.factory(writerClass.getName(), beanContainer));
        writer.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerWriter(serialisers, entityClass.getName(), writer);
    }

    private void registerReader(QuarkusRestRecorder recorder, ServerSerialisers serialisers, Class<?> entityClass,
            Class<? extends MessageBodyReader<?>> readerClass, BeanContainer beanContainer, String mediaType,
            RuntimeType constraint) {
        ResourceReader reader = new ResourceReader();
        reader.setFactory(recorder.factory(readerClass.getName(), beanContainer));
        reader.setMediaTypeStrings(Collections.singletonList(mediaType));
        reader.setConstraint(constraint);
        recorder.registerReader(serialisers, entityClass.getName(), reader);
    }

}
