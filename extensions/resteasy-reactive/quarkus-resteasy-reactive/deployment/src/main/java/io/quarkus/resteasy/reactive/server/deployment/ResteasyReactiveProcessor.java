package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_REQUEST;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_RESPONSE;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.ROUTING_CONTEXT;
import static java.util.stream.Collectors.toList;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DATE_FORMAT;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
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
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.TargetJavaVersion;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;
import org.jboss.resteasy.reactive.common.types.AllWriteableMarker;
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
import org.jboss.resteasy.reactive.server.model.ServerMethodParameter;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.processor.generation.converters.GeneratedConverterIndexerExtension;
import org.jboss.resteasy.reactive.server.processor.generation.exceptionmappers.ServerExceptionMapperGenerator;
import org.jboss.resteasy.reactive.server.processor.generation.injection.TransformedFieldInjectionIndexerExtension;
import org.jboss.resteasy.reactive.server.processor.generation.multipart.GeneratedHandlerMultipartReturnTypeIndexerExtension;
import org.jboss.resteasy.reactive.server.processor.generation.multipart.GeneratedMultipartParamIndexerExtension;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResponseHeaderMethodScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResponseStatusMethodScanner;
import org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerMutinyAsyncFileMessageBodyWriter;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerMutinyBufferMessageBodyWriter;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerVertxAsyncFileMessageBodyWriter;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerVertxBufferMessageBodyWriter;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.arc.Unremovable;
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
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RecordableConstructorBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.netty.deployment.MinNettyAllocatorMaxOrderBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.FactoryUtils;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusFactoryCreator;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames;
import io.quarkus.resteasy.reactive.common.deployment.ResourceInterceptorsBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.SerializersUtil;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveInitialiser;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRuntimeRecorder;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveServerRuntimeConfig;
import io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.AuthenticationCompletionExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.AuthenticationFailedExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.AuthenticationRedirectExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.ForbiddenExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.reactive.server.runtime.security.EagerSecurityHandler;
import io.quarkus.resteasy.reactive.server.runtime.security.SecurityContextOverrideHandler;
import io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.ContextTypeBuildItem;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.NonBlockingReturnTypeBuildItem;
import io.quarkus.resteasy.reactive.server.spi.ResumeOn404BuildItem;
import io.quarkus.resteasy.reactive.spi.CustomExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.ExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.JaxrsFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterOverrideBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class ResteasyReactiveProcessor {

    private static final int REST_ROUTE_ORDER_OFFSET = 500;

    private static final String QUARKUS_INIT_CLASS = "io.quarkus.rest.runtime.__QuarkusInit";

    private static final Logger log = Logger.getLogger("io.quarkus.resteasy.reactive.server");

    private static final Predicate<Object[]> isEmpty = array -> array == null || array.length == 0;

    private static final Set<DotName> CONTEXT_TYPES = Set.of(
            DotName.createSimple(HttpServerRequest.class.getName()),
            DotName.createSimple(HttpServerResponse.class.getName()),
            DotName.createSimple(RoutingContext.class.getName()));
    private static final DotName FILE = DotName.createSimple(File.class.getName());

    private static final int SECURITY_EXCEPTION_MAPPERS_PRIORITY = Priorities.USER + 1;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    @BuildStep
    public FeatureBuildItem buildSetup() {
        return new FeatureBuildItem(Feature.RESTEASY_REACTIVE);
    }

    // This is required to get rid of netty exceptions when allocating direct buffers in tests running
    // in IDEs, which have assertions enabled, otherwise we run into:
    /*
     * java.lang.AssertionError
     * at io.netty.buffer.PoolChunk.calculateRunSize(PoolChunk.java:366)
     * at io.netty.buffer.PoolChunk.allocateSubpage(PoolChunk.java:424)
     * at io.netty.buffer.PoolChunk.allocate(PoolChunk.java:299)
     * at io.netty.buffer.PoolArena.allocateNormal(PoolArena.java:205)
     * at io.netty.buffer.PoolArena.tcacheAllocateSmall(PoolArena.java:174)
     * at io.netty.buffer.PoolArena.allocate(PoolArena.java:136)
     * at io.netty.buffer.PoolArena.allocate(PoolArena.java:128)
     *
     * at io.netty.buffer.PooledByteBufAllocator.newDirectBuffer(PooledByteBufAllocator.java:378)
     * at io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:187)
     * at io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:178)
     * at io.vertx.core.net.impl.PartialPooledByteBufAllocator.directBuffer(PartialPooledByteBufAllocator.java:92)
     */
    @BuildStep
    MinNettyAllocatorMaxOrderBuildItem setMinimalNettyMaxOrderSize() {
        return new MinNettyAllocatorMaxOrderBuildItem(3);
    }

    @BuildStep
    void recordableConstructor(BuildProducer<RecordableConstructorBuildItem> ctors) {
        ctors.produce(new RecordableConstructorBuildItem(ServerResourceMethod.class));
        ctors.produce(new RecordableConstructorBuildItem(ServerMethodParameter.class));
    }

    @BuildStep
    MethodScannerBuildItem responseStatusSupport() {
        return new MethodScannerBuildItem(new ResponseStatusMethodScanner());
    }

    @BuildStep
    MethodScannerBuildItem responseHeaderSupport() {
        return new MethodScannerBuildItem(new ResponseHeaderMethodScanner());
    }

    @BuildStep
    void vertxIntegration(BuildProducer<MessageBodyWriterBuildItem> writerBuildItemBuildProducer) {
        writerBuildItemBuildProducer.produce(new MessageBodyWriterBuildItem(ServerVertxBufferMessageBodyWriter.class.getName(),
                io.vertx.core.buffer.Buffer.class.getName(), Collections.singletonList(MediaType.WILDCARD), RuntimeType.SERVER,
                true,
                Priorities.USER));
        writerBuildItemBuildProducer.produce(new MessageBodyWriterBuildItem(ServerMutinyBufferMessageBodyWriter.class.getName(),
                io.vertx.mutiny.core.buffer.Buffer.class.getName(), Collections.singletonList(MediaType.WILDCARD),
                RuntimeType.SERVER, true,
                Priorities.USER));
        writerBuildItemBuildProducer
                .produce(new MessageBodyWriterBuildItem(ServerVertxAsyncFileMessageBodyWriter.class.getName(),
                        io.vertx.core.file.AsyncFile.class.getName(), Collections.singletonList(MediaType.WILDCARD),
                        RuntimeType.SERVER, true,
                        Priorities.USER));
        writerBuildItemBuildProducer
                .produce(new MessageBodyWriterBuildItem(ServerMutinyAsyncFileMessageBodyWriter.class.getName(),
                        io.vertx.mutiny.core.file.AsyncFile.class.getName(), Collections.singletonList(MediaType.WILDCARD),
                        RuntimeType.SERVER, true,
                        Priorities.USER));
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

    //TODO: replace with MethodLevelExceptionMappingFeature
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
                    classOutput,
                    Set.of(HTTP_SERVER_REQUEST, HTTP_SERVER_RESPONSE, ROUTING_CONTEXT), Set.of(Unremovable.class.getName()));
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, false, false, generationResult.values().toArray(
                            EMPTY_STRING_ARRAY)));
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
    public void unremovableBeans(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }
        Set<String> beanParams = resourceScanningResultBuildItem.get().getResult()
                .getBeanParams();
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(beanParams.toArray(EMPTY_STRING_ARRAY)));
    }

    @BuildStep
    public void additionalAsyncTypeMethodScanners(List<NonBlockingReturnTypeBuildItem> buildItems,
            BuildProducer<MethodScannerBuildItem> producer) {
        for (NonBlockingReturnTypeBuildItem bi : buildItems) {
            producer.produce(new MethodScannerBuildItem(new MethodScanner() {
                @Override
                public boolean isMethodSignatureAsync(MethodInfo info) {
                    return info.returnType().name().equals(bi.getType());
                }
            }));
        }
    }

    @BuildStep
    //note useIdentityComparisonForParameters=false
    //resteasy can generate lots of small collections with similar values as part of its metadata gathering
    //this allows multiple objects to be compressed into a single object at runtime
    //saving memory and reducing reload time
    @Record(value = ExecutionTime.STATIC_INIT, useIdentityComparisonForParameters = false)
    public void setupEndpoints(ApplicationIndexBuildItem applicationIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            ResteasyReactiveConfig config,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            ResteasyReactiveRecorder recorder,
            List<ServerDefaultProducesHandlerBuildItem> serverDefaultProducesHandlers,
            Optional<ClassLevelExceptionMappersBuildItem> classLevelExceptionMappers,
            BuildProducer<SetupEndpointsResultBuildItem> setupEndpointsResultProducer,
            BuildProducer<ResteasyReactiveResourceMethodEntriesBuildItem> resourceMethodEntriesBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            ApplicationResultBuildItem applicationResultBuildItem,
            ParamConverterProvidersBuildItem paramConverterProvidersBuildItem,
            List<ApplicationClassPredicateBuildItem> applicationClassPredicateBuildItems,
            List<MethodScannerBuildItem> methodScanners,
            List<AnnotationsTransformerBuildItem> annotationTransformerBuildItems,
            List<ContextTypeBuildItem> contextTypeBuildItems,
            CompiledJavaVersionBuildItem compiledJavaVersionBuildItem,
            Capabilities capabilities)
            throws NoSuchMethodException {

        if (!resourceScanningResultBuildItem.isPresent()) {
            // no detected @Path, bail out
            return;
        }

        IndexView index = beanArchiveIndexBuildItem.getIndex();

        ResourceScanningResult result = resourceScanningResultBuildItem.get().getResult();
        Map<DotName, ClassInfo> scannedResources = result.getScannedResources();
        Map<DotName, String> scannedResourcePaths = result.getScannedResourcePaths();
        Map<DotName, String> pathInterfaces = result.getPathInterfaces();

        ApplicationScanningResult appResult = applicationResultBuildItem.getResult();
        Set<String> singletonClasses = appResult.getSingletonClasses();

        Map<String, String> existingConverters = new HashMap<>();
        List<ResourceClass> resourceClasses = new ArrayList<>();
        List<ResourceClass> subResourceClasses = new ArrayList<>();
        AdditionalReaders additionalReaders = new AdditionalReaders();
        AdditionalWriters additionalWriters = new AdditionalWriters();
        Map<String, InjectableBean> injectableBeans = new HashMap<>();
        QuarkusServerEndpointIndexer serverEndpointIndexer;

        ParamConverterProviders paramConverterProviders = paramConverterProvidersBuildItem.getParamConverterProviders();
        Function<String, BeanFactory<?>> factoryFunction = s -> FactoryUtils.factory(s, singletonClasses, recorder,
                beanContainerBuildItem);
        paramConverterProviders.initializeDefaultFactories(factoryFunction);
        paramConverterProviders.sort();

        GeneratedClassGizmoAdaptor classOutput = new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true);
        try (ClassCreator c = new ClassCreator(classOutput,
                QUARKUS_INIT_CLASS, null, Object.class.getName(), ResteasyReactiveInitialiser.class.getName());
                MethodCreator initConverters = c.getMethodCreator("init", void.class, Deployment.class)) {

            List<ResteasyReactiveResourceMethodEntriesBuildItem.Entry> resourceMethodEntries = new ArrayList<>();

            Predicate<String> applicationClassPredicate = s -> {
                for (ApplicationClassPredicateBuildItem i : applicationClassPredicateBuildItems) {
                    if (i.test(s)) {
                        return true;
                    }
                }
                return false;
            };

            BiConsumer<String, BiFunction<String, ClassVisitor, ClassVisitor>> transformationConsumer = (name,
                    function) -> bytecodeTransformerBuildItemBuildProducer
                            .produce(new BytecodeTransformerBuildItem(name, function));
            QuarkusServerEndpointIndexer.Builder serverEndpointIndexerBuilder = new QuarkusServerEndpointIndexer.Builder(
                    capabilities)
                    .addMethodScanners(
                            methodScanners.stream().map(MethodScannerBuildItem::getMethodScanner).collect(toList()))
                    .setIndex(index)
                    .setApplicationIndex(applicationIndexBuildItem.getIndex())
                    .addContextTypes(additionalContextTypes(contextTypeBuildItems))
                    .setFactoryCreator(new QuarkusFactoryCreator(recorder, beanContainerBuildItem.getValue()))
                    .setEndpointInvokerFactory(
                            new QuarkusInvokerFactory(generatedClassBuildItemBuildProducer, recorder))
                    .setGeneratedClassBuildItemBuildProducer(generatedClassBuildItemBuildProducer)
                    .setBytecodeTransformerBuildProducer(bytecodeTransformerBuildItemBuildProducer)
                    .setReflectiveClassProducer(reflectiveClassBuildItemBuildProducer)
                    .setExistingConverters(existingConverters)
                    .setScannedResourcePaths(scannedResourcePaths)
                    .setConfig(createRestReactiveConfig(config))
                    .setAdditionalReaders(additionalReaders)
                    .setHttpAnnotationToMethod(result.getHttpAnnotationToMethod())
                    .setInjectableBeans(injectableBeans)
                    .setAdditionalWriters(additionalWriters)
                    .setDefaultBlocking(appResult.getBlockingDefault())
                    .setApplicationScanningResult(appResult)
                    .setMultipartParameterIndexerExtension(
                            new GeneratedMultipartParamIndexerExtension(transformationConsumer, classOutput))
                    .setMultipartReturnTypeIndexerExtension(
                            new GeneratedHandlerMultipartReturnTypeIndexerExtension(classOutput))
                    .setFieldInjectionIndexerExtension(
                            new TransformedFieldInjectionIndexerExtension(transformationConsumer, false, (field) -> {
                                initConverters.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(field.getInjectedClassName(),
                                                field.getMethodName(),
                                                void.class, Deployment.class),
                                        initConverters.getMethodParam(0));
                            }))
                    .setConverterSupplierIndexerExtension(new GeneratedConverterIndexerExtension(
                            (name) -> new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer,
                                    applicationClassPredicate.test(name))))
                    .setHasRuntimeConverters(!paramConverterProviders.getParamConverterProviders().isEmpty())
                    .setClassLevelExceptionMappers(
                            classLevelExceptionMappers.isPresent() ? classLevelExceptionMappers.get().getMappers()
                                    : Collections.emptyMap())
                    .setResourceMethodCallback(new Consumer<>() {
                        @Override
                        public void accept(EndpointIndexer.ResourceMethodCallbackData entry) {
                            MethodInfo method = entry.getMethodInfo();

                            resourceMethodEntries.add(new ResteasyReactiveResourceMethodEntriesBuildItem.Entry(
                                    entry.getBasicResourceClassInfo(), method,
                                    entry.getActualEndpointInfo(), entry.getResourceMethod()));

                            String source = ResteasyReactiveProcessor.class.getSimpleName() + " > "
                                    + method.declaringClass()
                                    + "[" + method + "]";

                            ClassInfo classInfoWithSecurity = consumeStandardSecurityAnnotations(method,
                                    entry.getActualEndpointInfo(), index, c -> c);
                            if (classInfoWithSecurity != null) {
                                reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, false,
                                        entry.getActualEndpointInfo().name().toString()));
                            }

                            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                                    .type(method.returnType())
                                    .index(index)
                                    .ignoreTypePredicate(
                                            QuarkusResteasyReactiveDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                                    .ignoreFieldPredicate(
                                            QuarkusResteasyReactiveDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                                    .ignoreMethodPredicate(
                                            QuarkusResteasyReactiveDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                                    .source(source)
                                    .build());

                            for (short i = 0; i < method.parametersCount(); i++) {
                                Type parameterType = method.parameterType(i);
                                if (!hasAnnotation(method, i, ResteasyReactiveServerDotNames.CONTEXT)) {
                                    reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                                            .type(parameterType)
                                            .index(index)
                                            .ignoreTypePredicate(
                                                    QuarkusResteasyReactiveDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                                            .ignoreFieldPredicate(
                                                    QuarkusResteasyReactiveDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                                            .ignoreMethodPredicate(
                                                    QuarkusResteasyReactiveDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                                            .source(source)
                                            .build());
                                }
                                if (parameterType.name().equals(FILE)) {
                                    reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, false,
                                            entry.getActualEndpointInfo().name().toString()));
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
                    .setResteasyReactiveRecorder(recorder)
                    .setApplicationClassPredicate(applicationClassPredicate)
                    .setTargetJavaVersion(new TargetJavaVersion() {

                        private final Status result;

                        {
                            CompiledJavaVersionBuildItem.JavaVersion.Status status = compiledJavaVersionBuildItem
                                    .getJavaVersion().isJava19OrHigher();
                            if (status == CompiledJavaVersionBuildItem.JavaVersion.Status.FALSE) {
                                result = Status.FALSE;
                            } else if (status == CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE) {
                                result = Status.TRUE;
                            } else {
                                result = Status.UNKNOWN;
                            }
                        }

                        @Override
                        public Status isJava19OrHigher() {
                            return result;
                        }
                    });

            if (!serverDefaultProducesHandlers.isEmpty()) {
                List<DefaultProducesHandler> handlers = new ArrayList<>(serverDefaultProducesHandlers.size());
                for (ServerDefaultProducesHandlerBuildItem bi : serverDefaultProducesHandlers) {
                    handlers.add(bi.getDefaultProducesHandler());
                }
                serverEndpointIndexerBuilder
                        .setDefaultProducesHandler(new DefaultProducesHandler.DelegatingDefaultProducesHandler(handlers));
            }

            if (!annotationTransformerBuildItems.isEmpty()) {
                List<AnnotationsTransformer> annotationsTransformers = new ArrayList<>(annotationTransformerBuildItems.size());
                for (AnnotationsTransformerBuildItem bi : annotationTransformerBuildItems) {
                    annotationsTransformers.add(bi.getAnnotationsTransformer());
                }
                serverEndpointIndexerBuilder.setAnnotationsTransformers(annotationsTransformers);
            }

            serverEndpointIndexerBuilder.setMultipartReturnTypeIndexerExtension(new QuarkusMultipartReturnTypeHandler(
                    generatedClassBuildItemBuildProducer, applicationClassPredicate, reflectiveClassBuildItemBuildProducer));
            serverEndpointIndexerBuilder.setMultipartParameterIndexerExtension(new QuarkusMultipartParamHandler(
                    generatedClassBuildItemBuildProducer, applicationClassPredicate, reflectiveClassBuildItemBuildProducer,
                    bytecodeTransformerBuildItemBuildProducer));
            serverEndpointIndexer = serverEndpointIndexerBuilder.build();

            Map<String, List<EndpointConfig>> allMethods = new HashMap<>();
            for (ClassInfo i : scannedResources.values()) {
                Optional<ResourceClass> endpoints = serverEndpointIndexer.createEndpoints(i, true);
                if (endpoints.isPresent()) {
                    if (singletonClasses.contains(i.name().toString())) {
                        endpoints.get().setFactory(new SingletonBeanFactory<>(i.name().toString()));
                    }
                    resourceClasses.add(endpoints.get());
                    for (ResourceMethod rm : endpoints.get().getMethods()) {
                        addResourceMethodByPath(allMethods, endpoints.get().getPath(), i, rm);
                    }
                }
            }

            checkForDuplicateEndpoint(config, allMethods);

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
            //sub resources can also have just a path annotation
            //if they are 'intermediate' sub resources
            for (AnnotationInstance instance : index.getAnnotations(ResteasyReactiveDotNames.PATH)) {
                if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo method = instance.target().asMethod();
                    ClassInfo classInfo = method.declaringClass();
                    toScan.add(classInfo);
                }
            }
            Map<DotName, ClassInfo> possibleSubResources = new HashMap<>();
            while (!toScan.isEmpty()) {
                ClassInfo classInfo = toScan.poll();
                if (scannedResources.containsKey(classInfo.name()) ||
                        pathInterfaces.containsKey(classInfo.name()) ||
                        possibleSubResources.containsKey(classInfo.name())) {
                    continue;
                }
                possibleSubResources.put(classInfo.name(), classInfo);
                Optional<ResourceClass> endpoints = serverEndpointIndexer.createEndpoints(classInfo, false);
                if (endpoints.isPresent()) {
                    subResourceClasses.add(endpoints.get());
                }
                //we need to also look for all subclasses and interfaces
                //they may have type variables that need to be handled
                toScan.addAll(index.getKnownDirectImplementors(classInfo.name()));
                toScan.addAll(index.getKnownDirectSubclasses(classInfo.name()));
            }

            setupEndpointsResultProducer.produce(new SetupEndpointsResultBuildItem(resourceClasses, subResourceClasses,
                    additionalReaders, additionalWriters));
            resourceMethodEntriesBuildItemBuildProducer
                    .produce(new ResteasyReactiveResourceMethodEntriesBuildItem(resourceMethodEntries));

            initConverters.returnValue(null);
        }

        handleDateFormatReflection(reflectiveClass, index);
    }

    private Collection<DotName> additionalContextTypes(List<ContextTypeBuildItem> contextTypeBuildItems) {
        if (contextTypeBuildItems.isEmpty()) {
            return CONTEXT_TYPES;
        }
        Set<DotName> contextTypes = new HashSet<>(CONTEXT_TYPES.size() + contextTypeBuildItems.size());
        contextTypes.addAll(CONTEXT_TYPES);
        for (ContextTypeBuildItem bi : contextTypeBuildItems) {
            contextTypes.add(bi.getType());
        }
        return contextTypes;
    }

    private void handleDateFormatReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, IndexView index) {
        Collection<AnnotationInstance> dateFormatInstances = index.getAnnotations(DATE_FORMAT);
        if (dateFormatInstances.isEmpty()) {
            return;
        }
        List<String> dateTimeFormatterProviderClassNames = new ArrayList<>();
        for (AnnotationInstance instance : dateFormatInstances) {
            AnnotationValue dateTimeFormatterProviderValue = instance.value("dateTimeFormatterProvider");
            if (dateTimeFormatterProviderValue != null) {
                dateTimeFormatterProviderClassNames.add(dateTimeFormatterProviderValue.asClass().name().toString());
            }
        }
        if (!dateTimeFormatterProviderClassNames.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, false,
                    dateTimeFormatterProviderClassNames.toArray(EMPTY_STRING_ARRAY)));
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, useIdentityComparisonForParameters = false)
    public void serverSerializers(ResteasyReactiveRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            List<MessageBodyReaderBuildItem> additionalMessageBodyReaders,
            List<MessageBodyWriterBuildItem> additionalMessageBodyWriters,
            List<MessageBodyReaderOverrideBuildItem> messageBodyReaderOverrideBuildItems,
            List<MessageBodyWriterOverrideBuildItem> messageBodyWriterOverrideBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServerSerialisersBuildItem> serverSerializersProducer) {

        ServerSerialisers serialisers = recorder.createServerSerialisers();
        SerializersUtil.setupSerializers(recorder, reflectiveClass, additionalMessageBodyReaders,
                additionalMessageBodyWriters, messageBodyReaderOverrideBuildItems, messageBodyWriterOverrideBuildItems,
                beanContainerBuildItem, applicationResultBuildItem, serialisers,
                RuntimeType.SERVER);

        // built-ins
        for (Serialisers.BuiltinWriter builtinWriter : ServerSerialisers.BUILTIN_WRITERS) {
            registerWriter(recorder, serialisers, builtinWriter.entityClass.getName(), builtinWriter.writerClass.getName(),
                    beanContainerBuildItem.getValue(),
                    builtinWriter.mediaType);
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, builtinWriter.writerClass.getName()));
        }
        for (Serialisers.BuiltinReader builtinReader : ServerSerialisers.BUILTIN_READERS) {
            registerReader(recorder, serialisers, builtinReader.entityClass.getName(), builtinReader.readerClass.getName(),
                    beanContainerBuildItem.getValue(),
                    builtinReader.mediaType, builtinReader.constraint);
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, builtinReader.readerClass.getName()));
        }

        serverSerializersProducer.produce(new ServerSerialisersBuildItem(serialisers));
    }

    @BuildStep
    public void additionalReflection(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            SetupEndpointsResultBuildItem setupEndpointsResult,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            BuildProducer<ReflectiveClassBuildItem> producer) {
        List<ResourceClass> resourceClasses = setupEndpointsResult.getResourceClasses();
        IndexView index = beanArchiveIndexBuildItem.getIndex();

        // when user provided MessageBodyWriter classes exist that do not extend ServerMessageBodyWriter, we need to enable reflection
        // on every resource method, because these providers will be checked first and the JAX-RS API requires
        // the method return type and annotations to be passed to the serializers
        boolean serializersRequireResourceReflection = false;
        for (var writer : messageBodyWriterBuildItems) {
            if (writer.isBuiltin()) {
                continue;
            }
            if ((writer.getRuntimeType() != null) && (writer.getRuntimeType() == RuntimeType.CLIENT)) {
                continue;
            }
            ClassInfo writerClassInfo = index.getClassByName(DotName.createSimple(writer.getClassName()));
            if (writerClassInfo == null) {
                continue;
            }
            List<DotName> interfaceNames = writerClassInfo.interfaceNames();
            if (!interfaceNames.contains(ResteasyReactiveServerDotNames.SERVER_MESSAGE_BODY_WRITER)) {
                serializersRequireResourceReflection = true;
                break;
            }
        }
        if (!serializersRequireResourceReflection) {
            for (var reader : messageBodyReaderBuildItems) {
                if (reader.isBuiltin()) {
                    continue;
                }
                if ((reader.getRuntimeType() != null) && (reader.getRuntimeType() == RuntimeType.CLIENT)) {
                    continue;
                }
                ClassInfo writerClassInfo = index.getClassByName(DotName.createSimple(reader.getClassName()));
                if (writerClassInfo == null) {
                    continue;
                }
                List<DotName> interfaceNames = writerClassInfo.interfaceNames();
                if (!interfaceNames.contains(ResteasyReactiveServerDotNames.SERVER_MESSAGE_BODY_READER)) {
                    serializersRequireResourceReflection = true;
                    break;
                }
            }
        }
        if (serializersRequireResourceReflection) {
            producer.produce(ReflectiveClassBuildItem
                    .builder(resourceClasses.stream().map(ResourceClass::getClassName).toArray(String[]::new)).fields(false)
                    .constructors(false).methods(true).build());
        }
    }

    @SuppressWarnings("unchecked")
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, useIdentityComparisonForParameters = false)
    public void setupDeployment(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BeanContainerBuildItem beanContainerBuildItem,
            Capabilities capabilities,
            ResteasyReactiveConfig config,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            ResteasyReactiveRecorder recorder,
            RecorderContext recorderContext,
            ShutdownContextBuildItem shutdownContext,
            HttpBuildTimeConfig vertxConfig,
            SetupEndpointsResultBuildItem setupEndpointsResult,
            ServerSerialisersBuildItem serverSerialisersBuildItem,
            List<DynamicFeatureBuildItem> dynamicFeatures,
            List<JaxrsFeatureBuildItem> features,
            Optional<RequestContextFactoryBuildItem> requestContextFactoryBuildItem,
            BuildProducer<ResteasyReactiveDeploymentInfoBuildItem> quarkusRestDeploymentInfoBuildItemBuildProducer,
            BuildProducer<ResteasyReactiveDeploymentBuildItem> quarkusRestDeploymentBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RouteBuildItem> routes,
            ApplicationResultBuildItem applicationResultBuildItem,
            ResourceInterceptorsBuildItem resourceInterceptorsBuildItem,
            ExceptionMappersBuildItem exceptionMappersBuildItem,
            ParamConverterProvidersBuildItem paramConverterProvidersBuildItem,
            ContextResolversBuildItem contextResolversBuildItem,
            ResteasyReactiveServerConfig serverConfig,
            LaunchModeBuildItem launchModeBuildItem,
            List<ResumeOn404BuildItem> resumeOn404Items)
            throws NoSuchMethodException {

        if (!resourceScanningResultBuildItem.isPresent()) {
            // no detected @Path, bail out
            return;
        }

        recorderContext.registerNonDefaultConstructor(
                MediaType.class.getDeclaredConstructor(String.class, String.class, String.class),
                mediaType -> Stream.of(mediaType.getType(), mediaType.getSubtype(), mediaType.getParameters())
                        .collect(toList()));

        IndexView index = beanArchiveIndexBuildItem.getIndex();

        ApplicationScanningResult appResult = applicationResultBuildItem.getResult();
        Set<String> singletonClasses = appResult.getSingletonClasses();
        Application application = appResult.getApplication();

        List<ResourceClass> resourceClasses = setupEndpointsResult.getResourceClasses();
        List<ResourceClass> subResourceClasses = setupEndpointsResult.getSubResourceClasses();
        AdditionalReaders additionalReaders = setupEndpointsResult.getAdditionalReaders();
        AdditionalWriters additionalWriters = setupEndpointsResult.getAdditionalWriters();

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
        interceptors.getContainerRequestFilters().validateThreadModel();

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

        ServerSerialisers serialisers = serverSerialisersBuildItem.getSerialisers();

        for (AdditionalReaderWriter.Entry additionalReader : additionalReaders.get()) {
            String readerClass = additionalReader.getHandlerClass();
            registerReader(recorder, serialisers, additionalReader.getEntityClass(), readerClass,
                    beanContainerBuildItem.getValue(), additionalReader.getMediaType(), additionalReader.getConstraint());
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, readerClass));
        }

        for (AdditionalReaderWriter.Entry entry : additionalWriters.get()) {
            String writerClass = entry.getHandlerClass();
            registerWriter(recorder, serialisers, entry.getEntityClass(), writerClass,
                    beanContainerBuildItem.getValue(), entry.getMediaType());
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, writerClass));
        }

        BeanFactory<ResteasyReactiveInitialiser> initClassFactory = recorder.factory(QUARKUS_INIT_CLASS,
                beanContainerBuildItem.getValue());

        String applicationPath = determineApplicationPath(appResult, getAppPath(serverConfig.path));
        // spec allows the path contain encoded characters
        if ((applicationPath != null) && applicationPath.contains("%")) {
            applicationPath = Encode.decodePath(applicationPath);
        }

        String deploymentPath = sanitizeApplicationPath(applicationPath);

        // Handler used for both the default and non-default deployment path (specified as application path or resteasyConfig.path)
        // Routes use the order VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1 to ensure the default route is called before the resteasy one
        Class<? extends Application> applicationClass = application == null ? Application.class : application.getClass();
        DeploymentInfo deploymentInfo = new DeploymentInfo()
                .setInterceptors(interceptors.sort())
                .setResteasyReactiveConfig(createRestReactiveConfig(config))
                .setExceptionMapping(exceptionMapping)
                .setCtxResolvers(contextResolvers)
                .setFeatures(feats)
                .setClientProxyUnwrapper(new ClientProxyUnwrapper())
                .setApplicationSupplier(recorder.handleApplication(applicationClass, singletonClasses.isEmpty()))
                .setFactoryCreator(recorder.factoryCreator(beanContainerBuildItem.getValue()))
                .setDynamicFeatures(dynamicFeats)
                .setSerialisers(serialisers)
                .setApplicationPath(applicationPath)
                .setGlobalHandlerCustomizers(Collections.singletonList(new SecurityContextOverrideHandler.Customizer())) //TODO: should be pluggable
                .setResourceClasses(resourceClasses)
                .setDevelopmentMode(launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT)
                .setLocatableResourceClasses(subResourceClasses)
                .setParamConverterProviders(paramConverterProviders);
        quarkusRestDeploymentInfoBuildItemBuildProducer
                .produce(new ResteasyReactiveDeploymentInfoBuildItem(deploymentInfo));

        boolean servletPresent = false;
        int order = VertxHttpRecorder.AFTER_DEFAULT_ROUTE_ORDER_MARK + REST_ROUTE_ORDER_OFFSET;
        if (capabilities.isPresent("io.quarkus.servlet")) {
            //if servlet is present we run RR before the default route
            //otherwise we run after it
            order = VertxHttpRecorder.BEFORE_DEFAULT_ROUTE_ORDER_MARK + REST_ROUTE_ORDER_OFFSET;
            servletPresent = true;
        }

        RuntimeValue<Deployment> deployment = recorder.createDeployment(deploymentInfo,
                beanContainerBuildItem.getValue(), shutdownContext, vertxConfig,
                requestContextFactoryBuildItem.map(RequestContextFactoryBuildItem::getFactory).orElse(null),
                initClassFactory, launchModeBuildItem.getLaunchMode(), servletPresent || !resumeOn404Items.isEmpty());

        quarkusRestDeploymentBuildItemBuildProducer
                .produce(new ResteasyReactiveDeploymentBuildItem(deployment, deploymentPath));

        if (!requestContextFactoryBuildItem.isPresent()) {
            Handler<RoutingContext> handler = recorder.handler(deployment);

            // Exact match for resources matched to the root path
            routes.produce(RouteBuildItem.builder()
                    .orderedRoute(deploymentPath, order).handler(handler).build());
            String matchPath = deploymentPath;
            if (matchPath.endsWith("/")) {
                matchPath += "*";
            } else {
                matchPath += "/*";
            }
            // Match paths that begin with the deployment path
            routes.produce(
                    RouteBuildItem.builder().orderedRoute(matchPath, order)
                            .handler(handler).build());
        }
    }

    private void checkForDuplicateEndpoint(ResteasyReactiveConfig config, Map<String, List<EndpointConfig>> allMethods) {
        String message = allMethods.values().stream()
                .map(this::getDuplicateEndpointMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
        if (message.length() > 0) {
            if (config.failOnDuplicate) {
                throw new ConfigurationError(message);
            }
            log.warn(message);
        }
    }

    private void addResourceMethodByPath(Map<String, List<EndpointConfig>> allMethods, String path, ClassInfo info,
            ResourceMethod rm) {
        allMethods.computeIfAbsent(getEndpointClassifier(rm, path), key -> new ArrayList<>())
                .addAll(getEndpointConfigs(path, info, rm));
    }

    private String getEndpointClassifier(ResourceMethod resourceMethod, String path) {
        return resourceMethod.getHttpMethod() + " " + (path.equals("/") ? "" : path)
                + resourceMethod.getPath();
    }

    private String getDuplicateEndpointMessage(List<EndpointConfig> endpoints) {
        if (endpoints.size() < 2) {
            return null;
        }
        StringBuilder message = new StringBuilder();
        Map<String, List<EndpointConfig>> duplicatesByMimeTypes = endpoints.stream()
                .collect(Collectors.groupingBy(EndpointConfig::toString));
        for (Map.Entry<String, List<EndpointConfig>> duplicates : duplicatesByMimeTypes.entrySet()) {
            if (duplicates.getValue().size() < 2) {
                continue;
            }
            message.append(endpoints.get(0).getExposedEndpoint())
                    .append(" is declared by :")
                    .append(System.lineSeparator());
            for (EndpointConfig config : duplicates.getValue()) {
                message.append(config.toCompleteString())
                        .append(System.lineSeparator());
            }
        }
        return message.toString();
    }

    private List<EndpointConfig> getEndpointConfigs(String path, ClassInfo info, ResourceMethod rm) {
        List<EndpointConfig> result = new ArrayList<>();
        String exposingMethod = info.name().toString() + "#" + rm.getName();
        if (isEmpty.test(rm.getConsumes()) && isEmpty.test(rm.getProduces()))
            result.add(new EndpointConfig(path, rm.getHttpMethod(), null, null, exposingMethod));
        else if (isEmpty.negate().test(rm.getConsumes()) && isEmpty.test(rm.getProduces())) {
            for (String consume : rm.getConsumes()) {
                result.add(new EndpointConfig(path, rm.getHttpMethod(), consume, null, exposingMethod));
            }
        } else if (isEmpty.test(rm.getConsumes()) && isEmpty.negate().test(rm.getProduces())) {
            for (String produce : rm.getProduces()) {
                result.add(new EndpointConfig(path, rm.getHttpMethod(), null, produce, exposingMethod));
            }
        } else {
            for (String consume : rm.getConsumes()) {
                for (String produce : rm.getProduces()) {
                    result.add(new EndpointConfig(path, rm.getHttpMethod(), consume, produce, exposingMethod));
                }
            }
        }
        return result;
    }

    private org.jboss.resteasy.reactive.common.ResteasyReactiveConfig createRestReactiveConfig(ResteasyReactiveConfig config) {
        Config mpConfig = ConfigProvider.getConfig();

        return new org.jboss.resteasy.reactive.common.ResteasyReactiveConfig(
                getEffectivePropertyValue("input-buffer-size", config.inputBufferSize.asLongValue(), Long.class, mpConfig),
                getEffectivePropertyValue("output-buffer-size", config.outputBufferSize, Integer.class, mpConfig),
                getEffectivePropertyValue("single-default-produces", config.singleDefaultProduces, Boolean.class, mpConfig),
                getEffectivePropertyValue("default-produces", config.defaultProduces, Boolean.class, mpConfig));
    }

    private <T> T getEffectivePropertyValue(String legacyPropertyName, T newPropertyValue, Class<T> propertyType,
            Config mpConfig) {
        Optional<T> legacyPropertyValue = mpConfig.getOptionalValue("quarkus.rest." + legacyPropertyName, propertyType);
        if (legacyPropertyValue.isPresent()) {
            return legacyPropertyValue.get();
        }
        return newPropertyValue;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void applyRuntimeConfig(ResteasyReactiveRuntimeRecorder recorder,
            Optional<ResteasyReactiveDeploymentBuildItem> deployment,
            ResteasyReactiveServerRuntimeConfig resteasyReactiveServerRuntimeConf) {
        if (!deployment.isPresent()) {
            return;
        }
        recorder.configure(deployment.get().getDeployment(), resteasyReactiveServerRuntimeConf);
    }

    @BuildStep
    public void securityExceptionMappers(BuildProducer<ExceptionMapperBuildItem> exceptionMapperBuildItemBuildProducer) {
        // built-ins
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                AuthenticationCompletionExceptionMapper.class.getName(),
                AuthenticationCompletionException.class.getName(),
                SECURITY_EXCEPTION_MAPPERS_PRIORITY, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                AuthenticationRedirectExceptionMapper.class.getName(),
                AuthenticationRedirectException.class.getName(),
                SECURITY_EXCEPTION_MAPPERS_PRIORITY, false));
        exceptionMapperBuildItemBuildProducer.produce(new ExceptionMapperBuildItem(
                ForbiddenExceptionMapper.class.getName(),
                ForbiddenException.class.getName(),
                SECURITY_EXCEPTION_MAPPERS_PRIORITY, false));
    }

    @BuildStep
    MethodScannerBuildItem integrateEagerSecurity(Capabilities capabilities, CombinedIndexBuildItem indexBuildItem,
            HttpBuildTimeConfig httpBuildTimeConfig) {
        if (!capabilities.isPresent(Capability.SECURITY)) {
            return null;
        }

        final boolean denyJaxRs = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.security.jaxrs.deny-unannotated-endpoints", Boolean.class).orElse(false);
        final boolean hasDefaultJaxRsRolesAllowed = ConfigProvider.getConfig()
                .getOptionalValues("quarkus.security.jaxrs.default-roles-allowed", String.class).map(l -> !l.isEmpty())
                .orElse(false);
        var index = indexBuildItem.getComputingIndex();
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                List<HandlerChainCustomizer> securityHandlerList = consumeStandardSecurityAnnotations(method,
                        actualEndpointClass, index,
                        (c) -> Collections.singletonList(
                                EagerSecurityHandler.Customizer.newInstance(httpBuildTimeConfig.auth.proactive)));
                if (securityHandlerList == null && (denyJaxRs || hasDefaultJaxRsRolesAllowed)) {
                    securityHandlerList = Collections
                            .singletonList(EagerSecurityHandler.Customizer.newInstance(httpBuildTimeConfig.auth.proactive));
                }
                return Objects.requireNonNullElse(securityHandlerList, Collections.emptyList());
            }
        });
    }

    /**
     * This results in adding {@link AllWriteableMarker} to user provided {@link MessageBodyWriter} classes
     * that handle every class
     *
     * RESTEasy Reactive already has a mechanism to do this for built-in types at the build time of that project,
     * so we don't need to do it here.
     */
    @BuildStep
    void addAllWriteableMarker(List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        List<String> messageBodyWriterClassNames = new ArrayList<>(messageBodyWriterBuildItems.size());
        for (MessageBodyWriterBuildItem bi : messageBodyWriterBuildItems) {
            if (!bi.isBuiltin() && ((bi.getRuntimeType() == null) || bi.getRuntimeType().equals(RuntimeType.SERVER))) {
                messageBodyWriterClassNames.add(bi.getClassName());
            }
        }

        for (String className : messageBodyWriterClassNames) {
            if (MessageBodyWriterTransformerUtils.shouldAddAllWriteableMarker(className,
                    Thread.currentThread().getContextClassLoader())) {
                log.debug("Class '" + className + "' will be transformed to add '" + AllWriteableMarker.class.getName()
                        + "' to its interfaces");
                producer.produce(new BytecodeTransformerBuildItem(className,
                        new BiFunction<>() {
                            @Override
                            public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                                return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                                    /**
                                     * Simply adds {@link AllWriteableMarker} to the list of implemented interfaces
                                     */
                                    @Override
                                    public void visit(int version, int access, String name, String signature,
                                            String superName,
                                            String[] interfaces) {
                                        LinkedHashSet<String> newInterfaces = new LinkedHashSet<>(interfaces.length + 1);
                                        newInterfaces.addAll(Arrays.asList(interfaces));
                                        newInterfaces.add(AllWriteableMarker.class.getName().replace('.', '/'));
                                        super.visit(version, access, name, signature, superName,
                                                newInterfaces.toArray(EMPTY_STRING_ARRAY));
                                    }
                                };
                            }
                        }));
            }
        }

    }

    @BuildStep
    void registerSecurityInterceptors(Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            // Register interceptors for standard security annotations to prevent repeated security checks
            beans.produce(new AdditionalBeanBuildItem(StandardSecurityCheckInterceptor.RolesAllowedInterceptor.class,
                    StandardSecurityCheckInterceptor.AuthenticatedInterceptor.class,
                    StandardSecurityCheckInterceptor.PermitAllInterceptor.class));
        }
    }

    private <T> T consumeStandardSecurityAnnotations(MethodInfo methodInfo, ClassInfo classInfo, IndexView index,
            Function<ClassInfo, T> function) {
        if (SecurityTransformerUtils.hasStandardSecurityAnnotation(methodInfo)) {
            return function.apply(methodInfo.declaringClass());
        }
        ClassInfo c = classInfo;
        while (c.superName() != null) {
            if (SecurityTransformerUtils.hasStandardSecurityAnnotation(c)) {
                return function.apply(c);
            }
            c = index.getClassByName(c.superName());
        }
        return null;
    }

    private Optional<String> getAppPath(Optional<String> newPropertyValue) {
        Optional<String> legacyProperty = ConfigProvider.getConfig().getOptionalValue("quarkus.rest.path", String.class);
        if (legacyProperty.isPresent()) {
            return legacyProperty;
        }

        return newPropertyValue;
    }

    private String determineApplicationPath(ApplicationScanningResult appResult,
            Optional<String> defaultPath) {
        if (appResult.getSelectedAppClass() == null) {
            return defaultPath.orElse("/");
        }
        AnnotationInstance applicationPathValue = appResult.getSelectedAppClass()
                .classAnnotation(ResteasyReactiveDotNames.APPLICATION_PATH);
        if (applicationPathValue == null) {
            return defaultPath.orElse("/");
        }
        String applicationPath = null;
        if ((applicationPathValue.value() != null)) {
            applicationPath = applicationPathValue.value().asString();
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

    private void registerWriter(ResteasyReactiveRecorder recorder, ServerSerialisers serialisers, String entityClass,
            String writerClass, BeanContainer beanContainer,
            String mediaType) {
        ResourceWriter writer = new ResourceWriter();
        writer.setFactory(recorder.factory(writerClass, beanContainer));
        writer.setMediaTypeStrings(Collections.singletonList(mediaType));
        recorder.registerWriter(serialisers, entityClass, writer);
    }

    private void registerReader(ResteasyReactiveRecorder recorder, ServerSerialisers serialisers, String entityClass,
            String readerClass, BeanContainer beanContainer, String mediaType,
            RuntimeType constraint) {
        ResourceReader reader = new ResourceReader();
        reader.setFactory(recorder.factory(readerClass, beanContainer));
        reader.setMediaTypeStrings(Collections.singletonList(mediaType));
        reader.setConstraint(constraint);
        recorder.registerReader(serialisers, entityClass, reader);
    }

}
