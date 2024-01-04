package io.quarkus.resteasy.reactive.server.deployment;

import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_REQUEST;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.HTTP_SERVER_RESPONSE;
import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.ROUTING_CONTEXT;
import static java.util.stream.Collectors.toList;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DATE_FORMAT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.LEGACY_PUBLISHER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PUBLISHER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_MULTI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;

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
import org.jboss.resteasy.reactive.common.model.InterceptorContainer;
import org.jboss.resteasy.reactive.common.model.PreMatchInterceptorContainer;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceDynamicFeature;
import org.jboss.resteasy.reactive.common.model.ResourceFeature;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
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
import org.jboss.resteasy.reactive.common.util.types.Types;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.model.DelegatingServerRestHandler;
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
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResponseHeaderMethodScanner;
import org.jboss.resteasy.reactive.server.processor.scanning.ResponseStatusMethodScanner;
import org.jboss.resteasy.reactive.server.processor.util.ResteasyReactiveServerDotNames;
import org.jboss.resteasy.reactive.server.providers.serialisers.ServerFileBodyHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerMutinyAsyncFileMessageBodyWriter;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerMutinyBufferMessageBodyWriter;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerVertxAsyncFileMessageBodyWriter;
import org.jboss.resteasy.reactive.server.vertx.serializers.ServerVertxBufferMessageBodyWriter;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
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
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.netty.deployment.MinNettyAllocatorMaxOrderBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.FactoryUtils;
import io.quarkus.resteasy.reactive.common.deployment.ParameterContainersBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusFactoryCreator;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames;
import io.quarkus.resteasy.reactive.common.deployment.ResourceInterceptorsBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.SerializersUtil;
import io.quarkus.resteasy.reactive.common.deployment.ServerDefaultProducesHandlerBuildItem;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.server.EndpointDisabled;
import io.quarkus.resteasy.reactive.server.runtime.QuarkusServerFileBodyHandler;
import io.quarkus.resteasy.reactive.server.runtime.QuarkusServerPathBodyHandler;
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
import io.quarkus.resteasy.reactive.server.runtime.security.EagerSecurityInterceptorHandler;
import io.quarkus.resteasy.reactive.server.runtime.security.SecurityContextOverrideHandler;
import io.quarkus.resteasy.reactive.server.runtime.security.SecurityEventContext;
import io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.ContextTypeBuildItem;
import io.quarkus.resteasy.reactive.server.spi.HandlerConfigurationProviderBuildItem;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.NonBlockingReturnTypeBuildItem;
import io.quarkus.resteasy.reactive.server.spi.PreExceptionMapperHandlerBuildItem;
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
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.vertx.http.deployment.EagerSecurityInterceptorBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.RouteConstants;
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
    private static final DotName ENDPOINT_DISABLED = DotName.createSimple(EndpointDisabled.class.getName());

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
                    ReflectiveClassBuildItem.builder(generationResult.values().toArray(
                            EMPTY_STRING_ARRAY)).build());
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
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            ApplicationResultBuildItem applicationResultBuildItem,
            ParamConverterProvidersBuildItem paramConverterProvidersBuildItem,
            List<ParameterContainersBuildItem> parameterContainersBuildItems,
            List<ApplicationClassPredicateBuildItem> applicationClassPredicateBuildItems,
            List<MethodScannerBuildItem> methodScanners,
            List<AnnotationsTransformerBuildItem> annotationTransformerBuildItems,
            List<ContextTypeBuildItem> contextTypeBuildItems,
            CompiledJavaVersionBuildItem compiledJavaVersionBuildItem,
            ResourceInterceptorsBuildItem resourceInterceptorsBuildItem,
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
        Set<DotName> scannedParameterContainers = new HashSet<>();

        for (ParameterContainersBuildItem parameterContainersBuildItem : parameterContainersBuildItems) {
            scannedParameterContainers.addAll(parameterContainersBuildItem.getClassNames());
        }

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
                    .addParameterContainerTypes(scannedParameterContainers)
                    .addContextTypes(additionalContextTypes(contextTypeBuildItems))
                    .setFactoryCreator(new QuarkusFactoryCreator(recorder, beanContainerBuildItem.getValue()))
                    .setEndpointInvokerFactory(
                            new QuarkusInvokerFactory(generatedClassBuildItemBuildProducer, recorder))
                    .setGeneratedClassBuildItemBuildProducer(generatedClassBuildItemBuildProducer)
                    .setExistingConverters(existingConverters)
                    .setScannedResourcePaths(scannedResourcePaths)
                    .setConfig(createRestReactiveConfig(config))
                    .setAdditionalReaders(additionalReaders)
                    .setHttpAnnotationToMethod(result.getHttpAnnotationToMethod())
                    .setInjectableBeans(injectableBeans)
                    .setAdditionalWriters(additionalWriters)
                    .setDefaultBlocking(appResult.getBlockingDefault())
                    .setApplicationScanningResult(appResult)
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
                        public void accept(EndpointIndexer.ResourceMethodCallbackEntry entry) {
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
                                reflectiveClassBuildItemBuildProducer.produce(
                                        ReflectiveClassBuildItem.builder(entry.getActualEndpointInfo().name().toString())
                                                .constructors(false).methods().build());
                            }

                            if (!result.getPossibleSubResources().containsKey(method.returnType().name())) {
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
                            }

                            boolean paramsRequireReflection = false;
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
                                    paramsRequireReflection = true;
                                    break;

                                }
                            }

                            if (paramsRequireReflection ||
                                    MULTI.toString().equals(entry.getResourceMethod().getSimpleReturnType()) ||
                                    REST_MULTI.toString().equals(entry.getResourceMethod().getSimpleReturnType()) ||
                                    PUBLISHER.toString().equals(entry.getResourceMethod().getSimpleReturnType()) ||
                                    LEGACY_PUBLISHER.toString().equals(entry.getResourceMethod().getSimpleReturnType()) ||
                                    filtersAccessResourceMethod(resourceInterceptorsBuildItem.getResourceInterceptors()) ||
                                    entry.additionalRegisterClassForReflectionCheck()) {
                                minimallyRegisterResourceClassForReflection(entry, reflectiveClassBuildItemBuildProducer);
                            }
                        }

                        private void minimallyRegisterResourceClassForReflection(
                                EndpointIndexer.ResourceMethodCallbackEntry entry,
                                BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) {
                            reflectiveClassBuildItemBuildProducer
                                    .produce(ReflectiveClassBuildItem
                                            .builder(entry.getActualEndpointInfo().name().toString())
                                            .constructors(false).methods().build());
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
                    })
                    .setIsDisabledCreator(new Function<>() {
                        @Override
                        public Supplier<Boolean> apply(ClassInfo classInfo) {
                            AnnotationInstance instance = classInfo.declaredAnnotation(ENDPOINT_DISABLED);
                            if (instance == null) {
                                return null;
                            }
                            String propertyName = instance.value("name").asString();
                            String propertyValue = instance.value("stringValue").asString();
                            AnnotationValue disableIfMissingValue = instance.value("disableIfMissing");
                            boolean disableIfMissing = disableIfMissingValue != null && disableIfMissingValue.asBoolean();
                            return recorder.disableIfPropertyMatches(propertyName, propertyValue, disableIfMissing);
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
            Set<String> resourceClassNames = null;
            while (!toScan.isEmpty()) {
                ClassInfo classInfo = toScan.poll();
                if (scannedResources.containsKey(classInfo.name()) ||
                        pathInterfaces.containsKey(classInfo.name()) ||
                        possibleSubResources.containsKey(classInfo.name())) {
                    continue;
                }
                possibleSubResources.put(classInfo.name(), classInfo);

                if (classInfo.isInterface()) {
                    int resourceClassImplCount = 0;
                    if (resourceClassNames == null) {
                        resourceClassNames = resourceClasses.stream().map(ResourceClass::getClassName)
                                .collect(Collectors.toSet());
                    }
                    for (ClassInfo impl : index.getAllKnownImplementors(classInfo.name())) {
                        if (resourceClassNames.contains(impl.name().toString())) {
                            resourceClassImplCount++;
                        }
                    }
                    if (resourceClassImplCount > 1) {
                        // this is the case were an interface doesn't denote a subresource, but it's simply used
                        // to share method and annotations between Resource classes
                        continue;
                    }
                }

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

        handleDateFormatReflection(reflectiveClassBuildItemBuildProducer, index);
    }

    private boolean filtersAccessResourceMethod(ResourceInterceptors resourceInterceptors) {
        AtomicBoolean ab = new AtomicBoolean(false);
        ResourceInterceptors.FiltersVisitor visitor = new ResourceInterceptors.FiltersVisitor() {

            @Override
            public VisitResult visitPreMatchRequestFilter(ResourceInterceptor<ContainerRequestFilter> interceptor) {
                return inspect(interceptor);
            }

            @Override
            public VisitResult visitGlobalRequestFilter(ResourceInterceptor<ContainerRequestFilter> interceptor) {
                return inspect(interceptor);
            }

            @Override
            public VisitResult visitNamedRequestFilter(ResourceInterceptor<ContainerRequestFilter> interceptor) {
                return inspect(interceptor);
            }

            @Override
            public VisitResult visitGlobalResponseFilter(ResourceInterceptor<ContainerResponseFilter> interceptor) {
                return inspect(interceptor);
            }

            @Override
            public VisitResult visitNamedResponseFilter(ResourceInterceptor<ContainerResponseFilter> interceptor) {
                return inspect(interceptor);
            }

            private VisitResult inspect(ResourceInterceptor<?> interceptor) {
                Map<String, Object> metadata = interceptor.metadata;
                if (metadata == null) {
                    return VisitResult.CONTINUE;
                }
                MethodInfo methodInfo = (MethodInfo) metadata.get(ResourceInterceptor.FILTER_SOURCE_METHOD_METADATA_KEY);
                if (methodInfo == null) {
                    return VisitResult.CONTINUE;
                }
                boolean result = createFilterClassIntrospector().usesGetResourceMethod(methodInfo);
                if (result) {
                    ab.set(true);
                    return VisitResult.ABORT;
                }
                return VisitResult.CONTINUE;
            }

            private FilterClassIntrospector createFilterClassIntrospector() {
                return new FilterClassIntrospector(Thread.currentThread().getContextClassLoader());
            }
        };
        resourceInterceptors.visitFilters(visitor);
        return ab.get();
    }

    // We want to add @Typed to resources, beanparams and providers so that they can be resolved as CDI bean using purely their
    // class as a bean type. This removes any ambiguity that potential subclasses may have.
    @BuildStep
    public void transformEndpoints(
            ResourceScanningResultBuildItem resourceScanningResultBuildItem,
            ResourceInterceptorsBuildItem resourceInterceptorsBuildItem,
            BuildProducer<io.quarkus.arc.deployment.AnnotationsTransformerBuildItem> annotationsTransformer,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {

        // all found resources and sub-resources
        Set<DotName> allResources = new HashSet<>();
        allResources.addAll(resourceScanningResultBuildItem.getResult().getScannedResources().keySet());
        allResources.addAll(resourceScanningResultBuildItem.getResult().getPossibleSubResources().keySet());

        // all found bean params
        Set<String> beanParams = resourceScanningResultBuildItem.getResult()
                .getBeanParams();

        // discovered filters and interceptors
        Set<String> filtersAndInterceptors = new HashSet<>();
        InterceptorContainer<ReaderInterceptor> readerInterceptors = resourceInterceptorsBuildItem.getResourceInterceptors()
                .getReaderInterceptors();
        readerInterceptors.getNameResourceInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));
        readerInterceptors.getGlobalResourceInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));
        InterceptorContainer<WriterInterceptor> writerInterceptors = resourceInterceptorsBuildItem.getResourceInterceptors()
                .getWriterInterceptors();
        writerInterceptors.getNameResourceInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));
        writerInterceptors.getGlobalResourceInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));
        PreMatchInterceptorContainer<ContainerRequestFilter> containerRequestFilters = resourceInterceptorsBuildItem
                .getResourceInterceptors().getContainerRequestFilters();
        containerRequestFilters.getPreMatchInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));
        containerRequestFilters.getNameResourceInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));
        containerRequestFilters.getGlobalResourceInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));
        InterceptorContainer<ContainerResponseFilter> containerResponseFilters = resourceInterceptorsBuildItem
                .getResourceInterceptors().getContainerResponseFilters();
        containerResponseFilters.getGlobalResourceInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));
        containerResponseFilters.getNameResourceInterceptors().forEach(i -> filtersAndInterceptors.add(i.getClassName()));

        annotationsTransformer.produce(new io.quarkus.arc.deployment.AnnotationsTransformerBuildItem(
                new io.quarkus.arc.processor.AnnotationsTransformer() {

                    @Override
                    public boolean appliesTo(AnnotationTarget.Kind kind) {
                        return kind == AnnotationTarget.Kind.CLASS;
                    }

                    @Override
                    public void transform(TransformationContext context) {
                        ClassInfo clazz = context.getTarget().asClass();
                        // check if the class is one of resources/sub-resources
                        if (allResources.contains(clazz.name())
                                && clazz.declaredAnnotation(ResteasyReactiveDotNames.TYPED) == null) {
                            context.transform().add(createTypedAnnotationInstance(clazz, beanArchiveIndexBuildItem)).done();
                            return;
                        }
                        // check if the class is one of providers, either explicitly declaring the annotation
                        // or discovered as resource interceptor or filter
                        if ((clazz.declaredAnnotation(ResteasyReactiveDotNames.PROVIDER) != null
                                || filtersAndInterceptors.contains(clazz.name().toString()))
                                && clazz.declaredAnnotation(ResteasyReactiveDotNames.TYPED) == null) {
                            // Add @Typed(MyResource.class)
                            context.transform().add(createTypedAnnotationInstance(clazz, beanArchiveIndexBuildItem)).done();
                            return;
                        }
                        // check if the class is a bean param
                        if (beanParams.contains(clazz.name().toString())
                                && clazz.declaredAnnotation(ResteasyReactiveDotNames.TYPED) == null) {
                            // Add @Typed(MyBean.class)
                            context.transform().add(createTypedAnnotationInstance(clazz, beanArchiveIndexBuildItem)).done();
                            return;
                        }
                    }
                }));
    }

    private AnnotationInstance createTypedAnnotationInstance(ClassInfo clazz,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        Set<DotName> interfaceNames = new HashSet<>();
        ClassInfo currentClazz = clazz;
        while (!ResteasyReactiveDotNames.OBJECT.equals(currentClazz.name())) {
            currentClazz.interfaceNames().forEach(iface -> interfaceNames.add(iface));
            // inspect super class
            currentClazz = beanArchiveIndexBuildItem.getIndex().getClassByName(currentClazz.superName());
        }
        Set<DotName> allInterfaces = new HashSet<>();
        recursiveInterfaceSearch(interfaceNames, allInterfaces, beanArchiveIndexBuildItem);
        AnnotationValue[] annotationValues = new AnnotationValue[allInterfaces.size() + 1];
        // always add the bean impl class
        annotationValues[0] = AnnotationValue.createClassValue("value",
                Type.create(clazz.name(), Type.Kind.CLASS));
        Iterator<DotName> iterator = allInterfaces.iterator();
        for (int i = 1; i < annotationValues.length; i++) {
            annotationValues[i] = AnnotationValue.createClassValue("value",
                    Type.create(iterator.next(), Type.Kind.CLASS));
        }
        return AnnotationInstance.create(ResteasyReactiveDotNames.TYPED, clazz,
                new AnnotationValue[] { AnnotationValue.createArrayValue("value",
                        annotationValues) });
    }

    private void recursiveInterfaceSearch(Set<DotName> interfacesToProcess, Set<DotName> allDiscoveredInterfaces,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        allDiscoveredInterfaces.addAll(interfacesToProcess);
        Set<DotName> additionalInterfacesToProcess = new HashSet<>();
        for (DotName name : interfacesToProcess) {
            ClassInfo clazz = beanArchiveIndexBuildItem.getIndex().getClassByName(name);
            if (clazz != null) {
                // get all interface that this interface extends
                additionalInterfacesToProcess.addAll(clazz.interfaceNames());
            }
        }
        if (!additionalInterfacesToProcess.isEmpty()) {
            // recursively process newly found interfaces
            recursiveInterfaceSearch(additionalInterfacesToProcess, allDiscoveredInterfaces, beanArchiveIndexBuildItem);
        }
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
            reflectiveClass
                    .produce(ReflectiveClassBuildItem.builder(dateTimeFormatterProviderClassNames.toArray(EMPTY_STRING_ARRAY))
                            .serialization(false).build());
        }
    }

    /**
     * RESTEasy Classic also includes the providers that are set in the 'META-INF/services/jakarta.ws.rs.ext.Providers' file
     * This is not a ServiceLoader call, but essentially provides the same functionality.
     */
    @BuildStep
    public void providersFromClasspath(BuildProducer<MessageBodyReaderBuildItem> messageBodyReaderProducer,
            BuildProducer<MessageBodyWriterBuildItem> messageBodyWriterProducer) {
        String fileName = "META-INF/services/" + Providers.class.getName();
        // we never want to include the Classic RESTEasy providers - these can end up on the classpath by using the Keycloak client for example
        Predicate<String> ignoredProviders = s -> s.startsWith("org.jboss.resteasy.plugins.providers");
        try {
            Set<String> detectedProviders = new HashSet<>(ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                    fileName));
            for (String providerClassName : detectedProviders) {
                if (ignoredProviders.test(providerClassName)) {
                    continue;
                }
                try {
                    Class<?> providerClass = Class.forName(providerClassName, false,
                            Thread.currentThread().getContextClassLoader());
                    if (MessageBodyReader.class.isAssignableFrom(providerClass)) {
                        String handledClassName = determineHandledGenericTypeOfProviderInterface(providerClass,
                                MessageBodyReader.class);
                        if (handledClassName == null) {
                            log.warn("Unable to determine which type MessageBodyReader '" + providerClass.getName()
                                    + "' handles so this Provider will be ignored");
                            continue;
                        }
                        MessageBodyReaderBuildItem.Builder builder = new MessageBodyReaderBuildItem.Builder(
                                providerClassName, handledClassName).setBuiltin(true);
                        Consumes consumes = providerClass.getAnnotation(Consumes.class);
                        if (consumes != null) {
                            builder.setMediaTypeStrings(Arrays.asList(consumes.value()));
                        } else {
                            builder.setMediaTypeStrings(Collections.singletonList(MediaType.WILDCARD_TYPE.toString()));
                        }
                        messageBodyReaderProducer.produce(builder.build()); // TODO: does it make sense to limit these to the Server?
                    }
                    if (MessageBodyWriter.class.isAssignableFrom(providerClass)) {
                        String handledClassName = determineHandledGenericTypeOfProviderInterface(providerClass,
                                MessageBodyWriter.class);
                        if (handledClassName == null) {
                            log.warn("Unable to determine which type MessageBodyWriter '" + providerClass.getName()
                                    + "' handles so this Provider will be ignored");
                            continue;
                        }
                        MessageBodyWriterBuildItem.Builder builder = new MessageBodyWriterBuildItem.Builder(
                                providerClassName, handledClassName).setBuiltin(true);
                        Produces produces = providerClass.getAnnotation(Produces.class);
                        if (produces != null) {
                            builder.setMediaTypeStrings(Arrays.asList(produces.value()));
                        } else {
                            builder.setMediaTypeStrings(Collections.singletonList(MediaType.WILDCARD_TYPE.toString()));
                        }
                        messageBodyWriterProducer.produce(builder.build()); // TODO: does it make sense to limit these to the Server?
                    }
                    // TODO: handle other providers as well
                } catch (ClassNotFoundException e) {
                    log.warn("Unable to load class '" + providerClassName
                            + "' when trying to determine what kind of JAX-RS Provider it is.", e);
                }
            }
        } catch (IOException e) {
            log.warn("Unable to properly detect and parse the contents of '" + fileName + "'", e);
        }
    }

    @Nullable
    private static String determineHandledGenericTypeOfProviderInterface(Class<?> providerClass,
            Class<?> targetProviderInterface) {

        java.lang.reflect.Type[] types = Types.findParameterizedTypes(providerClass, targetProviderInterface);
        if ((types == null) || (types.length != 1)) {
            return null;
        }
        try {
            return Types.getRawType(types[0]).getName();
        } catch (Exception ignored) {
            return null;
        }
    }

    @BuildStep
    public void fileHandling(BuildProducer<BuiltInReaderOverrideBuildItem> overrideProducer,
            BuildProducer<MessageBodyReaderBuildItem> readerProducer) {
        overrideProducer.produce(new BuiltInReaderOverrideBuildItem(ServerFileBodyHandler.class.getName(),
                QuarkusServerFileBodyHandler.class.getName()));
        readerProducer.produce(
                new MessageBodyReaderBuildItem(QuarkusServerPathBodyHandler.class.getName(), Path.class.getName(), List.of(
                        MediaType.WILDCARD), RuntimeType.SERVER, true, Priorities.USER));
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
            List<BuiltInReaderOverrideBuildItem> builtInReaderOverrideBuildItems,
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
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(builtinWriter.writerClass.getName())
                    .build());
        }
        Map<String, String> builtInReaderOverrides = BuiltInReaderOverrideBuildItem.toMap(builtInReaderOverrideBuildItems);
        for (Serialisers.BuiltinReader builtinReader : ServerSerialisers.BUILTIN_READERS) {
            String effectiveReaderClassName = builtinReader.readerClass.getName();
            if (builtInReaderOverrides.containsKey(effectiveReaderClassName)) {
                effectiveReaderClassName = builtInReaderOverrides.get(effectiveReaderClassName);
            }
            registerReader(recorder, serialisers, builtinReader.entityClass.getName(), effectiveReaderClassName,
                    beanContainerBuildItem.getValue(),
                    builtinReader.mediaType, builtinReader.constraint);
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(effectiveReaderClassName)
                    .build());
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
                    .builder(resourceClasses.stream().map(ResourceClass::getClassName).toArray(String[]::new))
                    .constructors(false).methods().build());
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, useIdentityComparisonForParameters = false)
    public void setupDeployment(BeanContainerBuildItem beanContainerBuildItem,
            Capabilities capabilities,
            ResteasyReactiveConfig config,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            ResteasyReactiveRecorder recorder,
            RecorderContext recorderContext,
            ShutdownContextBuildItem shutdownContext,
            HttpBuildTimeConfig vertxConfig,
            SetupEndpointsResultBuildItem setupEndpointsResult,
            ServerSerialisersBuildItem serverSerialisersBuildItem,
            List<PreExceptionMapperHandlerBuildItem> preExceptionMapperHandlerBuildItems,
            List<DynamicFeatureBuildItem> dynamicFeatures,
            List<JaxrsFeatureBuildItem> features,
            Optional<RequestContextFactoryBuildItem> requestContextFactoryBuildItem,
            BuildProducer<ResteasyReactiveDeploymentInfoBuildItem> quarkusRestDeploymentInfoBuildItemBuildProducer,
            BuildProducer<ResteasyReactiveDeploymentBuildItem> quarkusRestDeploymentBuildItemBuildProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<FilterBuildItem> filterBuildItemBuildProducer,
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
        contextResolvers.initializeDefaultFactories(factoryFunction);
        exceptionMapping.initializeDefaultFactories(factoryFunction);
        exceptionMapping.replaceDiscardAtRuntimeIfBeanIsUnavailable(className -> recorder.beanUnavailable(className));

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
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(readerClass).build());
        }

        for (AdditionalReaderWriter.Entry entry : additionalWriters.get()) {
            String writerClass = entry.getHandlerClass();
            registerWriter(recorder, serialisers, entry.getEntityClass(), writerClass,
                    beanContainerBuildItem.getValue(), entry.getMediaType());
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(writerClass).build());
        }

        BeanFactory<ResteasyReactiveInitialiser> initClassFactory = recorder.factory(QUARKUS_INIT_CLASS,
                beanContainerBuildItem.getValue());

        String applicationPath = determineApplicationPath(appResult, getAppPath(serverConfig.path()));
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
                .setClientProxyUnwrapper(recorder.clientProxyUnwrapper())
                .setApplicationSupplier(recorder.handleApplication(applicationClass, singletonClasses.isEmpty()))
                .setFactoryCreator(recorder.factoryCreator(beanContainerBuildItem.getValue()))
                .setDynamicFeatures(dynamicFeats)
                .setSerialisers(serialisers)
                .setPreExceptionMapperHandler(determinePreExceptionMapperHandler(preExceptionMapperHandlerBuildItems))
                .setApplicationPath(applicationPath)
                .setGlobalHandlerCustomizers(Collections.singletonList(new SecurityContextOverrideHandler.Customizer())) //TODO: should be pluggable
                .setResourceClasses(resourceClasses)
                .setDevelopmentMode(launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT)
                .setLocatableResourceClasses(subResourceClasses)
                .setParamConverterProviders(paramConverterProviders);
        quarkusRestDeploymentInfoBuildItemBuildProducer
                .produce(new ResteasyReactiveDeploymentInfoBuildItem(deploymentInfo));

        boolean servletPresent = false;
        int order = RouteConstants.ROUTE_ORDER_AFTER_DEFAULT + REST_ROUTE_ORDER_OFFSET;
        if (capabilities.isPresent("io.quarkus.servlet")) {
            //if servlet is present we run RR before the default route
            //otherwise we run after it
            order = RouteConstants.ROUTE_ORDER_BEFORE_DEFAULT + REST_ROUTE_ORDER_OFFSET;
            servletPresent = true;
        }

        RuntimeValue<Deployment> deployment = recorder.createDeployment(deploymentInfo,
                beanContainerBuildItem.getValue(), shutdownContext, vertxConfig,
                requestContextFactoryBuildItem.map(RequestContextFactoryBuildItem::getFactory).orElse(null),
                initClassFactory, launchModeBuildItem.getLaunchMode(), servletPresent || !resumeOn404Items.isEmpty());

        quarkusRestDeploymentBuildItemBuildProducer
                .produce(new ResteasyReactiveDeploymentBuildItem(deployment, deploymentPath));

        if (!requestContextFactoryBuildItem.isPresent()) {
            RuntimeValue<RestInitialHandler> restInitialHandler = recorder.restInitialHandler(deployment);
            Handler<RoutingContext> handler = recorder.handler(restInitialHandler);

            final boolean noCustomAuthCompletionExMapper;
            final boolean noCustomAuthFailureExMapper;
            final boolean noCustomAuthRedirectExMapper;
            if (vertxConfig.auth.proactive) {
                noCustomAuthCompletionExMapper = notFoundCustomExMapper(AuthenticationCompletionException.class.getName(),
                        AuthenticationCompletionExceptionMapper.class.getName(), exceptionMapping);
                noCustomAuthFailureExMapper = notFoundCustomExMapper(AuthenticationFailedException.class.getName(),
                        AuthenticationFailedExceptionMapper.class.getName(), exceptionMapping);
                noCustomAuthRedirectExMapper = notFoundCustomExMapper(AuthenticationRedirectException.class.getName(),
                        AuthenticationRedirectExceptionMapper.class.getName(), exceptionMapping);
            } else {
                // with disabled proactive auth we need to handle exceptions anyway as default auth failure handler did not
                noCustomAuthCompletionExMapper = false;
                noCustomAuthFailureExMapper = false;
                noCustomAuthRedirectExMapper = false;
            }

            Handler<RoutingContext> failureHandler = recorder.failureHandler(restInitialHandler, noCustomAuthCompletionExMapper,
                    noCustomAuthFailureExMapper, noCustomAuthRedirectExMapper, vertxConfig.auth.proactive);

            // we add failure handler right before QuarkusErrorHandler
            // so that user can define failure handlers that precede exception mappers
            filterBuildItemBuildProducer.produce(FilterBuildItem.ofAuthenticationFailureHandler(failureHandler));

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

    private ServerRestHandler determinePreExceptionMapperHandler(
            List<PreExceptionMapperHandlerBuildItem> preExceptionMapperHandlerBuildItems) {
        if ((preExceptionMapperHandlerBuildItems == null) || preExceptionMapperHandlerBuildItems.isEmpty()) {
            return null;
        }
        if (preExceptionMapperHandlerBuildItems.size() == 1) {
            return preExceptionMapperHandlerBuildItems.get(0).getHandler();
        }
        Collections.sort(preExceptionMapperHandlerBuildItems);
        return new DelegatingServerRestHandler(preExceptionMapperHandlerBuildItems.stream()
                .map(PreExceptionMapperHandlerBuildItem::getHandler).collect(toList()));
    }

    private static boolean notFoundCustomExMapper(String builtInExSignature, String builtInMapperSignature,
            ExceptionMapping exceptionMapping) {
        for (var entry : exceptionMapping.getMappers().entrySet()) {
            if (builtInExSignature.equals(entry.getKey())
                    && !entry.getValue().getClassName().startsWith(builtInMapperSignature)) {
                return false;
            }
        }
        for (var entry : exceptionMapping.getRuntimeCheckMappers().entrySet()) {
            if (builtInExSignature.equals(entry.getKey())) {
                for (var resourceExceptionMapper : entry.getValue()) {
                    if (!resourceExceptionMapper.getClassName().startsWith(builtInMapperSignature)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    public FilterBuildItem addDefaultAuthFailureHandler(ResteasyReactiveRecorder recorder) {
        // replace default auth failure handler added by vertx-http so that our exception mappers can customize response
        return new FilterBuildItem(recorder.defaultAuthFailureHandler(), FilterBuildItem.AUTHENTICATION - 1);
    }

    private void checkForDuplicateEndpoint(ResteasyReactiveConfig config, Map<String, List<EndpointConfig>> allMethods) {
        String message = allMethods.values().stream()
                .map(this::getDuplicateEndpointMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
        if (message.length() > 0) {
            if (config.failOnDuplicate()) {
                throw new DeploymentException(message);
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
                getEffectivePropertyValue("input-buffer-size", config.inputBufferSize().asLongValue(), Long.class, mpConfig),
                getEffectivePropertyValue("min-chunk-size", config.outputBufferSize(), Integer.class, mpConfig),
                getEffectivePropertyValue("output-buffer-size", config.outputBufferSize(), Integer.class, mpConfig),
                getEffectivePropertyValue("single-default-produces", config.singleDefaultProduces(), Boolean.class, mpConfig),
                getEffectivePropertyValue("default-produces", config.defaultProduces(), Boolean.class, mpConfig));
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
    public void runtimeConfiguration(ResteasyReactiveRuntimeRecorder recorder,
            Optional<ResteasyReactiveDeploymentBuildItem> deployment,
            ResteasyReactiveServerRuntimeConfig resteasyReactiveServerRuntimeConf,
            BuildProducer<HandlerConfigurationProviderBuildItem> producer) {
        if (deployment.isEmpty()) {
            return;
        }
        producer.produce(new HandlerConfigurationProviderBuildItem(RuntimeConfiguration.class,
                recorder.runtimeConfiguration(deployment.get().getDeployment(), resteasyReactiveServerRuntimeConf)));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void configureHandlers(ResteasyReactiveRuntimeRecorder recorder,
            Optional<ResteasyReactiveDeploymentBuildItem> deployment,
            List<HandlerConfigurationProviderBuildItem> items) {
        if (deployment.isEmpty()) {
            return;
        }

        Map<Class<?>, Supplier<?>> runtimeConfigMap = new HashMap<>();
        for (HandlerConfigurationProviderBuildItem item : items) {
            runtimeConfigMap.put(item.getConfigClass(), item.getValueSupplier());
        }

        recorder.configureHandlers(deployment.get().getDeployment(), runtimeConfigMap);
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
            HttpBuildTimeConfig httpBuildTimeConfig, Optional<EagerSecurityInterceptorBuildItem> eagerSecurityInterceptors) {
        if (!capabilities.isPresent(Capability.SECURITY)) {
            return null;
        }

        final boolean applySecurityInterceptors = eagerSecurityInterceptors.isPresent();
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
                if (applySecurityInterceptors && eagerSecurityInterceptors.get().applyInterceptorOn(method)) {
                    List<HandlerChainCustomizer> nextSecurityHandlerList = new ArrayList<>();
                    nextSecurityHandlerList.add(EagerSecurityInterceptorHandler.Customizer.newInstance());

                    // EagerSecurityInterceptorHandler must be run before EagerSecurityHandler
                    if (securityHandlerList != null) {
                        nextSecurityHandlerList.addAll(securityHandlerList);
                    }

                    securityHandlerList = List.copyOf(nextSecurityHandlerList);
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
                    StandardSecurityCheckInterceptor.PermitAllInterceptor.class,
                    StandardSecurityCheckInterceptor.PermissionsAllowedInterceptor.class));
            beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityEventContext.class));
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
                .declaredAnnotation(ResteasyReactiveDotNames.APPLICATION_PATH);
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
