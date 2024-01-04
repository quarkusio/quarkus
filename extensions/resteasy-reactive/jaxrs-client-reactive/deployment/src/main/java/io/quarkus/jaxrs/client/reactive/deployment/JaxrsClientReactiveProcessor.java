package io.quarkus.jaxrs.client.reactive.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static org.jboss.jandex.Type.Kind.ARRAY;
import static org.jboss.jandex.Type.Kind.CLASS;
import static org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE;
import static org.jboss.jandex.Type.Kind.PRIMITIVE;
import static org.jboss.resteasy.reactive.client.impl.RestClientRequestContext.DEFAULT_CONTENT_TYPE_PROP;
import static org.jboss.resteasy.reactive.common.processor.EndpointIndexer.extractProducesConsumesValues;
import static org.jboss.resteasy.reactive.common.processor.JandexUtil.*;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COLLECTION;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONSUMES;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.ENCODED;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MAP;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OBJECT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PART_TYPE_NAME;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_MULTI;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.CompletionStageRxInvoker;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.sse.SseEventSource;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.jboss.resteasy.reactive.client.handlers.ClientObservabilityHandler;
import org.jboss.resteasy.reactive.client.impl.AbstractRxInvoker;
import org.jboss.resteasy.reactive.client.impl.AsyncInvokerImpl;
import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.MultiInvoker;
import org.jboss.resteasy.reactive.client.impl.SseEventSourceBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.StorkClientRequestFilter;
import org.jboss.resteasy.reactive.client.impl.UniInvoker;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartForm;
import org.jboss.resteasy.reactive.client.processor.beanparam.BeanParamItem;
import org.jboss.resteasy.reactive.client.processor.beanparam.ClientBeanParamInfo;
import org.jboss.resteasy.reactive.client.processor.beanparam.CookieParamItem;
import org.jboss.resteasy.reactive.client.processor.beanparam.FormParamItem;
import org.jboss.resteasy.reactive.client.processor.beanparam.HeaderParamItem;
import org.jboss.resteasy.reactive.client.processor.beanparam.Item;
import org.jboss.resteasy.reactive.client.processor.beanparam.PathParamItem;
import org.jboss.resteasy.reactive.client.processor.beanparam.QueryParamItem;
import org.jboss.resteasy.reactive.client.processor.scanning.ClientEndpointIndexer;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.client.spi.FieldFiller;
import org.jboss.resteasy.reactive.client.spi.MultipartResponseData;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.ResponseBuilderFactory;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.UriBuilderImpl;
import org.jboss.resteasy.reactive.common.model.MaybeRestClientInterface;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.common.model.RestClientInterface;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaderWriter;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.HashUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.multipart.FileDownload;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.arc.processor.Types;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.jaxrs.client.reactive.runtime.ClientResponseBuilderFactory;
import io.quarkus.jaxrs.client.reactive.runtime.JaxrsClientReactiveRecorder;
import io.quarkus.jaxrs.client.reactive.runtime.ParameterDescriptorFromClassSupplier;
import io.quarkus.jaxrs.client.reactive.runtime.RestClientBase;
import io.quarkus.jaxrs.client.reactive.runtime.ToObjectArray;
import io.quarkus.jaxrs.client.reactive.runtime.impl.MultipartResponseDataBase;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ParameterContainersBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusFactoryCreator;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.SerializersUtil;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterOverrideBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;

public class JaxrsClientReactiveProcessor {

    private static final String MULTI_BYTE_SIGNATURE = "L" + Multi.class.getName().replace('.', '/') + "<Ljava/lang/Byte;>;";
    private static final String FILE_SIGNATURE = "L" + File.class.getName().replace('.', '/') + ";";
    private static final String PATH_SIGNATURE = "L" + java.nio.file.Path.class.getName().replace('.', '/') + ";";
    private static final String BUFFER_SIGNATURE = "L" + Buffer.class.getName().replace('.', '/') + ";";
    private static final String BYTE_ARRAY_SIGNATURE = "[B";

    private static final Logger log = Logger.getLogger(JaxrsClientReactiveProcessor.class);

    private static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("//+");

    private static final MethodDescriptor WEB_TARGET_RESOLVE_TEMPLATE_METHOD = MethodDescriptor.ofMethod(WebTarget.class,
            "resolveTemplate",
            WebTarget.class,
            String.class, Object.class);
    private static final MethodDescriptor MULTIVALUED_MAP_ADD = MethodDescriptor.ofMethod(MultivaluedMap.class, "add",
            void.class, Object.class, Object.class);
    private static final MethodDescriptor PATH_GET_FILENAME = MethodDescriptor.ofMethod(Path.class, "getFileName",
            Path.class);
    private static final MethodDescriptor OBJECT_TO_STRING = MethodDescriptor.ofMethod(Object.class, "toString", String.class);

    static final DotName CONTINUATION = DotName.createSimple("kotlin.coroutines.Continuation");
    private static final DotName UNI_KT = DotName.createSimple("io.smallrye.mutiny.coroutines.UniKt");
    private static final DotName FILE = DotName.createSimple(File.class.getName());
    private static final DotName PATH = DotName.createSimple(Path.class.getName());
    private static final DotName BUFFER = DotName.createSimple(Buffer.class.getName());

    private static final DotName NOT_BODY = DotName.createSimple("io.quarkus.rest.client.reactive.NotBody");

    private static final Set<DotName> ASYNC_RETURN_TYPES = Set.of(COMPLETION_STAGE, UNI, MULTI, REST_MULTI);
    public static final DotName BYTE = DotName.createSimple(Byte.class.getName());
    public static final MethodDescriptor MULTIPART_RESPONSE_DATA_ADD_FILLER = MethodDescriptor
            .ofMethod(MultipartResponseDataBase.class, "addFiller", void.class, FieldFiller.class);

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    @BuildStep
    void registerClientResponseBuilder(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        serviceProviders.produce(new ServiceProviderBuildItem(ResponseBuilderFactory.class.getName(),
                ClientResponseBuilderFactory.class.getName()));

        serviceProviders.produce(new ServiceProviderBuildItem(ClientBuilder.class.getName(),
                ClientBuilderImpl.class.getName()));

        serviceProviders.produce(new ServiceProviderBuildItem(SseEventSource.Builder.class.getName(),
                SseEventSourceBuilderImpl.class.getName()));
    }

    @BuildStep
    void initializeRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(AsyncInvokerImpl.class.getName()));
    }

    @BuildStep
    void initializeStorkFilter(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(StorkClientRequestFilter.class));
        additionalIndexedClassesBuildItem
                .produce(new AdditionalIndexedClassesBuildItem(StorkClientRequestFilter.class.getName()));
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(StorkClientRequestFilter.class).methods().fields().build());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupClientProxies(JaxrsClientReactiveRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyBuildItemBuildProducer,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            List<MessageBodyReaderOverrideBuildItem> messageBodyReaderOverrideBuildItems,
            List<MessageBodyWriterOverrideBuildItem> messageBodyWriterOverrideBuildItems,
            List<JaxrsClientReactiveEnricherBuildItem> enricherBuildItems,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            ApplicationIndexBuildItem applicationIndexBuildItem,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            Capabilities capabilities, Optional<MetricsCapabilityBuildItem> metricsCapability,
            ResteasyReactiveConfig config,
            RecorderContext recorderContext,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer,
            List<RestClientDefaultProducesBuildItem> defaultConsumes,
            List<RestClientDefaultConsumesBuildItem> defaultProduces,
            List<RestClientDisableSmartDefaultProduces> disableSmartDefaultProduces,
            List<ParameterContainersBuildItem> parameterContainersBuildItems) {
        String defaultConsumesType = defaultMediaType(defaultConsumes, MediaType.APPLICATION_OCTET_STREAM);
        String defaultProducesType = defaultMediaType(defaultProduces, MediaType.TEXT_PLAIN);

        Serialisers serialisers = recorder.createSerializers();

        SerializersUtil.setupSerializers(recorder, reflectiveClassBuildItemBuildProducer, messageBodyReaderBuildItems,
                messageBodyWriterBuildItems, messageBodyReaderOverrideBuildItems, messageBodyWriterOverrideBuildItems,
                beanContainerBuildItem, applicationResultBuildItem, serialisers,
                RuntimeType.CLIENT);
        Set<DotName> scannedParameterContainers = new HashSet<>();

        for (ParameterContainersBuildItem parameterContainersBuildItem : parameterContainersBuildItems) {
            scannedParameterContainers.addAll(parameterContainersBuildItem.getClassNames());
        }
        reflectiveClassBuildItemBuildProducer.produce(ReflectiveClassBuildItem
                .builder(scannedParameterContainers.stream().map(DotName::toString).distinct().toArray(String[]::new))
                .methods().fields().build());

        if (resourceScanningResultBuildItem.isEmpty()
                || resourceScanningResultBuildItem.get().getResult().getClientInterfaces().isEmpty()) {
            recorder.setupClientProxies(new HashMap<>(), Collections.emptyMap());
            return;
        }
        ResourceScanningResult result = resourceScanningResultBuildItem.get().getResult();

        AdditionalReaders additionalReaders = new AdditionalReaders();
        AdditionalWriters additionalWriters = new AdditionalWriters();

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        ClientEndpointIndexer clientEndpointIndexer = new QuarkusClientEndpointIndexer.Builder(capabilities)
                .setIndex(index)
                .setApplicationIndex(applicationIndexBuildItem.getIndex())
                .setExistingConverters(new HashMap<>())
                .addParameterContainerTypes(scannedParameterContainers)
                .setScannedResourcePaths(result.getScannedResourcePaths())
                .setConfig(createRestReactiveConfig(config))
                .setAdditionalReaders(additionalReaders)
                .setHttpAnnotationToMethod(result.getHttpAnnotationToMethod())
                .setInjectableBeans(new HashMap<>())
                .setFactoryCreator(new QuarkusFactoryCreator(recorder, beanContainerBuildItem.getValue()))
                .setAdditionalWriters(additionalWriters)
                .setDefaultBlocking(applicationResultBuildItem.getResult().getBlockingDefault())
                .setHasRuntimeConverters(false)
                .setDefaultProduces(defaultProducesType)
                .setSmartDefaultProduces(disableSmartDefaultProduces.isEmpty())
                .setSkipMethodParameter(new Predicate<Map<DotName, AnnotationInstance>>() {
                    @Override
                    public boolean test(Map<DotName, AnnotationInstance> anns) {
                        return anns.containsKey(NOT_BODY);
                    }
                })
                .setResourceMethodCallback(new Consumer<>() {
                    @Override
                    public void accept(EndpointIndexer.ResourceMethodCallbackEntry entry) {
                        MethodInfo method = entry.getMethodInfo();
                        String source = JaxrsClientReactiveProcessor.class.getSimpleName() + " > " + method.declaringClass()
                                + "[" + method + "]";

                        reflectiveHierarchyBuildItemBuildProducer.produce(new ReflectiveHierarchyBuildItem.Builder()
                                .type(method.returnType())
                                .index(index)
                                .ignoreTypePredicate(QuarkusResteasyReactiveDotNames.IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                                .ignoreFieldPredicate(QuarkusResteasyReactiveDotNames.IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                                .ignoreMethodPredicate(
                                        QuarkusResteasyReactiveDotNames.IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                                .source(source)
                                .build());

                        // if there is a body parameter type, register it for reflection
                        ResourceMethod resourceMethod = entry.getResourceMethod();
                        MethodParameter[] methodParameters = resourceMethod.getParameters();
                        if (methodParameters != null) {
                            for (int i = 0; i < methodParameters.length; i++) {
                                MethodParameter methodParameter = methodParameters[i];
                                if (methodParameter.getParameterType() == ParameterType.BODY) {
                                    reflectiveHierarchyBuildItemBuildProducer.produce(new ReflectiveHierarchyBuildItem.Builder()
                                            .type(method.parameterType(i))
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
                            }
                        }
                    }
                })
                .build();

        boolean observabilityIntegrationNeeded = (capabilities.isPresent(Capability.OPENTELEMETRY_TRACER) ||
                (metricsCapability.isPresent()
                        && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)));

        Map<String, RuntimeValue<BiFunction<WebTarget, List<ParamConverterProvider>, ?>>> clientImplementations = new HashMap<>();
        Map<String, String> failures = new HashMap<>();

        Set<ClassInfo> multipartResponseTypes = new HashSet<>();
        // collect classes annotated with MultipartForm and add classes that are used in rest client interfaces as return
        // types for multipart responses
        for (AnnotationInstance annotation : index.getAnnotations(ResteasyReactiveDotNames.MULTI_PART_FORM_PARAM)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                multipartResponseTypes.add(annotation.target().asClass());
            }
        }

        for (Map.Entry<DotName, String> i : result.getClientInterfaces().entrySet()) {
            ClassInfo clazz = index.getClassByName(i.getKey());
            //these interfaces can also be clients
            //so we generate client proxies for them
            MaybeRestClientInterface maybeClientProxy = clientEndpointIndexer.createClientProxy(clazz,
                    i.getValue());
            if (maybeClientProxy.exists()) {
                RestClientInterface clientProxy = maybeClientProxy.getRestClientInterface();
                try {
                    RuntimeValue<BiFunction<WebTarget, List<ParamConverterProvider>, ?>> proxyProvider = generateClientInvoker(
                            recorderContext, clientProxy,
                            enricherBuildItems, generatedClassBuildItemBuildProducer, clazz, index, defaultConsumesType,
                            result.getHttpAnnotationToMethod(), observabilityIntegrationNeeded, multipartResponseTypes);
                    if (proxyProvider != null) {
                        clientImplementations.put(clientProxy.getClassName(), proxyProvider);
                    }
                } catch (Exception any) {
                    log.debugv(any, "Failed to create client proxy for {0} this can usually be safely ignored", clazz.name());
                    failures.put(clazz.name().toString(), any.getMessage());
                }

            } else {
                failures.put(clazz.name().toString(), maybeClientProxy.getFailure());
            }
        }

        recorder.setupClientProxies(clientImplementations, failures);

        for (AdditionalReaderWriter.Entry additionalReader : additionalReaders.get()) {
            String readerClass = additionalReader.getHandlerClass();
            ResourceReader reader = new ResourceReader();
            reader.setBuiltin(true);
            reader.setFactory(recorder.factory(readerClass, beanContainerBuildItem.getValue()));
            reader.setMediaTypeStrings(Collections.singletonList(additionalReader.getMediaType()));
            recorder.registerReader(serialisers, additionalReader.getEntityClass(), reader);
            reflectiveClassBuildItemBuildProducer
                    .produce(ReflectiveClassBuildItem.builder(readerClass)
                            .build());
        }

        for (AdditionalReaderWriter.Entry entry : additionalWriters.get()) {
            String writerClass = entry.getHandlerClass();
            ResourceWriter writer = new ResourceWriter();
            writer.setBuiltin(true);
            writer.setFactory(recorder.factory(writerClass, beanContainerBuildItem.getValue()));
            writer.setMediaTypeStrings(Collections.singletonList(entry.getMediaType()));
            recorder.registerWriter(serialisers, entry.getEntityClass(), writer);
            reflectiveClassBuildItemBuildProducer
                    .produce(ReflectiveClassBuildItem.builder(writerClass)
                            .build());
        }

        Map<String, RuntimeValue<MultipartResponseData>> responsesData = new HashMap<>();
        for (ClassInfo multipartResponseType : multipartResponseTypes) {
            responsesData.put(multipartResponseType.toString(), createMultipartResponseData(multipartResponseType,
                    generatedClassBuildItemBuildProducer, recorderContext));
        }
        recorder.setMultipartResponsesData(responsesData);
    }

    /**
     * Scan `multipartResponseTypeInfo` for fields and setters/getters annotated with @PartType and prepares
     * {@link MultipartResponseData} class for it. This class is later used to create a new instance of the response type
     * and to provide {@link FieldFiller field fillers} that are responsible for setting values for each of the fields and
     * setters
     *
     * @param multipartResponseTypeInfo a class to scan
     * @param generatedClasses build producer for generating classes
     * @param context recorder context to instantiate the newly created class
     * @return a runtime value with an instance of the MultipartResponseData corresponding to the given class
     */
    private RuntimeValue<MultipartResponseData> createMultipartResponseData(ClassInfo multipartResponseTypeInfo,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, RecorderContext context) {
        String multipartResponseType = multipartResponseTypeInfo.toString();
        String dataClassName = multipartResponseType + "$$MultipartData";
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClasses, true),
                dataClassName, null, MultipartResponseDataBase.class.getName())) {

            // implement {@link org.jboss.resteasy.reactive.client.spi.MultipartResponseData#newInstance}
            // method that returns a new instance of the response type
            MethodCreator newInstance = c.getMethodCreator("newInstance", Object.class);
            newInstance.returnValue(
                    newInstance.newInstance(MethodDescriptor.ofConstructor(multipartResponseType)));

            // scan for public fields and public setters annotated with @PartType.
            // initialize appropriate collections of FieldFillers in the constructor

            MethodCreator constructor = c.getMethodCreator(MethodDescriptor.ofConstructor(multipartResponseType));
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(MultipartResponseDataBase.class),
                    constructor.getThis());

            Map<String, AnnotationInstance> nonPublicPartTypeFields = new HashMap<>();
            // 1. public fields
            for (FieldInfo field : multipartResponseTypeInfo.fields()) {
                AnnotationInstance partType = field.annotation(ResteasyReactiveDotNames.PART_TYPE_NAME);
                if (partType == null) {
                    log.debugf("Skipping field %s.%s from multipart mapping because it is not annotated with " +
                            "@PartType", multipartResponseType, field.name());
                } else if (!Modifier.isPublic(field.flags())) {
                    // the field is not public, let's memorize its name in case it has a getter
                    nonPublicPartTypeFields.put(field.name(), partType);
                } else {
                    String partName = extractPartName(partType.target(), field.name());
                    String fillerName = createFieldFillerForField(partType, field, partName, generatedClasses, dataClassName);
                    constructor.invokeVirtualMethod(MULTIPART_RESPONSE_DATA_ADD_FILLER, constructor.getThis(),
                            constructor.newInstance(MethodDescriptor.ofConstructor(fillerName)));
                }
            }
            for (MethodInfo method : multipartResponseTypeInfo.methods()) {
                String methodName = method.name();
                if (methodName.startsWith("set") && method.parametersCount() == 1) {
                    AnnotationInstance partType;
                    String fieldName = setterToFieldName(methodName);
                    if ((partType = partTypeFromGetterOrSetter(method)) != null
                            || (partType = nonPublicPartTypeFields.get(fieldName)) != null) {
                        String partName = extractPartName(partType.target(), fieldName);
                        String fillerName = createFieldFillerForSetter(partType, method, partName, generatedClasses,
                                dataClassName);
                        constructor.invokeVirtualMethod(MULTIPART_RESPONSE_DATA_ADD_FILLER, constructor.getThis(),
                                constructor.newInstance(MethodDescriptor.ofConstructor(fillerName)));
                    } else {
                        log.debugf("Ignoring possible setter " + methodName + ", no part type annotation found");
                    }
                }
            }
            constructor.returnValue(null);
        }
        return context.newInstance(dataClassName);
    }

    private String extractPartName(AnnotationTarget target, String fieldName) {
        AnnotationInstance restForm;
        AnnotationInstance formParam;
        switch (target.kind()) {
            case FIELD:
                restForm = target.asField().annotation(REST_FORM_PARAM);
                formParam = target.asField().annotation(FORM_PARAM);
                break;
            case METHOD:
                restForm = target.asMethod().annotation(REST_FORM_PARAM);
                formParam = target.asMethod().annotation(FORM_PARAM);
                break;
            default:
                throw new IllegalArgumentException(
                        "PartType annotation is only supported on fields and (setter/getter) methods for multipart responses, found one on "
                                + target);
        }
        return getAnnotationValueOrDefault(fieldName, restForm, formParam);
    }

    private String getAnnotationValueOrDefault(String fieldName, AnnotationInstance restForm, AnnotationInstance formParam) {
        if (restForm != null) {
            AnnotationValue restFormValue = restForm.value();
            return restFormValue == null ? fieldName : restFormValue.asString();
        } else if (formParam != null) {
            return formParam.value().asString();
        } else {
            return fieldName;
        }
    }

    private String createFieldFillerForSetter(AnnotationInstance partType, MethodInfo setter, String partName,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, String dataClassName) {
        String fillerClassName = dataClassName + "$$" + setter.name();
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClasses, true),
                fillerClassName, null, FieldFiller.class.getName())) {
            Type parameter = setter.parameterType(0);
            createFieldFillerConstructor(partType, parameter, partName, fillerClassName, c);

            MethodCreator set = c
                    .getMethodCreator(
                            MethodDescriptor.ofMethod(fillerClassName, "set", void.class, Object.class, Object.class));

            ResultHandle value = set.getMethodParam(1);
            value = performValueConversion(parameter, set, value);

            set.invokeVirtualMethod(setter, set.getMethodParam(0), value);

            set.returnValue(null);
        }
        return fillerClassName;
    }

    private ResultHandle performValueConversion(Type parameter, MethodCreator set, ResultHandle value) {
        if (parameter.kind() == CLASS) {
            if (parameter.asClassType().name().equals(FILE)) {
                // we should get a FileDownload type, let's convert it to File
                value = set.invokeStaticMethod(MethodDescriptor.ofMethod(FieldFiller.class, "fileDownloadToFile",
                        File.class, FileDownload.class), value);
            } else if (parameter.asClassType().name().equals(PATH)) {
                // we should get a FileDownload type, let's convert it to Path
                value = set.invokeStaticMethod(MethodDescriptor.ofMethod(FieldFiller.class, "fileDownloadToPath",
                        Path.class, FileDownload.class), value);
            }
        }
        return value;
    }

    private String createFieldFillerForField(AnnotationInstance partType, FieldInfo field, String partName,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, String dataClassName) {
        String fillerClassName = dataClassName + "$$" + field.name();
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClasses, true),
                fillerClassName, null, FieldFiller.class.getName())) {
            createFieldFillerConstructor(partType, field.type(), partName, fillerClassName, c);

            MethodCreator set = c
                    .getMethodCreator(
                            MethodDescriptor.ofMethod(fillerClassName, "set", void.class, Object.class, Object.class));

            ResultHandle value = set.getMethodParam(1);
            value = performValueConversion(field.type(), set, value);
            set.writeInstanceField(field, set.getMethodParam(0), value);

            set.returnValue(null);
        }
        return fillerClassName;
    }

    private void createFieldFillerConstructor(AnnotationInstance partType, Type type, String partName,
            String fillerClassName, ClassCreator c) {
        MethodCreator ctor = c.getMethodCreator(MethodDescriptor.ofConstructor(fillerClassName));

        ResultHandle genericType;
        if (type.kind() == PARAMETERIZED_TYPE) {
            genericType = createGenericTypeFromParameterizedType(ctor, type.asParameterizedType());
        } else if (type.kind() == CLASS) {
            genericType = ctor.newInstance(
                    MethodDescriptor.ofConstructor(GenericType.class, java.lang.reflect.Type.class),
                    ctor.loadClassFromTCCL(type.asClassType().name().toString()));
        } else if (type.kind() == ARRAY) {
            genericType = ctor.newInstance(
                    MethodDescriptor.ofConstructor(GenericType.class, java.lang.reflect.Type.class),
                    ctor.loadClassFromTCCL(type.asArrayType().name().toString()));
        } else if (type.kind() == PRIMITIVE) {
            throw new IllegalArgumentException("Primitive types are not supported for multipart response mapping. " +
                    "Please use a wrapper class instead");
        } else {
            throw new IllegalArgumentException("Unsupported field type for multipart response mapping: " +
                    type + ". Only classes, arrays and parameterized types are supported");
        }

        ctor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(FieldFiller.class, GenericType.class, String.class, String.class),
                ctor.getThis(), genericType, ctor.load(partName), ctor.load(partType.value().asString()));
        ctor.returnValue(null);
    }

    private AnnotationInstance partTypeFromGetterOrSetter(MethodInfo setter) {
        AnnotationInstance partTypeAnno = setter.annotation(PART_TYPE_NAME);
        if (partTypeAnno != null) {
            return partTypeAnno;
        }

        String getterName = setter.name().replaceFirst("s", "g");
        MethodInfo getter = setter.declaringClass().method(getterName);
        if (getter != null && null != (partTypeAnno = getter.annotation(PART_TYPE_NAME))) {
            return partTypeAnno;
        }

        return null;
    }

    private String setterToFieldName(String methodName) {
        if (methodName.length() <= 3) {
            return "";
        } else {
            char[] nameArray = methodName.toCharArray();
            nameArray[3] = Character.toLowerCase(nameArray[3]);
            return new String(nameArray, 3, nameArray.length - 3);
        }
    }

    private org.jboss.resteasy.reactive.common.ResteasyReactiveConfig createRestReactiveConfig(ResteasyReactiveConfig config) {
        Config mpConfig = ConfigProvider.getConfig();

        return new org.jboss.resteasy.reactive.common.ResteasyReactiveConfig(
                getEffectivePropertyValue("input-buffer-size", config.inputBufferSize().asLongValue(), Long.class, mpConfig),
                getEffectivePropertyValue("min-chunk-size", config.minChunkSize(), Integer.class, mpConfig),
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

    private String defaultMediaType(List<? extends MediaTypeWithPriority> defaultMediaTypes, String defaultMediaType) {
        if (defaultMediaTypes == null || defaultMediaTypes.isEmpty()) {
            return defaultMediaType;
        }
        defaultMediaTypes.sort(Comparator.comparingInt(MediaTypeWithPriority::getPriority));
        return defaultMediaTypes.get(0).getMediaType();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerInvocationCallbacks(CombinedIndexBuildItem index, JaxrsClientReactiveRecorder recorder) {

        Collection<ClassInfo> invocationCallbacks = index.getComputingIndex()
                .getAllKnownImplementors(ResteasyReactiveDotNames.INVOCATION_CALLBACK);

        GenericTypeMapping genericTypeMapping = new GenericTypeMapping();
        for (ClassInfo invocationCallback : invocationCallbacks) {
            try {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(invocationCallback.name(),
                        ResteasyReactiveDotNames.INVOCATION_CALLBACK, index.getComputingIndex());
                recorder.registerInvocationHandlerGenericType(genericTypeMapping, invocationCallback.name().toString(),
                        typeParameters.get(0).name().toString());
            } catch (Exception ignored) {

            }
        }
        recorder.setGenericTypeMapping(genericTypeMapping);
    }

    /*
       @formatter:off
       Generates client stub, e.g. for the following interface:
       ```
       public interface BaseClient {
          @GET
          @Path("/base")
          Response executeBaseGet();

          @POST
          @Path("/base")
          Response executeBasePost();
       }
       ```
       Generates the following (with MicroProfile enricher):
       ```
      public class BaseClient$$QuarkusRestClientInterface implements Closeable, BaseClient {
         final WebTarget target1;
         private final Method javaMethod1;
         private final HeaderFiller headerFiller1;
         final WebTarget target2;
         private final Method javaMethod2;
         private final HeaderFiller headerFiller2;

         public BaseClient$$QuarkusRestClientInterface(WebTarget var1) {
            WebTarget var3 = var1.path("");
            DefaultClientHeadersFactoryImpl var2 = new DefaultClientHeadersFactoryImpl();
            MicroProfileRestClientRequestFilter var4 = new MicroProfileRestClientRequestFilter((ClientHeadersFactory)var2);
            var3 = (WebTarget)((Configurable)var3).register(var4);
            String var6 = "/base";
            WebTarget var5 = var3.path(var6);
            this.target1 = var5;
            Class[] var7 = new Class[0];
            Method var8 = BaseClient.class.getMethod("executeBasePost", var7);
            this.javaMethod1 = var8;
            NoOpHeaderFiller var9 = NoOpHeaderFiller.INSTANCE;
            this.headerFiller1 = (HeaderFiller)var9;
            String var11 = "/base";
            WebTarget var10 = var3.path(var11);
            this.target2 = var10;
            Class[] var12 = new Class[0];
            Method var13 = BaseClient.class.getMethod("executeBaseGet", var12);
            this.javaMethod2 = var13;
            NoOpHeaderFiller var14 = NoOpHeaderFiller.INSTANCE;
            this.headerFiller2 = (HeaderFiller)var14;
         }

         public Response executeBasePost() {
            WebTarget var1 = this.target1;
            String[] var2 = new String[]{"application/json"};
            Builder var3 = var1.request(var2);
            Method var4 = this.javaMethod1;
            var3 = var3.property("org.eclipse.microprofile.rest.client.invokedMethod", var4);
            HeaderFiller var5 = this.headerFiller1;
            var3 = var3.property("io.quarkus.resteasy.reactive.client.microprofile.HeaderFiller", var5);

            try {
               return (Response)var3.method("POST", Response.class);
            } catch (ProcessingException var8) {
               Throwable var7 = ((Throwable)var8).getCause();
               if (!(var7 instanceof WebApplicationException)) {
                  throw (Throwable)var8;
               } else {
                  throw var7;
               }
            }
         }

         public Response executeBaseGet() {
            WebTarget var1 = this.target2;
            String[] var2 = new String[]{"application/json"};
            Builder var3 = var1.request(var2);
            Method var4 = this.javaMethod2;
            var3 = var3.property("org.eclipse.microprofile.rest.client.invokedMethod", var4);
            HeaderFiller var5 = this.headerFiller2;
            var3 = var3.property("io.quarkus.resteasy.reactive.client.microprofile.HeaderFiller", var5);

            try {
               return (Response)var3.method("GET", Response.class);
            } catch (ProcessingException var8) {
               Throwable var7 = ((Throwable)var8).getCause();
               if (!(var7 instanceof WebApplicationException)) {
                  throw (Throwable)var8;
               } else {
                  throw var7;
               }
            }
         }

         public void close() {
            ((WebTargetImpl)this.target1).getRestClient().close();
            ((WebTargetImpl)this.target2).getRestClient().close();
         }
      }
       ```

       @formatter:on

       A more full example of generated client (with sub-resource) can is at the bottom of
       extensions/resteasy-reactive/rest-client-reactive/deployment/src/test/java/io/quarkus/rest/client/reactive/subresource/SubResourceTest.java
     */
    private RuntimeValue<BiFunction<WebTarget, List<ParamConverterProvider>, ?>> generateClientInvoker(
            RecorderContext recorderContext,
            RestClientInterface restClientInterface, List<JaxrsClientReactiveEnricherBuildItem> enrichers,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, ClassInfo interfaceClass,
            IndexView index, String defaultMediaType, Map<DotName, String> httpAnnotationToMethod,
            boolean observabilityIntegrationNeeded, Set<ClassInfo> multipartResponseTypes) {

        String creatorName = restClientInterface.getClassName() + "$$QuarkusRestClientInterfaceCreator";
        String name = restClientInterface.getClassName() + "$$QuarkusRestClientInterface";
        MethodDescriptor constructorDesc = MethodDescriptor.ofConstructor(name, WebTarget.class.getName(), List.class);
        try (ClassRestClientContext classContext = new ClassRestClientContext(name, constructorDesc, generatedClasses,
                RestClientBase.class, Closeable.class.getName(), restClientInterface.getClassName())) {
            classContext.constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(RestClientBase.class, List.class),
                    classContext.constructor.getThis(), classContext.constructor.getMethodParam(1));

            AssignableResultHandle baseTarget = classContext.constructor.createVariable(WebTargetImpl.class);
            if (restClientInterface.isEncoded()) {
                classContext.constructor.assign(baseTarget,
                        disableEncodingForWebTarget(classContext.constructor, classContext.constructor.getMethodParam(0)));
            } else {
                classContext.constructor.assign(baseTarget, classContext.constructor.getMethodParam(0));
            }

            classContext.constructor.assign(baseTarget,
                    classContext.constructor.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(WebTargetImpl.class, "path", WebTargetImpl.class, String.class),
                            baseTarget,
                            classContext.constructor.load(restClientInterface.getPath())));
            FieldDescriptor baseTargetField = classContext.classCreator
                    .getFieldCreator("baseTarget", WebTargetImpl.class.getName())
                    .getFieldDescriptor();
            classContext.constructor.writeInstanceField(baseTargetField, classContext.constructor.getThis(), baseTarget);

            for (JaxrsClientReactiveEnricherBuildItem enricher : enrichers) {
                enricher.getEnricher().forClass(classContext.constructor, baseTarget, interfaceClass, index);
            }

            //
            // go through all the methods of the jaxrs interface. Create specific WebTargets (in the constructor) and methods
            //
            int methodIndex = 0;
            for (ResourceMethod method : restClientInterface.getMethods()) {
                methodIndex++;

                // finding corresponding jandex method, used by enricher (MicroProfile enricher stores it in a field
                // to later fill in context with corresponding java.lang.reflect.Method)
                String[] javaMethodParameters = new String[method.getParameters().length];
                for (int i = 0; i < method.getParameters().length; i++) {
                    MethodParameter param = method.getParameters()[i];
                    javaMethodParameters[i] = param.declaredType != null ? param.declaredType : param.type;
                }
                MethodInfo jandexMethod = getJavaMethod(interfaceClass, method, method.getParameters(), index)
                        .orElseThrow(() -> new RuntimeException(
                                "Failed to find matching java method for " + method + " on " + interfaceClass
                                        + ". It may have unresolved parameter types (generics)"));

                if (!Modifier.isAbstract(jandexMethod.flags())) {
                    // ignore default methods, they can be used for Fault Tolerance's @Fallback or filling headers
                    continue;
                }

                if (method.getHttpMethod() == null) {
                    handleSubResourceMethod(enrichers, generatedClasses, interfaceClass, index, defaultMediaType,
                            httpAnnotationToMethod, name, classContext, baseTarget, methodIndex, method,
                            javaMethodParameters, jandexMethod, multipartResponseTypes, Collections.emptyList());
                } else {
                    FieldDescriptor methodField = classContext.createJavaMethodField(interfaceClass, jandexMethod,
                            methodIndex);
                    Supplier<FieldDescriptor> methodParamAnnotationsField = classContext
                            .getLazyJavaMethodParamAnnotationsField(methodIndex);
                    Supplier<FieldDescriptor> methodGenericParametersField = classContext
                            .getLazyJavaMethodGenericParametersField(methodIndex);

                    // if the response is multipart, let's add it's class to the appropriate collection:
                    addResponseTypeIfMultipart(multipartResponseTypes, jandexMethod, index);

                    // constructor: initializing the immutable part of the method-specific web target
                    FieldDescriptor webTargetForMethod = FieldDescriptor.of(name, "target" + methodIndex, WebTargetImpl.class);
                    classContext.classCreator.getFieldCreator(webTargetForMethod).setModifiers(Modifier.FINAL);

                    AssignableResultHandle constructorTarget = createWebTargetForMethod(classContext.constructor, baseTarget,
                            method);
                    classContext.constructor.writeInstanceField(webTargetForMethod, classContext.constructor.getThis(),
                            constructorTarget);
                    if (observabilityIntegrationNeeded) {
                        String templatePath = MULTIPLE_SLASH_PATTERN.matcher(restClientInterface.getPath() + method.getPath())
                                .replaceAll("/");
                        classContext.constructor.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(WebTargetImpl.class, "setPreClientSendHandler", void.class,
                                        ClientRestHandler.class),
                                classContext.constructor.readInstanceField(webTargetForMethod,
                                        classContext.constructor.getThis()),
                                classContext.constructor.newInstance(
                                        MethodDescriptor.ofConstructor(ClientObservabilityHandler.class, String.class),
                                        classContext.constructor.load(templatePath)));
                    }

                    // generate implementation for a method from jaxrs interface:
                    MethodCreator methodCreator = classContext.classCreator.getMethodCreator(method.getName(),
                            method.getSimpleReturnType(),
                            javaMethodParameters);

                    AssignableResultHandle methodTarget = methodCreator.createVariable(WebTarget.class);
                    methodCreator.assign(methodTarget,
                            methodCreator.readInstanceField(webTargetForMethod, methodCreator.getThis()));
                    if (!restClientInterface.isEncoded() && method.isEncoded()) {
                        methodCreator.assign(methodTarget, disableEncodingForWebTarget(methodCreator, methodTarget));
                    }

                    Integer bodyParameterIdx = null;
                    Map<MethodDescriptor, ResultHandle> invocationBuilderEnrichers = new HashMap<>();

                    String[] consumes = extractProducesConsumesValues(
                            jandexMethod.declaringClass().declaredAnnotation(CONSUMES), method.getConsumes());
                    boolean multipart = isMultipart(consumes, method.getParameters());

                    AssignableResultHandle formParams = null;

                    boolean lastEncodingEnabledByParam = true;
                    for (int paramIdx = 0; paramIdx < method.getParameters().length; ++paramIdx) {
                        MethodParameter param = method.getParameters()[paramIdx];
                        if (!restClientInterface.isEncoded() && !method.isEncoded()) {
                            boolean needsDisabling = isParamAlreadyEncoded(param);
                            if (lastEncodingEnabledByParam && needsDisabling) {
                                methodCreator.assign(methodTarget, disableEncodingForWebTarget(methodCreator, methodTarget));
                                lastEncodingEnabledByParam = false;
                            } else if (!lastEncodingEnabledByParam && !needsDisabling) {
                                methodCreator.assign(methodTarget, enableEncodingForWebTarget(methodCreator, methodTarget));
                                lastEncodingEnabledByParam = true;
                            }
                        }

                        if (param.parameterType == ParameterType.QUERY) {
                            //TODO: converters

                            // query params have to be set on a method-level web target (they vary between invocations)
                            methodCreator.assign(methodTarget,
                                    addQueryParam(jandexMethod, methodCreator, methodTarget, param.name,
                                            methodCreator.getMethodParam(paramIdx),
                                            jandexMethod.parameterType(paramIdx), index, methodCreator.getThis(),
                                            getGenericTypeFromArray(methodCreator, methodGenericParametersField, paramIdx),
                                            getAnnotationsFromArray(methodCreator, methodParamAnnotationsField, paramIdx)));
                        } else if (param.parameterType == ParameterType.BEAN
                                || param.parameterType == ParameterType.MULTI_PART_FORM) {
                            // bean params require both, web-target and Invocation.Builder, modifications
                            // The web target changes have to be done on the method level.
                            // Invocation.Builder changes are offloaded to a separate method
                            // so that we can generate bytecode for both, web target and invocation builder modifications
                            // at once
                            ClientBeanParamInfo beanParam = (ClientBeanParamInfo) param;
                            MethodDescriptor handleBeanParamDescriptor = MethodDescriptor.ofMethod(name,
                                    method.getName() + "$$" + methodIndex + "$$handleBeanParam$$" + paramIdx,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.type);
                            MethodCreator handleBeanParamMethod = classContext.classCreator.getMethodCreator(
                                    handleBeanParamDescriptor).setModifiers(Modifier.PRIVATE);

                            AssignableResultHandle invocationBuilderRef = handleBeanParamMethod
                                    .createVariable(Invocation.Builder.class);
                            handleBeanParamMethod.assign(invocationBuilderRef, handleBeanParamMethod.getMethodParam(0));

                            Supplier<FieldDescriptor> beanParamDescriptorsField = classContext
                                    .getLazyBeanParameterDescriptors(beanParam.type);

                            formParams = addBeanParamData(jandexMethod, methodCreator, handleBeanParamMethod,
                                    invocationBuilderRef, classContext, beanParam.getItems(),
                                    methodCreator.getMethodParam(paramIdx), methodTarget, index,
                                    restClientInterface.getClassName(),
                                    methodCreator.getThis(),
                                    handleBeanParamMethod.getThis(),
                                    formParams, beanParamDescriptorsField, multipart,
                                    beanParam.type);

                            handleBeanParamMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleBeanParamDescriptor, methodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.PATH) {
                            // methodTarget = methodTarget.resolveTemplate(paramname, paramvalue);
                            addPathParam(methodCreator, methodTarget, param.name, methodCreator.getMethodParam(paramIdx),
                                    param.type, methodCreator.getThis(),
                                    getGenericTypeFromArray(methodCreator, methodGenericParametersField, paramIdx),
                                    getAnnotationsFromArray(methodCreator, methodParamAnnotationsField, paramIdx));
                        } else if (param.parameterType == ParameterType.BODY) {
                            // just store the index of parameter used to create the body, we'll use it later
                            bodyParameterIdx = paramIdx;
                        } else if (param.parameterType == ParameterType.HEADER) {
                            // headers are added at the invocation builder level
                            MethodDescriptor handleHeaderDescriptor = MethodDescriptor.ofMethod(name,
                                    method.getName() + "$$" + methodIndex + "$$handleHeader$$" + paramIdx,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.declaredType);
                            MethodCreator handleHeaderMethod = classContext.classCreator.getMethodCreator(
                                    handleHeaderDescriptor).setModifiers(Modifier.PRIVATE);

                            AssignableResultHandle invocationBuilderRef = handleHeaderMethod
                                    .createVariable(Invocation.Builder.class);
                            handleHeaderMethod.assign(invocationBuilderRef, handleHeaderMethod.getMethodParam(0));
                            addHeaderParam(handleHeaderMethod, invocationBuilderRef, param.name,
                                    handleHeaderMethod.getMethodParam(1), param.type,
                                    handleHeaderMethod.getThis(),
                                    getGenericTypeFromArray(handleHeaderMethod, methodGenericParametersField, paramIdx),
                                    getAnnotationsFromArray(handleHeaderMethod, methodParamAnnotationsField, paramIdx));
                            handleHeaderMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleHeaderDescriptor, methodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.COOKIE) {
                            // headers are added at the invocation builder level
                            MethodDescriptor handleHeaderDescriptor = MethodDescriptor.ofMethod(name,
                                    method.getName() + "$$" + methodIndex + "$$handleCookie$$" + paramIdx,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.type);
                            MethodCreator handleCookieMethod = classContext.classCreator.getMethodCreator(
                                    handleHeaderDescriptor).setModifiers(Modifier.PRIVATE);

                            AssignableResultHandle invocationBuilderRef = handleCookieMethod
                                    .createVariable(Invocation.Builder.class);
                            handleCookieMethod.assign(invocationBuilderRef, handleCookieMethod.getMethodParam(0));
                            addCookieParam(handleCookieMethod, invocationBuilderRef, param.name,
                                    handleCookieMethod.getMethodParam(1), param.type,
                                    handleCookieMethod.getThis(),
                                    getGenericTypeFromArray(handleCookieMethod, methodGenericParametersField, paramIdx),
                                    getAnnotationsFromArray(handleCookieMethod, methodParamAnnotationsField, paramIdx));
                            handleCookieMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleHeaderDescriptor, methodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.FORM) {
                            formParams = createFormDataIfAbsent(methodCreator, formParams, multipart);
                            // NOTE: don't use type here, because we're not going through the collection converters and stuff
                            addFormParam(methodCreator, param.name, methodCreator.getMethodParam(paramIdx), param.declaredType,
                                    param.signature,
                                    restClientInterface.getClassName(), methodCreator.getThis(), formParams,
                                    getGenericTypeFromArray(methodCreator, methodGenericParametersField, paramIdx),
                                    getAnnotationsFromArray(methodCreator, methodParamAnnotationsField, paramIdx),
                                    multipart,
                                    param.mimeType, param.partFileName,
                                    jandexMethod.declaringClass().name() + "." + jandexMethod.name());
                        }
                    }

                    for (JaxrsClientReactiveEnricherBuildItem enricher : enrichers) {
                        enricher.getEnricher()
                                .forWebTarget(methodCreator, index, interfaceClass, jandexMethod, methodTarget,
                                        generatedClasses);
                        formParams = enricher.getEnricher().handleFormParams(methodCreator, index, interfaceClass, jandexMethod,
                                generatedClasses, formParams, multipart);
                    }

                    AssignableResultHandle builder = methodCreator.createVariable(Invocation.Builder.class);
                    if (method.getProduces() == null || method.getProduces().length == 0) { // this should never happen!
                        methodCreator.assign(builder, methodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class), methodTarget));
                    } else {

                        ResultHandle array = methodCreator.newArray(String.class, method.getProduces().length);
                        for (int i = 0; i < method.getProduces().length; ++i) {
                            methodCreator.writeArrayValue(array, i, methodCreator.load(method.getProduces()[i]));
                        }
                        methodCreator.assign(builder, methodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class, String[].class),
                                methodTarget, array));
                    }

                    for (Map.Entry<MethodDescriptor, ResultHandle> invocationBuilderEnricher : invocationBuilderEnrichers
                            .entrySet()) {
                        methodCreator.assign(builder,
                                methodCreator.invokeVirtualMethod(invocationBuilderEnricher.getKey(), methodCreator.getThis(),
                                        builder, invocationBuilderEnricher.getValue()));
                    }

                    for (JaxrsClientReactiveEnricherBuildItem enricher : enrichers) {
                        enricher.getEnricher()
                                .forMethod(classContext.classCreator, classContext.constructor, classContext.clinit,
                                        methodCreator, interfaceClass, jandexMethod, builder, index, generatedClasses,
                                        methodIndex, methodField);
                    }

                    handleReturn(interfaceClass, defaultMediaType, method.getHttpMethod(),
                            method.getConsumes(), jandexMethod, methodCreator, formParams,
                            bodyParameterIdx == null ? null : methodCreator.getMethodParam(bodyParameterIdx), builder,
                            multipart);
                }
            }

            classContext.constructor.returnValue(null);
            classContext.clinit.returnValue(null);

            // create `void close()` method:
            // we only close the RestClient of the base target - all targets share the same one
            MethodCreator closeCreator = classContext.classCreator
                    .getMethodCreator(MethodDescriptor.ofMethod(Closeable.class, "close", void.class));
            ResultHandle webTarget = closeCreator.readInstanceField(baseTargetField, closeCreator.getThis());
            ResultHandle webTargetImpl = closeCreator.checkCast(webTarget, WebTargetImpl.class);
            ResultHandle restClient = closeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(WebTargetImpl.class, "getRestClient", ClientImpl.class), webTargetImpl);
            closeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ClientImpl.class, "close", void.class), restClient);
            closeCreator.returnValue(null);
        }

        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClasses, true),
                creatorName, null, Object.class.getName(), BiFunction.class.getName())) {

            MethodCreator apply = c
                    .getMethodCreator(
                            MethodDescriptor.ofMethod(creatorName, "apply", Object.class, Object.class, Object.class));
            apply.returnValue(
                    apply.newInstance(constructorDesc, apply.getMethodParam(0), apply.getMethodParam(1)));
        }

        return recorderContext.newInstance(creatorName);

    }

    /**
     * The @Encoded annotation is only supported in path/query/matrix/form params.
     */
    private boolean isParamAlreadyEncoded(MethodParameter param) {
        return param.encoded
                && (param.parameterType == ParameterType.PATH
                        || param.parameterType == ParameterType.QUERY
                        || param.parameterType == ParameterType.FORM
                        || param.parameterType == ParameterType.MATRIX);
    }

    private ResultHandle disableEncodingForWebTarget(BytecodeCreator creator, ResultHandle baseTarget) {
        return setEncodingForWebTarget(creator, baseTarget, false);
    }

    private ResultHandle enableEncodingForWebTarget(BytecodeCreator creator, ResultHandle baseTarget) {
        return setEncodingForWebTarget(creator, baseTarget, true);
    }

    private ResultHandle setEncodingForWebTarget(BytecodeCreator creator, ResultHandle baseTarget, boolean encode) {
        ResultHandle webTarget = creator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(WebTargetImpl.class, "clone", WebTargetImpl.class), baseTarget);

        ResultHandle uriBuilderImpl = creator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(WebTargetImpl.class, "getUriBuilderUnsafe", UriBuilderImpl.class),
                webTarget);

        creator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(UriBuilderImpl.class, "encode", UriBuilder.class, boolean.class),
                uriBuilderImpl, creator.load(encode));

        return webTarget;
    }

    private boolean isMultipart(String[] consumes, MethodParameter[] methodParameters) {
        if (consumes != null) {
            for (String mimeType : consumes) {
                if (mimeType.startsWith(MediaType.MULTIPART_FORM_DATA)) {
                    return true;
                }
            }
        }
        // see if the parameters require a multipart form
        for (MethodParameter methodParameter : methodParameters) {
            if (methodParameter.parameterType == ParameterType.FORM) {
                if (isMultipartRequiringType(methodParameter.signature, methodParameter.mimeType)) {
                    return true;
                }
            } else if (methodParameter.parameterType == ParameterType.BEAN
                    || methodParameter.parameterType == ParameterType.MULTI_PART_FORM) {
                ClientBeanParamInfo beanParam = (ClientBeanParamInfo) methodParameter;
                if (isMultipartRequiringBeanParam(beanParam.getItems())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMultipartRequiringType(String signature, String partType) {
        return (signature.equals(FILE_SIGNATURE)
                || signature.equals(PATH_SIGNATURE)
                || signature.equals(BUFFER_SIGNATURE)
                || signature.equals(BYTE_ARRAY_SIGNATURE)
                || signature.equals(MULTI_BYTE_SIGNATURE)
                || partType != null);
    }

    private boolean isMultipartRequiringBeanParam(List<Item> beanItems) {
        for (Item beanItem : beanItems) {
            if (beanItem instanceof FormParamItem) {
                FormParamItem formParamItem = (FormParamItem) beanItem;
                if (isMultipartRequiringType(formParamItem.getParamSignature(), formParamItem.getMimeType())) {
                    return true;
                }
            } else if (beanItem instanceof BeanParamItem) {
                BeanParamItem beanParamItem = (BeanParamItem) beanItem;
                if (isMultipartRequiringBeanParam(beanParamItem.items())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addResponseTypeIfMultipart(Set<ClassInfo> multipartResponseTypes, MethodInfo method, IndexView index) {
        AnnotationInstance produces = method.annotation(ResteasyReactiveDotNames.PRODUCES);
        if (produces == null) {
            produces = method.annotation(ResteasyReactiveDotNames.PRODUCES);
        }
        if (produces != null) {
            String[] producesValues = produces.value().asStringArray();
            for (String producesValue : producesValues) {
                if (producesValue.toLowerCase(Locale.ROOT)
                        .startsWith(MULTIPART_FORM_DATA)) {
                    multipartResponseTypes.add(returnTypeAsClass(method, index));
                }
            }
        }
    }

    private ClassInfo returnTypeAsClass(MethodInfo jandexMethod, IndexView index) {
        Type result = jandexMethod.returnType();
        if (result.kind() == PARAMETERIZED_TYPE) {
            if (result.name().equals(COMPLETION_STAGE) || result.name().equals(UNI)) {
                Type firstArgument = result.asParameterizedType().arguments().get(0);
                if (firstArgument.kind() == CLASS) {
                    return index.getClassByName(firstArgument.asClassType().name());
                }
            }
        } else if (result.kind() == CLASS) {
            return index.getClassByName(result.asClassType().name());
        }
        throw new IllegalArgumentException("multipart responses can only be mapped to non-generic classes, " +
                "got " + result + " of type: " + result.kind());
    }

    private void handleSubResourceMethod(List<JaxrsClientReactiveEnricherBuildItem> enrichers,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, ClassInfo interfaceClass, IndexView index,
            String defaultMediaType, Map<DotName, String> httpAnnotationToMethod, String name,
            ClassRestClientContext ownerContext, ResultHandle ownerTarget, int methodIndex,
            ResourceMethod method, String[] javaMethodParameters, MethodInfo jandexMethod,
            Set<ClassInfo> multipartResponseTypes, List<SubResourceParameter> ownerSubResourceParameters) {
        Type returnType = jandexMethod.returnType();
        if (returnType.kind() != CLASS) {
            // sort of sub-resource method that returns a thing that isn't a class
            throw new IllegalArgumentException("Sub resource type is not a class: " + returnType.name().toString());
        }
        ClassInfo subInterface = index.getClassByName(returnType.name());
        if (!Modifier.isInterface(subInterface.flags())) {
            throw new IllegalArgumentException(
                    "Client interface method: " + jandexMethod.declaringClass().name() + "#" + jandexMethod
                            + " has no HTTP method annotation  (@GET, @POST, etc) and it's return type: "
                            + returnType.name().toString() + " is not an interface. "
                            + "If it's a sub resource method, it has to return an interface. "
                            + "If it's not, it has to have one of the HTTP method annotations.");
        }

        ownerContext.createJavaMethodField(interfaceClass, jandexMethod, methodIndex);

        // generate implementation for a method that returns the sub-client:
        MethodCreator ownerMethod = ownerContext.classCreator.getMethodCreator(method.getName(), method.getSimpleReturnType(),
                javaMethodParameters);

        String subName = subInterface.name().toString() + HashUtil.sha1(name) + methodIndex;
        MethodDescriptor subConstructorDescriptor = MethodDescriptor.ofConstructor(subName, WebTargetImpl.class.getName());
        try (ClassRestClientContext subContext = new ClassRestClientContext(subName, subConstructorDescriptor,
                generatedClasses, Object.class, subInterface.name().toString())) {

            subContext.constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class),
                    subContext.constructor.getThis());

            AssignableResultHandle constructorTarget = createWebTargetForMethod(ownerContext.constructor, ownerTarget,
                    method);

            boolean encodingEnabled = true;

            FieldDescriptor forMethodTargetDesc = ownerContext.classCreator
                    .getFieldCreator("targetInOwner" + methodIndex, WebTargetImpl.class).getFieldDescriptor();
            if (subInterface.hasDeclaredAnnotation(ENCODED)) {
                ownerContext.constructor.assign(constructorTarget,
                        disableEncodingForWebTarget(ownerContext.constructor, constructorTarget));
                encodingEnabled = false;
            }

            ownerContext.constructor.writeInstanceField(forMethodTargetDesc, ownerContext.constructor.getThis(),
                    constructorTarget);

            Supplier<FieldDescriptor> methodParamAnnotationsField = ownerContext.getLazyJavaMethodParamAnnotationsField(
                    methodIndex);
            Supplier<FieldDescriptor> methodGenericParametersField = ownerContext.getLazyJavaMethodGenericParametersField(
                    methodIndex);

            AssignableResultHandle client = createRestClientField(name, ownerContext.classCreator, ownerMethod);
            AssignableResultHandle webTarget = ownerMethod.createVariable(WebTarget.class);
            ownerMethod.assign(webTarget, ownerMethod.readInstanceField(forMethodTargetDesc, ownerMethod.getThis()));

            if (encodingEnabled && method.isEncoded()) {
                ownerMethod.assign(webTarget, disableEncodingForWebTarget(ownerMethod, webTarget));
            }

            // Setup Path param from current method
            boolean lastEncodingEnabledByParam = true;
            for (int i = 0; i < method.getParameters().length; i++) {
                MethodParameter param = method.getParameters()[i];
                if (encodingEnabled && !method.isEncoded()) {
                    boolean needsDisabling = isParamAlreadyEncoded(param);
                    if (lastEncodingEnabledByParam && needsDisabling) {
                        ownerMethod.assign(webTarget, disableEncodingForWebTarget(ownerMethod, webTarget));
                        lastEncodingEnabledByParam = false;
                    } else if (!lastEncodingEnabledByParam && !needsDisabling) {
                        ownerMethod.assign(webTarget, enableEncodingForWebTarget(ownerMethod, webTarget));
                        lastEncodingEnabledByParam = true;
                    }
                }

                if (param.parameterType == ParameterType.PATH) {
                    ResultHandle paramValue = ownerMethod.getMethodParam(i);
                    // methodTarget = methodTarget.resolveTemplate(paramname, paramvalue);
                    addPathParam(ownerMethod, webTarget, param.name, paramValue,
                            param.type,
                            client,
                            getGenericTypeFromArray(ownerMethod, methodGenericParametersField, i),
                            getAnnotationsFromArray(ownerMethod, methodParamAnnotationsField, i));
                }
            }

            // Continue creating the subresource instance with the web target updated
            ResultHandle subInstance = ownerMethod.newInstance(subConstructorDescriptor, webTarget);

            List<SubResourceParameter> subParamFields = new ArrayList<>();

            for (SubResourceParameter ownerParameter : ownerSubResourceParameters) {
                FieldDescriptor paramField = subContext.classCreator.getFieldCreator(ownerParameter.field.getName() + "$_",
                        ownerParameter.typeName)
                        .setModifiers(Modifier.PUBLIC)
                        .getFieldDescriptor();
                ownerMethod.writeInstanceField(paramField, subInstance,
                        ownerMethod.readInstanceField(ownerParameter.field, ownerMethod.getThis()));
                subParamFields.add(new SubResourceParameter(ownerParameter.methodParameter, ownerParameter.typeName,
                        ownerParameter.type, paramField, ownerParameter.paramAnnotationsField,
                        ownerParameter.genericsParametersField,
                        ownerParameter.paramIndex));
            }

            FieldDescriptor clientField = subContext.classCreator.getFieldCreator("client", RestClientBase.class)
                    .setModifiers(Modifier.PUBLIC)
                    .getFieldDescriptor();
            ownerMethod.writeInstanceField(clientField, subInstance, client);
            // method parameters (except path parameters) are rewritten to sub client fields (directly, public fields):
            for (int i = 0; i < method.getParameters().length; i++) {
                MethodParameter param = method.getParameters()[i];
                if (param.parameterType != ParameterType.PATH) {
                    FieldDescriptor paramField = subContext.classCreator.getFieldCreator("param" + i, param.type)
                            .setModifiers(Modifier.PUBLIC)
                            .getFieldDescriptor();
                    ownerMethod.writeInstanceField(paramField, subInstance, ownerMethod.getMethodParam(i));
                    subParamFields.add(new SubResourceParameter(method.getParameters()[i], param.type,
                            jandexMethod.parameterType(i), paramField, methodParamAnnotationsField,
                            methodGenericParametersField,
                            i));
                }

            }

            int subMethodIndex = 0;
            for (ResourceMethod subMethod : method.getSubResourceMethods()) {
                MethodInfo jandexSubMethod = getJavaMethod(subInterface, subMethod,
                        subMethod.getParameters(), index)
                        .orElseThrow(() -> new RuntimeException(
                                "Failed to find matching java method for " + subMethod + " on "
                                        + subInterface
                                        + ". It may have unresolved parameter types (generics)"));
                subMethodIndex++;
                String[] consumes = extractProducesConsumesValues(
                        jandexSubMethod.declaringClass().declaredAnnotation(CONSUMES), method.getConsumes());
                consumes = extractProducesConsumesValues(jandexSubMethod.annotation(CONSUMES), consumes);
                boolean multipart = isMultipart(consumes, subMethod.getParameters());

                boolean isSubResourceMethod = subMethod.getHttpMethod() == null;
                if (!isSubResourceMethod) {
                    // java method data:
                    FieldDescriptor subMethodField = subContext.createJavaMethodField(subInterface, jandexSubMethod,
                            subMethodIndex);
                    Supplier<FieldDescriptor> subMethodParamAnnotationsField = subContext
                            .getLazyJavaMethodParamAnnotationsField(subMethodIndex);
                    Supplier<FieldDescriptor> subMethodGenericParametersField = subContext
                            .getLazyJavaMethodGenericParametersField(subMethodIndex);

                    MethodCreator subMethodCreator = subContext.classCreator.getMethodCreator(subMethod.getName(),
                            jandexSubMethod.returnType().name().toString(),
                            parametersAsStringArray(jandexSubMethod));
                    // initializing the web target in the sub constructor:
                    FieldDescriptor subMethodTarget = FieldDescriptor.of(subName, "target" + subMethodIndex,
                            WebTarget.class);
                    subContext.classCreator.getFieldCreator(subMethodTarget).setModifiers(Modifier.FINAL);

                    AssignableResultHandle subMethodTargetV = subContext.constructor.createVariable(WebTargetImpl.class);
                    subContext.constructor.assign(subMethodTargetV, subContext.constructor.getMethodParam(0));
                    if (subMethod.getPath() != null) {
                        appendPath(subContext.constructor, subMethod.getPath(), subMethodTargetV);
                    }

                    subContext.constructor.writeInstanceField(subMethodTarget, subContext.constructor.getThis(),
                            subMethodTargetV);

                    AssignableResultHandle methodTarget = subMethodCreator.createVariable(WebTarget.class);
                    subMethodCreator.assign(methodTarget,
                            subMethodCreator.readInstanceField(subMethodTarget, subMethodCreator.getThis()));
                    if (encodingEnabled && subMethod.isEncoded()) {
                        subMethodCreator.assign(methodTarget, disableEncodingForWebTarget(subMethodCreator, methodTarget));
                    }

                    ResultHandle bodyParameterValue = null;
                    AssignableResultHandle formParams = null;
                    Map<MethodDescriptor, ResultHandle> invocationBuilderEnrichers = new HashMap<>();

                    boolean lastEncodingEnabledBySubParam = true;
                    int inheritedParamIndex = 0;
                    for (SubResourceParameter subParamField : subParamFields) {
                        inheritedParamIndex++;
                        MethodParameter param = subParamField.methodParameter;
                        ResultHandle paramValue = subMethodCreator.readInstanceField(subParamField.field,
                                subMethodCreator.getThis());
                        if (encodingEnabled && !subMethod.isEncoded()) {
                            boolean needsDisabling = isParamAlreadyEncoded(param);
                            if (lastEncodingEnabledBySubParam && needsDisabling) {
                                subMethodCreator.assign(methodTarget,
                                        disableEncodingForWebTarget(subMethodCreator, methodTarget));
                                lastEncodingEnabledBySubParam = false;
                            } else if (!lastEncodingEnabledBySubParam && !needsDisabling) {
                                subMethodCreator.assign(methodTarget,
                                        enableEncodingForWebTarget(subMethodCreator, methodTarget));
                                lastEncodingEnabledBySubParam = true;
                            }
                        }

                        if (param.parameterType == ParameterType.QUERY) {
                            //TODO: converters

                            // query params have to be set on a method-level web target (they vary between invocations)
                            subMethodCreator.assign(methodTarget,
                                    addQueryParam(jandexMethod, subMethodCreator, methodTarget, param.name,
                                            paramValue, subParamField.type, index,
                                            subMethodCreator.readInstanceField(clientField, subMethodCreator.getThis()),
                                            getGenericTypeFromArray(subMethodCreator, subParamField.genericsParametersField,
                                                    subParamField.paramIndex),
                                            getAnnotationsFromArray(subMethodCreator, subParamField.paramAnnotationsField,
                                                    subParamField.paramIndex)));
                        } else if (param.parameterType == ParameterType.BEAN
                                || param.parameterType == ParameterType.MULTI_PART_FORM) {
                            // bean params require both, web-target and Invocation.Builder, modifications
                            // The web target changes have to be done on the method level.
                            // Invocation.Builder changes are offloaded to a separate method
                            // so that we can generate bytecode for both, web target and invocation builder modifications
                            // at once
                            ClientBeanParamInfo beanParam = (ClientBeanParamInfo) param;
                            MethodDescriptor handleBeanParamDescriptor = MethodDescriptor.ofMethod(subName,
                                    subMethod.getName() + "$$" + methodIndex + "$$handleBeanParam$$" + inheritedParamIndex
                                            + "$" + subParamField.paramIndex,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.type);
                            MethodCreator handleBeanParamMethod = subContext.classCreator.getMethodCreator(
                                    handleBeanParamDescriptor).setModifiers(Modifier.PRIVATE);

                            Supplier<FieldDescriptor> beanParamDescriptors = subContext
                                    .getLazyBeanParameterDescriptors(beanParam.type);

                            AssignableResultHandle invocationBuilderRef = handleBeanParamMethod
                                    .createVariable(Invocation.Builder.class);
                            handleBeanParamMethod.assign(invocationBuilderRef, handleBeanParamMethod.getMethodParam(0));
                            formParams = addBeanParamData(jandexMethod, subMethodCreator, handleBeanParamMethod,
                                    invocationBuilderRef, subContext, beanParam.getItems(),
                                    paramValue, methodTarget, index,
                                    interfaceClass.name().toString(),
                                    subMethodCreator.readInstanceField(clientField, subMethodCreator.getThis()),
                                    handleBeanParamMethod.readInstanceField(clientField, handleBeanParamMethod.getThis()),
                                    formParams,
                                    beanParamDescriptors,
                                    multipart, beanParam.type);

                            handleBeanParamMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleBeanParamDescriptor, paramValue);
                        } else if (param.parameterType == ParameterType.PATH) {
                            // methodTarget = methodTarget.resolveTemplate(paramname, paramvalue);
                            addPathParam(subMethodCreator, methodTarget, param.name, paramValue,
                                    param.type,
                                    subMethodCreator.readInstanceField(clientField, subMethodCreator.getThis()),
                                    getGenericTypeFromArray(subMethodCreator, subParamField.genericsParametersField,
                                            subParamField.paramIndex),
                                    getAnnotationsFromArray(subMethodCreator, subParamField.paramAnnotationsField,
                                            subParamField.paramIndex));
                        } else if (param.parameterType == ParameterType.BODY) {
                            // just store the index of parameter used to create the body, we'll use it later
                            bodyParameterValue = paramValue;
                        } else if (param.parameterType == ParameterType.HEADER) {
                            // headers are added at the invocation builder level
                            MethodDescriptor handleHeaderDescriptor = MethodDescriptor.ofMethod(subName,
                                    subMethod.getName() + "$$" + subMethodIndex + "$$handleHeader$$param"
                                            + inheritedParamIndex + "$" + subParamField.paramIndex,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.declaredType);
                            MethodCreator handleHeaderMethod = subContext.classCreator.getMethodCreator(
                                    handleHeaderDescriptor).setModifiers(Modifier.PRIVATE);

                            AssignableResultHandle invocationBuilderRef = handleHeaderMethod
                                    .createVariable(Invocation.Builder.class);
                            handleHeaderMethod.assign(invocationBuilderRef, handleHeaderMethod.getMethodParam(0));
                            addHeaderParam(handleHeaderMethod, invocationBuilderRef, param.name,
                                    handleHeaderMethod.getMethodParam(1),
                                    param.type,
                                    handleHeaderMethod.readInstanceField(clientField, handleHeaderMethod.getThis()),
                                    getGenericTypeFromArray(handleHeaderMethod, subParamField.genericsParametersField,
                                            subParamField.paramIndex),
                                    getAnnotationsFromArray(handleHeaderMethod, subParamField.paramAnnotationsField,
                                            subParamField.paramIndex));
                            handleHeaderMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleHeaderDescriptor, paramValue);
                        } else if (param.parameterType == ParameterType.COOKIE) {
                            // cookies are added at the invocation builder level
                            MethodDescriptor handleCookieDescriptor = MethodDescriptor.ofMethod(subName,
                                    subMethod.getName() + "$$" + subMethodIndex + "$$handleCookie$$param" +
                                            inheritedParamIndex + "$" + subParamField.paramIndex,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.type);
                            MethodCreator handleCookieMethod = subContext.classCreator.getMethodCreator(
                                    handleCookieDescriptor).setModifiers(Modifier.PRIVATE);

                            AssignableResultHandle invocationBuilderRef = handleCookieMethod
                                    .createVariable(Invocation.Builder.class);
                            handleCookieMethod.assign(invocationBuilderRef, handleCookieMethod.getMethodParam(0));
                            addCookieParam(handleCookieMethod, invocationBuilderRef, param.name,
                                    handleCookieMethod.getMethodParam(1),
                                    param.type,
                                    handleCookieMethod.readInstanceField(clientField, handleCookieMethod.getThis()),
                                    getGenericTypeFromArray(handleCookieMethod, subParamField.genericsParametersField,
                                            subParamField.paramIndex),
                                    getAnnotationsFromArray(handleCookieMethod, subParamField.paramAnnotationsField,
                                            subParamField.paramIndex));
                            handleCookieMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleCookieDescriptor, paramValue);
                        } else if (param.parameterType == ParameterType.FORM) {
                            formParams = createFormDataIfAbsent(subMethodCreator, formParams, multipart);
                            // FIXME: this is weird, it doesn't go via converter nor multipart, looks like a bug
                            subMethodCreator.invokeInterfaceMethod(MULTIVALUED_MAP_ADD, formParams,
                                    subMethodCreator.load(param.name), paramValue);
                        }
                    }
                    // handle sub-method parameters:
                    for (int paramIdx = 0; paramIdx < subMethod.getParameters().length; ++paramIdx) {
                        MethodParameter param = subMethod.getParameters()[paramIdx];
                        if (encodingEnabled && !subMethod.isEncoded()) {
                            boolean needsDisabling = isParamAlreadyEncoded(param);
                            if (lastEncodingEnabledBySubParam && needsDisabling) {
                                subMethodCreator.assign(methodTarget,
                                        disableEncodingForWebTarget(subMethodCreator, methodTarget));
                                lastEncodingEnabledBySubParam = false;
                            } else if (!lastEncodingEnabledBySubParam && !needsDisabling) {
                                subMethodCreator.assign(methodTarget,
                                        enableEncodingForWebTarget(subMethodCreator, methodTarget));
                                lastEncodingEnabledBySubParam = true;
                            }
                        }

                        if (param.parameterType == ParameterType.QUERY) {
                            //TODO: converters

                            // query params have to be set on a method-level web target (they vary between invocations)
                            subMethodCreator.assign(methodTarget,
                                    addQueryParam(jandexMethod, subMethodCreator, methodTarget, param.name,
                                            subMethodCreator.getMethodParam(paramIdx),
                                            jandexSubMethod.parameterType(paramIdx), index,
                                            subMethodCreator.readInstanceField(clientField, subMethodCreator.getThis()),
                                            getGenericTypeFromArray(subMethodCreator, subMethodGenericParametersField,
                                                    paramIdx),
                                            getAnnotationsFromArray(subMethodCreator, subMethodParamAnnotationsField,
                                                    paramIdx)));
                        } else if (param.parameterType == ParameterType.BEAN
                                || param.parameterType == ParameterType.MULTI_PART_FORM) {
                            // bean params require both, web-target and Invocation.Builder, modifications
                            // The web target changes have to be done on the method level.
                            // Invocation.Builder changes are offloaded to a separate method
                            // so that we can generate bytecode for both, web target and invocation builder modifications
                            // at once
                            ClientBeanParamInfo beanParam = (ClientBeanParamInfo) param;
                            MethodDescriptor handleBeanParamDescriptor = MethodDescriptor.ofMethod(subName,
                                    subMethod.getName() + "$$" + subMethodIndex + "$$handleBeanParam$$" + paramIdx,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.type);
                            MethodCreator handleBeanParamMethod = subContext.classCreator.getMethodCreator(
                                    handleBeanParamDescriptor).setModifiers(Modifier.PRIVATE);

                            Supplier<FieldDescriptor> beanParamDescriptors = subContext
                                    .getLazyBeanParameterDescriptors(beanParam.type);

                            AssignableResultHandle invocationBuilderRef = handleBeanParamMethod
                                    .createVariable(Invocation.Builder.class);
                            handleBeanParamMethod.assign(invocationBuilderRef, handleBeanParamMethod.getMethodParam(0));
                            formParams = addBeanParamData(jandexMethod, subMethodCreator, handleBeanParamMethod,
                                    invocationBuilderRef, subContext, beanParam.getItems(),
                                    subMethodCreator.getMethodParam(paramIdx), methodTarget, index,
                                    interfaceClass.name().toString(),
                                    subMethodCreator.readInstanceField(clientField, subMethodCreator.getThis()),
                                    handleBeanParamMethod.readInstanceField(clientField, handleBeanParamMethod.getThis()),
                                    formParams,
                                    beanParamDescriptors, multipart,
                                    beanParam.type);

                            handleBeanParamMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleBeanParamDescriptor,
                                    subMethodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.PATH) {
                            addPathParam(subMethodCreator, methodTarget, param.name,
                                    subMethodCreator.getMethodParam(paramIdx), param.type,
                                    subMethodCreator.readInstanceField(clientField, subMethodCreator.getThis()),
                                    getGenericTypeFromArray(subMethodCreator, subMethodGenericParametersField, paramIdx),
                                    getAnnotationsFromArray(subMethodCreator, subMethodParamAnnotationsField, paramIdx));
                        } else if (param.parameterType == ParameterType.BODY) {
                            // just store the index of parameter used to create the body, we'll use it later
                            bodyParameterValue = subMethodCreator.getMethodParam(paramIdx);
                        } else if (param.parameterType == ParameterType.HEADER) {
                            // headers are added at the invocation builder level
                            MethodDescriptor handleHeaderDescriptor = MethodDescriptor.ofMethod(subName,
                                    subMethod.getName() + "$$" + subMethodIndex + "$$handleHeader$$" + paramIdx,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.declaredType);
                            MethodCreator handleHeaderMethod = subContext.classCreator.getMethodCreator(
                                    handleHeaderDescriptor).setModifiers(Modifier.PRIVATE);

                            AssignableResultHandle invocationBuilderRef = handleHeaderMethod
                                    .createVariable(Invocation.Builder.class);
                            handleHeaderMethod.assign(invocationBuilderRef, handleHeaderMethod.getMethodParam(0));
                            addHeaderParam(handleHeaderMethod, invocationBuilderRef, param.name,
                                    handleHeaderMethod.getMethodParam(1), param.type,
                                    handleHeaderMethod.readInstanceField(clientField, handleHeaderMethod.getThis()),
                                    getGenericTypeFromArray(handleHeaderMethod, subMethodGenericParametersField, paramIdx),
                                    getAnnotationsFromArray(handleHeaderMethod, subMethodParamAnnotationsField, paramIdx));
                            handleHeaderMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleHeaderDescriptor, subMethodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.COOKIE) {
                            // cookies are added at the invocation builder level
                            MethodDescriptor handleCookieDescriptor = MethodDescriptor.ofMethod(subName,
                                    subMethod.getName() + "$$" + subMethodIndex + "$$handleCookie$$" + paramIdx,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.type);
                            MethodCreator handleCookieMethod = subContext.classCreator.getMethodCreator(
                                    handleCookieDescriptor).setModifiers(Modifier.PRIVATE);

                            AssignableResultHandle invocationBuilderRef = handleCookieMethod
                                    .createVariable(Invocation.Builder.class);
                            handleCookieMethod.assign(invocationBuilderRef, handleCookieMethod.getMethodParam(0));
                            addCookieParam(handleCookieMethod, invocationBuilderRef, param.name,
                                    handleCookieMethod.getMethodParam(1),
                                    param.type,
                                    handleCookieMethod.readInstanceField(clientField, handleCookieMethod.getThis()),
                                    getGenericTypeFromArray(handleCookieMethod, subMethodGenericParametersField, paramIdx),
                                    getAnnotationsFromArray(handleCookieMethod, subMethodParamAnnotationsField, paramIdx));
                            handleCookieMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleCookieDescriptor, subMethodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.FORM) {
                            formParams = createFormDataIfAbsent(subMethodCreator, formParams, multipart);
                            // FIXME: this is weird, it doesn't go via converter nor multipart, looks like a bug
                            subMethodCreator.invokeInterfaceMethod(MULTIVALUED_MAP_ADD, formParams,
                                    subMethodCreator.load(param.name),
                                    subMethodCreator.getMethodParam(paramIdx));
                        }

                    }

                    // if the response is multipart, let's add it's class to the appropriate collection:
                    addResponseTypeIfMultipart(multipartResponseTypes, jandexSubMethod, index);

                    for (JaxrsClientReactiveEnricherBuildItem enricher : enrichers) {
                        enricher.getEnricher()
                                .forSubResourceWebTarget(subMethodCreator, index, interfaceClass, subInterface,
                                        jandexMethod, jandexSubMethod, methodTarget, generatedClasses);
                        formParams = enricher.getEnricher().handleFormParamsForSubResource(subMethodCreator, index,
                                interfaceClass, subInterface, jandexMethod, jandexSubMethod, methodTarget, generatedClasses,
                                formParams, multipart);
                    }

                    AssignableResultHandle builder = subMethodCreator.createVariable(Invocation.Builder.class);
                    if (method.getProduces() == null || method.getProduces().length == 0) { // this should never happen!
                        subMethodCreator.assign(builder, subMethodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class),
                                methodTarget));
                    } else {

                        ResultHandle array = subMethodCreator.newArray(String.class, subMethod.getProduces().length);
                        for (int i = 0; i < subMethod.getProduces().length; ++i) {
                            subMethodCreator.writeArrayValue(array, i,
                                    subMethodCreator.load(subMethod.getProduces()[i]));
                        }
                        subMethodCreator.assign(builder, subMethodCreator.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class,
                                        String[].class),
                                methodTarget, array));
                    }

                    for (Map.Entry<MethodDescriptor, ResultHandle> invocationBuilderEnricher : invocationBuilderEnrichers
                            .entrySet()) {
                        subMethodCreator.assign(builder,
                                subMethodCreator.invokeVirtualMethod(invocationBuilderEnricher.getKey(),
                                        subMethodCreator.getThis(),
                                        builder, invocationBuilderEnricher.getValue()));
                    }

                    for (JaxrsClientReactiveEnricherBuildItem enricher : enrichers) {
                        enricher.getEnricher()
                                .forSubResourceMethod(subContext.classCreator, subContext.constructor,
                                        subContext.clinit, subMethodCreator, interfaceClass,
                                        subInterface, jandexSubMethod, jandexMethod, builder, index,
                                        generatedClasses, methodIndex, subMethodIndex, subMethodField);
                    }

                    handleReturn(subInterface, defaultMediaType,
                            getHttpMethod(jandexSubMethod, subMethod.getHttpMethod(), httpAnnotationToMethod),
                            consumes, jandexSubMethod, subMethodCreator, formParams, bodyParameterValue,
                            builder, multipart);
                } else {
                    // finding corresponding jandex method, used by enricher (MicroProfile enricher stores it in a field
                    // to later fill in context with corresponding java.lang.reflect.Method)
                    String[] subJavaMethodParameters = new String[subMethod.getParameters().length];
                    for (int i = 0; i < subMethod.getParameters().length; i++) {
                        MethodParameter param = subMethod.getParameters()[i];
                        subJavaMethodParameters[i] = param.declaredType != null ? param.declaredType : param.type;
                    }
                    ResultHandle subMethodTarget = subContext.constructor.getMethodParam(0);
                    handleSubResourceMethod(enrichers, generatedClasses, subInterface, index,
                            defaultMediaType, httpAnnotationToMethod, subName, subContext, subMethodTarget,
                            subMethodIndex, subMethod, subJavaMethodParameters, jandexSubMethod,
                            multipartResponseTypes, subParamFields);
                }

            }

            subContext.constructor.returnValue(null);
            subContext.clinit.returnValue(null);

            ownerMethod.returnValue(subInstance);
        }
    }

    private AssignableResultHandle createWebTargetForMethod(MethodCreator constructor, ResultHandle baseTarget,
            ResourceMethod method) {
        AssignableResultHandle target = constructor.createVariable(WebTarget.class);
        constructor.assign(target, baseTarget);

        if (method.getPath() != null) {
            appendPath(constructor, method.getPath(), target);
        }
        return target;
    }

    private void appendPath(MethodCreator constructor, String pathPart, AssignableResultHandle target) {
        AssignableResultHandle path = constructor.createVariable(String.class);
        constructor.assign(path, constructor.load(pathPart));
        constructor.assign(target,
                constructor.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                        target, path));
    }

    /**
     * Create the `client` field into the `c` class that represents a RestClientBase instance.
     * The RestClientBase instance is coming from either a root client or a sub client (clients generated from root clients).
     */
    private AssignableResultHandle createRestClientField(String name, ClassCreator c, MethodCreator methodCreator) {
        AssignableResultHandle client = methodCreator.createVariable(RestClientBase.class);

        if (c.getSuperClass().contains(RestClientBase.class.getSimpleName())) {
            // We're in a root client, so we can set the client field with: sub.client = (RestClientBase) this
            methodCreator.assign(client, methodCreator.getThis());
        } else {
            FieldDescriptor subClientField = FieldDescriptor.of(name, "client", RestClientBase.class);
            // We're in a sub-sub resource, so we need to get the client from the field: subSub.client = sub.client
            methodCreator.assign(client, methodCreator.readInstanceField(subClientField, methodCreator.getThis()));
        }

        return client;
    }

    private void handleMultipartField(String formParamName, String partType, String partFilename,
            String type,
            String parameterGenericType, ResultHandle fieldValue, AssignableResultHandle multipartForm,
            BytecodeCreator methodCreator,
            ResultHandle client, String restClientInterfaceClassName, ResultHandle parameterAnnotations,
            ResultHandle genericType, String errorLocation) {

        BytecodeCreator ifValueNotNull = methodCreator.ifNotNull(fieldValue).trueBranch();

        // we support string, and send it as an attribute unconverted
        if (type.equals(String.class.getName())) {
            addString(ifValueNotNull, multipartForm, formParamName, partType, partFilename, fieldValue);
        } else if (type.equals(File.class.getName())) {
            // file is sent as file :)
            ResultHandle filePath = ifValueNotNull.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(File.class, "toPath", Path.class), fieldValue);
            addFile(ifValueNotNull, multipartForm, formParamName, partType, partFilename, filePath);
        } else if (type.equals(Path.class.getName())) {
            // and so is path
            addFile(ifValueNotNull, multipartForm, formParamName, partType, partFilename, fieldValue);
        } else if (type.equals(InputStream.class.getName())) {
            // and so is path
            addInputStream(ifValueNotNull, multipartForm, formParamName, partType, partFilename, fieldValue, type);
        } else if (type.equals(Buffer.class.getName())) {
            // and buffer
            addBuffer(ifValueNotNull, multipartForm, formParamName, partType, partFilename, fieldValue, errorLocation);
        } else if (type.startsWith("[")) {
            // byte[] can be sent as file too
            if (!type.equals("[B")) {
                throw new IllegalArgumentException("Array of unsupported type: " + type
                        + " on " + errorLocation);
            }
            ResultHandle buffer = ifValueNotNull.invokeStaticInterfaceMethod(
                    MethodDescriptor.ofMethod(Buffer.class, "buffer", Buffer.class, byte[].class),
                    fieldValue);
            addBuffer(ifValueNotNull, multipartForm, formParamName, partType, partFilename, buffer, errorLocation);
        } else if (parameterGenericType.equals(MULTI_BYTE_SIGNATURE)) {
            addMultiAsFile(ifValueNotNull, multipartForm, formParamName, partType, partFilename, fieldValue, errorLocation);
        } else if (partType != null) {
            if (partFilename != null) {
                log.warnf("Using the @PartFilename annotation is unsupported on the type '%s'. Problematic field is: '%s'",
                        partType, formParamName);
            }
            // assume POJO:
            addPojo(ifValueNotNull, multipartForm, formParamName, partType, fieldValue, type);
        } else {
            // go via converter
            ResultHandle convertedFormParam = convertParamToString(ifValueNotNull, client, fieldValue, type, genericType,
                    parameterAnnotations);
            BytecodeCreator parameterIsStringBranch = checkStringParam(ifValueNotNull, convertedFormParam,
                    restClientInterfaceClassName, errorLocation);
            addString(parameterIsStringBranch, multipartForm, formParamName, null, partFilename, convertedFormParam);
        }
    }

    private void addInputStream(BytecodeCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            String partType, String partFilename, ResultHandle fieldValue, String type) {
        ResultHandle formParamResult = methodCreator.load(formParamName);
        ResultHandle partFilenameResult = partFilename == null ? formParamResult : methodCreator.load(partFilename);
        methodCreator.assign(multipartForm,
                methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ClientMultipartForm.class, "entity",
                        ClientMultipartForm.class, String.class, String.class, Object.class, String.class, Class.class),
                        multipartForm, formParamResult, partFilenameResult, fieldValue,
                        methodCreator.load(partType),
                        // FIXME: doesn't support generics
                        methodCreator.loadClassFromTCCL(type)));
    }

    private void addPojo(BytecodeCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            String partType, ResultHandle fieldValue, String type) {
        methodCreator.assign(multipartForm,
                methodCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ClientMultipartForm.class, "entity",
                        ClientMultipartForm.class, String.class, Object.class, String.class, Class.class),
                        multipartForm, methodCreator.load(formParamName), fieldValue, methodCreator.load(partType),
                        // FIXME: doesn't support generics
                        methodCreator.loadClassFromTCCL(type)));
    }

    /**
     * add file upload, see {@link QuarkusMultipartForm#binaryFileUpload(String, String, String, String)} and
     * {@link QuarkusMultipartForm#textFileUpload(String, String, String, String)}
     */
    private void addFile(BytecodeCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            String partType, String partFilename, ResultHandle filePath) {
        ResultHandle fileNamePath = methodCreator.invokeInterfaceMethod(PATH_GET_FILENAME, filePath);
        ResultHandle fileName = partFilename != null ? methodCreator.load(partFilename)
                : methodCreator.invokeVirtualMethod(OBJECT_TO_STRING, fileNamePath);
        ResultHandle pathString = methodCreator.invokeVirtualMethod(OBJECT_TO_STRING, filePath);
        // they all default to plain/text except buffers/byte[]/Multi<Byte>/File/Path
        if (partType == null) {
            partType = MediaType.APPLICATION_OCTET_STREAM;
        }
        if (partType.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM)) {
            methodCreator.assign(multipartForm,
                    // MultipartForm#binaryFileUpload(String name, String filename, String pathname, String mediaType);
                    // filename = name
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ClientMultipartForm.class, "binaryFileUpload",
                                    ClientMultipartForm.class, String.class, String.class, String.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), fileName,
                            pathString, methodCreator.load(partType)));
        } else {
            methodCreator.assign(multipartForm,
                    // MultipartForm#textFileUpload(String name, String filename, String pathname, String mediaType);;
                    // filename = name
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ClientMultipartForm.class, "textFileUpload",
                                    ClientMultipartForm.class, String.class, String.class, String.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), fileName,
                            pathString, methodCreator.load(partType)));
        }
    }

    private ResultHandle primitiveToString(BytecodeCreator methodCreator, ResultHandle fieldValue, FieldInfo field) {
        PrimitiveType primitiveType = field.type().asPrimitiveType();
        switch (primitiveType.primitive()) {
            case BOOLEAN:
                return methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(String.class, "valueOf", String.class, boolean.class), fieldValue);
            case CHAR:
                return methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(String.class, "valueOf", String.class, char.class), fieldValue);
            case DOUBLE:
                return methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(String.class, "valueOf", String.class, double.class), fieldValue);
            case FLOAT:
                return methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(String.class, "valueOf", String.class, float.class), fieldValue);
            case INT:
                return methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(String.class, "valueOf", String.class, int.class), fieldValue);
            case LONG:
                return methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(String.class, "valueOf", String.class, long.class), fieldValue);
            default:
                throw new IllegalArgumentException("Unsupported primitive type in multipart form field: "
                        + field.declaringClass().name() + "." + field.name());
        }
    }

    private ResultHandle partFilenameHandle(BytecodeCreator methodCreator, String partFilename) {
        return partFilename != null ? methodCreator.load(partFilename) : methodCreator.loadNull();
    }

    private void addString(BytecodeCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            String partType, String partFilename, ResultHandle fieldValue) {
        if (MediaType.APPLICATION_OCTET_STREAM.equalsIgnoreCase(partType)) {
            methodCreator.assign(multipartForm,
                    // MultipartForm#stringFileUpload(String name, String filename, String content, String mediaType);
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ClientMultipartForm.class, "stringFileUpload",
                                    ClientMultipartForm.class, String.class, String.class, String.class,
                                    String.class),
                            multipartForm,
                            methodCreator.load(formParamName),
                            partFilename != null ? methodCreator.load(partFilename) : methodCreator.loadNull(),
                            fieldValue,
                            methodCreator.load(partType)));
        } else {
            methodCreator.assign(multipartForm,
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ClientMultipartForm.class, "attribute", ClientMultipartForm.class,
                                    String.class, String.class, String.class),
                            multipartForm, methodCreator.load(formParamName), fieldValue,
                            partFilenameHandle(methodCreator, partFilename)));
        }
    }

    private void addMultiAsFile(BytecodeCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            String partType, String partFilename,
            ResultHandle multi, String errorLocation) {
        // they all default to plain/text except buffers/byte[]/Multi<Byte>/File/Path
        if (partType == null) {
            partType = MediaType.APPLICATION_OCTET_STREAM;
        }
        String filename = partFilename;
        if (filename == null) {
            filename = formParamName;
        }

        if (partType.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM)) {
            methodCreator.assign(multipartForm,
                    // MultipartForm#binaryFileUpload(String name, String filename,  Multi<Byte> content, String mediaType);
                    // filename = name
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ClientMultipartForm.class, "multiAsBinaryFileUpload",
                                    ClientMultipartForm.class, String.class, String.class, Multi.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), methodCreator.load(filename),
                            multi, methodCreator.load(partType)));
        } else {
            methodCreator.assign(multipartForm,
                    // MultipartForm#multiAsTextFileUpload(String name, String filename, Multi<Byte> content, String mediaType)
                    // filename = name
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ClientMultipartForm.class, "multiAsTextFileUpload",
                                    ClientMultipartForm.class, String.class, String.class, Multi.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), methodCreator.load(filename),
                            multi, methodCreator.load(partType)));
        }
    }

    private void addBuffer(BytecodeCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            String partType, String partFilename, ResultHandle buffer, String errorLocation) {
        ResultHandle filenameHandle = partFilename != null ? methodCreator.load(partFilename)
                : methodCreator.load(formParamName);
        // they all default to plain/text except buffers/byte[]/Multi<Byte>/File/Path
        if (partType == null) {
            partType = MediaType.APPLICATION_OCTET_STREAM;
        }
        if (partType.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM)) {
            methodCreator.assign(multipartForm,
                    // MultipartForm#binaryFileUpload(String name, String filename, io.vertx.mutiny.core.buffer.Buffer content, String mediaType);
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ClientMultipartForm.class, "binaryFileUpload",
                                    ClientMultipartForm.class, String.class, String.class, Buffer.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), filenameHandle,
                            buffer, methodCreator.load(partType)));
        } else {
            methodCreator.assign(multipartForm,
                    // MultipartForm#textFileUpload(String name, String filename, io.vertx.mutiny.core.buffer.Buffer content, String mediaType)
                    methodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ClientMultipartForm.class, "textFileUpload",
                                    ClientMultipartForm.class, String.class, String.class, Buffer.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), filenameHandle,
                            buffer, methodCreator.load(partType)));
        }
    }

    private AssignableResultHandle createFormDataIfAbsent(BytecodeCreator methodCreator, AssignableResultHandle formValues,
            boolean multipart) {
        if (formValues == null) {
            if (multipart) {
                formValues = methodCreator.createVariable(QuarkusMultipartForm.class);
                methodCreator.assign(formValues,
                        methodCreator.newInstance(MethodDescriptor.ofConstructor(QuarkusMultipartForm.class)));
            } else {
                formValues = methodCreator.createVariable(MultivaluedMap.class);
                methodCreator.assign(formValues,
                        methodCreator.newInstance(MethodDescriptor.ofConstructor(MultivaluedHashMap.class)));
            }
        }
        return formValues;
    }

    private String[] parametersAsStringArray(MethodInfo subMethod) {
        List<Type> params = subMethod.parameterTypes();
        String[] result = new String[params.size()];
        int i = 0;
        for (Type param : params) {
            result[i++] = param.name().toString();
        }

        return result;
    }

    private String getHttpMethod(MethodInfo subMethod, String defaultMethod, Map<DotName, String> httpAnnotationToMethod) {

        for (Map.Entry<DotName, String> annoToMethod : httpAnnotationToMethod.entrySet()) {
            if (subMethod.annotation(annoToMethod.getKey()) != null) {
                return annoToMethod.getValue();
            }
        }

        for (Map.Entry<DotName, String> annoToMethod : httpAnnotationToMethod.entrySet()) {
            if (subMethod.declaringClass().declaredAnnotation(annoToMethod.getKey()) != null) {
                return annoToMethod.getValue();
            }
        }
        return defaultMethod;
    }

    private void handleReturn(ClassInfo restClientInterface, String defaultMediaType, String httpMethod, String[] consumes,
            MethodInfo jandexMethod, MethodCreator methodCreator, ResultHandle formParams,
            ResultHandle bodyValue, AssignableResultHandle builder, boolean multipart) {
        Type returnType = jandexMethod.returnType();
        ReturnCategory returnCategory = ReturnCategory.BLOCKING;

        String simpleReturnType = returnType.name().toString();
        ResultHandle genericReturnType = null;

        if (returnType.kind() == PARAMETERIZED_TYPE) {
            ParameterizedType paramType = returnType.asParameterizedType();
            if (ASYNC_RETURN_TYPES.contains(paramType.name())) {
                returnCategory = paramType.name().equals(COMPLETION_STAGE)
                        ? ReturnCategory.COMPLETION_STAGE
                        : paramType.name().equals(MULTI) || paramType.name().equals(REST_MULTI)
                                ? ReturnCategory.MULTI
                                : ReturnCategory.UNI;

                // the async types have one type argument:
                if (paramType.arguments().isEmpty()) {
                    simpleReturnType = Object.class.getName();
                } else {
                    Type type = paramType.arguments().get(0);
                    if (type.kind() == PARAMETERIZED_TYPE) {
                        genericReturnType = createGenericTypeFromParameterizedType(methodCreator,
                                type.asParameterizedType());
                    } else {
                        simpleReturnType = type.name().toString();
                    }
                }
            } else {
                genericReturnType = createGenericTypeFromParameterizedType(methodCreator, paramType);
            }
        }
        Integer continuationIndex = null;
        //TODO: there should be an SPI for this
        if (returnCategory == ReturnCategory.BLOCKING) {
            List<Type> parameters = jandexMethod.parameterTypes();
            if (!parameters.isEmpty()) {
                Type lastParamType = parameters.get(parameters.size() - 1);
                if (lastParamType.name().equals(CONTINUATION)) {
                    continuationIndex = parameters.size() - 1;
                    returnCategory = ReturnCategory.COROUTINE;

                    if (!QuarkusClassLoader.isClassPresentAtRuntime(UNI_KT.toString())) {
                        //TODO: make this automatic somehow
                        throw new RuntimeException("Suspendable rest client method" + jandexMethod + " is present on class "
                                + jandexMethod.declaringClass()
                                + " however io.smallrye.reactive:mutiny-kotlin is not detected. Please add a dependency on this artifact.");
                    }

                    //we infer the return type from the param type of the continuation
                    Type type = lastParamType.asParameterizedType().arguments().get(0);
                    for (;;) {
                        if (type.kind() == PARAMETERIZED_TYPE) {
                            genericReturnType = createGenericTypeFromParameterizedType(methodCreator,
                                    type.asParameterizedType());
                            break;
                        } else if (type.kind() == Type.Kind.WILDCARD_TYPE) {
                            if (type.asWildcardType().extendsBound().name().equals(OBJECT)) {
                                type = type.asWildcardType().superBound();
                            } else {
                                type = type.asWildcardType().extendsBound();
                            }
                        } else {
                            simpleReturnType = type.name().toString();
                            break;
                        }
                    }
                }
            }
        }

        ResultHandle result;

        String mediaTypeValue = defaultMediaType;

        // if a JAXRS method throws an exception, unwrap the ProcessingException and throw the exception instead
        // Similarly with RuntimeException's
        TryBlock tryBlock = methodCreator.tryBlock();

        List<Type> exceptionTypes = jandexMethod.exceptions();
        Set<String> exceptions = new LinkedHashSet<>();
        for (Type exceptionType : exceptionTypes) {
            exceptions.add(exceptionType.name().toString());
        }
        if (!exceptions.contains(Exception.class.getName()) && !exceptions.contains(Throwable.class.getName())) {
            exceptions.add(RuntimeException.class.getName());
        }

        CatchBlockCreator catchBlock = tryBlock.addCatch(ProcessingException.class);
        ResultHandle caughtException = catchBlock.getCaughtException();
        ResultHandle cause = catchBlock.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Throwable.class, "getCause", Throwable.class),
                caughtException);
        for (String exception : exceptions) {
            catchBlock.ifTrue(catchBlock.instanceOf(cause, exception))
                    .trueBranch().throwException(cause);
        }

        catchBlock.throwException(caughtException);

        if (bodyValue != null || formParams != null) {
            if (countNonNulls(bodyValue, formParams) > 1) {
                throw new IllegalArgumentException("Attempt to pass at least two of form " +
                        "or regular entity as a request body in " +
                        restClientInterface.name().toString() + "#" + jandexMethod.name());
            }

            if (consumes != null && consumes.length > 0) {

                if (consumes.length > 1) {
                    throw new IllegalArgumentException(
                            "Multiple `@Consumes` values used in a MicroProfile Rest Client: " +
                                    restClientInterface.name().toString()
                                    + " Unable to determine a single `Content-Type`.");
                }
                mediaTypeValue = consumes[0];
            } else if (formParams != null) {
                mediaTypeValue = multipart ? MediaType.MULTIPART_FORM_DATA : MediaType.APPLICATION_FORM_URLENCODED;
            }

            if (mediaTypeValue.equals(defaultMediaType)) {
                // we want to keep the default type around in order to ensure that Header manipulation code can override it without issue
                tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "property", Invocation.Builder.class, String.class,
                                Object.class),
                        builder, tryBlock.load(DEFAULT_CONTENT_TYPE_PROP), tryBlock.load(mediaTypeValue));
            }

            ResultHandle mediaType = tryBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(MediaType.class, "valueOf", MediaType.class, String.class),
                    tryBlock.load(mediaTypeValue));

            ResultHandle entity = tryBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Entity.class, "entity", Entity.class, Object.class, MediaType.class),
                    bodyValue != null ? bodyValue : formParams,
                    mediaType);

            if (returnCategory == ReturnCategory.COMPLETION_STAGE) {
                ResultHandle async = tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "async", AsyncInvoker.class),
                        builder);
                // with entity
                if (genericReturnType != null) {
                    result = tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(CompletionStageRxInvoker.class, "method",
                                    CompletionStage.class, String.class,
                                    Entity.class, GenericType.class),
                            async, tryBlock.load(httpMethod), entity,
                            genericReturnType);
                } else {
                    result = tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(CompletionStageRxInvoker.class, "method", CompletionStage.class,
                                    String.class,
                                    Entity.class, Class.class),
                            async, tryBlock.load(httpMethod), entity,
                            tryBlock.loadClassFromTCCL(simpleReturnType));
                }
            } else if (returnCategory == ReturnCategory.UNI || returnCategory == ReturnCategory.COROUTINE) {
                ResultHandle rx = tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "rx", RxInvoker.class, Class.class),
                        builder, tryBlock.loadClassFromTCCL(UniInvoker.class));
                ResultHandle uniInvoker = tryBlock.checkCast(rx, UniInvoker.class);
                // with entity
                if (genericReturnType != null) {
                    result = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(UniInvoker.class, "method",
                                    Uni.class, String.class,
                                    Entity.class, GenericType.class),
                            uniInvoker, tryBlock.load(httpMethod), entity,
                            genericReturnType);
                } else {
                    result = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(UniInvoker.class, "method",
                                    Uni.class, String.class,
                                    Entity.class, Class.class),
                            uniInvoker, tryBlock.load(httpMethod), entity,
                            tryBlock.loadClassFromTCCL(simpleReturnType));
                }
                if (returnCategory == ReturnCategory.COROUTINE) {
                    result = tryBlock.invokeStaticMethod(
                            MethodDescriptor.ofMethod(UNI_KT.toString(), "awaitSuspending", Object.class, Uni.class,
                                    CONTINUATION.toString()),
                            result, tryBlock.getMethodParam(continuationIndex));
                }
            } else if (returnCategory == ReturnCategory.MULTI) {
                ResultHandle rx = tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "rx", RxInvoker.class, Class.class),
                        builder, tryBlock.loadClassFromTCCL(MultiInvoker.class));
                ResultHandle multiInvoker = tryBlock.checkCast(rx, MultiInvoker.class);
                // with entity
                if (genericReturnType != null) {
                    result = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(MultiInvoker.class, "method",
                                    Multi.class, String.class,
                                    Entity.class, GenericType.class),
                            multiInvoker, tryBlock.load(httpMethod), entity,
                            genericReturnType);
                } else {
                    result = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractRxInvoker.class, "method",
                                    Object.class, String.class,
                                    Entity.class, Class.class),
                            multiInvoker, tryBlock.load(httpMethod), entity,
                            tryBlock.loadClassFromTCCL(simpleReturnType));
                }
            } else {
                if (genericReturnType != null) {
                    result = tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                    Entity.class, GenericType.class),
                            builder, tryBlock.load(httpMethod), entity,
                            genericReturnType);
                } else {
                    result = tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                    Entity.class, Class.class),
                            builder, tryBlock.load(httpMethod), entity,
                            tryBlock.loadClassFromTCCL(simpleReturnType));
                }
            }
        } else {

            if (returnCategory == ReturnCategory.COMPLETION_STAGE) {
                ResultHandle async = tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "async", AsyncInvoker.class),
                        builder);
                if (genericReturnType != null) {
                    result = tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(CompletionStageRxInvoker.class, "method",
                                    CompletionStage.class, String.class,
                                    GenericType.class),
                            async, tryBlock.load(httpMethod), genericReturnType);
                } else {
                    result = tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(CompletionStageRxInvoker.class, "method", CompletionStage.class,
                                    String.class,
                                    Class.class),
                            async, tryBlock.load(httpMethod),
                            tryBlock.loadClassFromTCCL(simpleReturnType));
                }
            } else if (returnCategory == ReturnCategory.UNI || returnCategory == ReturnCategory.COROUTINE) {
                ResultHandle rx = tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "rx", RxInvoker.class, Class.class),
                        builder, tryBlock.loadClassFromTCCL(UniInvoker.class));
                ResultHandle uniInvoker = tryBlock.checkCast(rx, UniInvoker.class);
                if (genericReturnType != null) {
                    result = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(UniInvoker.class, "method",
                                    Uni.class, String.class,
                                    GenericType.class),
                            uniInvoker, tryBlock.load(httpMethod), genericReturnType);
                } else {
                    result = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(UniInvoker.class, "method",
                                    Uni.class, String.class,
                                    Class.class),
                            uniInvoker, tryBlock.load(httpMethod),
                            tryBlock.loadClassFromTCCL(simpleReturnType));
                }
                if (returnCategory == ReturnCategory.COROUTINE) {
                    result = tryBlock.invokeStaticMethod(
                            MethodDescriptor.ofMethod(UNI_KT.toString(), "awaitSuspending", Object.class, Uni.class,
                                    CONTINUATION.toString()),
                            result, tryBlock.getMethodParam(continuationIndex));
                }
            } else if (returnCategory == ReturnCategory.MULTI) {
                ResultHandle rx = tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "rx", RxInvoker.class, Class.class),
                        builder, tryBlock.loadClassFromTCCL(MultiInvoker.class));
                ResultHandle multiInvoker = tryBlock.checkCast(rx, MultiInvoker.class);
                if (genericReturnType != null) {
                    result = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractRxInvoker.class, "method",
                                    Object.class, String.class,
                                    GenericType.class),
                            multiInvoker, tryBlock.load(httpMethod), genericReturnType);
                } else {
                    result = tryBlock.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractRxInvoker.class, "method",
                                    Object.class, String.class,
                                    Class.class),
                            multiInvoker, tryBlock.load(httpMethod),
                            tryBlock.loadClassFromTCCL(simpleReturnType));
                }
            } else {
                if (genericReturnType != null) {
                    result = tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                    GenericType.class),
                            builder, tryBlock.load(httpMethod), genericReturnType);
                } else {
                    result = tryBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                    Class.class),
                            builder, tryBlock.load(httpMethod),
                            tryBlock.loadClassFromTCCL(simpleReturnType));
                }
            }
        }
        tryBlock.returnValue(result);
    }

    private int countNonNulls(Object... objects) {
        int result = 0;
        for (Object object : objects) {
            if (object != null) {
                result++;
            }
        }

        return result;
    }

    private ResultHandle createGenericTypeFromParameterizedType(MethodCreator methodCreator,
            ParameterizedType parameterizedType2) {
        ResultHandle genericReturnType;
        ResultHandle currentThread = methodCreator.invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD);
        ResultHandle tccl = methodCreator.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL, currentThread);
        ResultHandle parameterizedType = Types.getParameterizedType(methodCreator, tccl, parameterizedType2);
        genericReturnType = methodCreator.newInstance(
                MethodDescriptor.ofConstructor(GenericType.class, java.lang.reflect.Type.class),
                parameterizedType);
        return genericReturnType;
    }

    private Optional<MethodInfo> getJavaMethod(ClassInfo interfaceClass, ResourceMethod method,
            MethodParameter[] parameters, IndexView index) {

        for (MethodInfo methodInfo : interfaceClass.methods()) {
            if (methodInfo.name().equals(method.getName()) && methodInfo.parametersCount() == parameters.length) {
                boolean matches = true;
                for (int i = 0; i < parameters.length; i++) {
                    MethodParameter actualParam = parameters[i];
                    Type parameterType = methodInfo.parameterType(i);
                    String declaredType = actualParam.declaredType != null ? actualParam.declaredType : actualParam.type;
                    if (!declaredType.equals(parameterType.name().toString())) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return Optional.of(methodInfo);
                }
            }
        }

        Optional<MethodInfo> maybeMethod = Optional.empty();
        for (DotName interfaceName : interfaceClass.interfaceNames()) {
            maybeMethod = getJavaMethod(index.getClassByName(interfaceName), method, parameters,
                    index);
            if (maybeMethod.isPresent()) {
                break;
            }
        }
        return maybeMethod;
    }

    private AssignableResultHandle addBeanParamData(MethodInfo jandexMethod,
            BytecodeCreator methodCreator,
            // Invocation.Builder executePut$$enrichInvocationBuilder${noOfBeanParam}(Invocation.Builder)
            BytecodeCreator invocationBuilderEnricher,
            AssignableResultHandle invocationBuilder,
            ClassRestClientContext classContext,
            List<Item> beanParamItems,
            ResultHandle param,
            // can only be used in the current method, not in `invocationBuilderEnricher`
            AssignableResultHandle target,
            IndexView index,
            String restClientInterfaceClassName,
            ResultHandle client,
            ResultHandle invocationEnricherClient,
            // this client or containing client if this is a sub-client
            AssignableResultHandle formParams,
            Supplier<FieldDescriptor> descriptorsField,
            boolean multipart, String beanParamClass) {
        // Form params collector must be initialized at method root level before any inner blocks that may use it
        if (areFormParamsDefinedIn(beanParamItems)) {
            formParams = createFormDataIfAbsent(methodCreator, formParams, multipart);
        }

        addSubBeanParamData(jandexMethod, methodCreator, invocationBuilderEnricher, invocationBuilder, classContext,
                beanParamItems, param, target,
                index, restClientInterfaceClassName, client, invocationEnricherClient, formParams,
                descriptorsField, multipart, beanParamClass);

        return formParams;
    }

    private void addSubBeanParamData(MethodInfo jandexMethod, BytecodeCreator methodCreator,
            // Invocation.Builder executePut$$enrichInvocationBuilder${noOfBeanParam}(Invocation.Builder)
            BytecodeCreator invocationBuilderEnricher,
            AssignableResultHandle invocationBuilder,
            ClassRestClientContext classContext,
            List<Item> beanParamItems,
            ResultHandle param,
            // can only be used in the current method, not in `invocationBuilderEnricher`
            AssignableResultHandle target,
            IndexView index,
            String restClientInterfaceClassName,
            ResultHandle client,
            // this client or containing client if this is a sub-client
            ResultHandle invocationEnricherClient,
            AssignableResultHandle formParams,
            Supplier<FieldDescriptor> beanParamDescriptorField,
            boolean multipart, String beanParamClass) {
        BytecodeCreator creator = methodCreator.ifNotNull(param).trueBranch();
        BytecodeCreator invoEnricher = invocationBuilderEnricher.ifNotNull(invocationBuilderEnricher.getMethodParam(1))
                .trueBranch();

        boolean encodingEnabled = true;
        for (Item item : beanParamItems) {

            if (encodingEnabled && item.isEncoded()) {
                creator.assign(target, disableEncodingForWebTarget(creator, target));
                encodingEnabled = false;
            } else if (!encodingEnabled && !item.isEncoded()) {
                creator.assign(target, enableEncodingForWebTarget(creator, target));
                encodingEnabled = true;
            }

            switch (item.type()) {
                case BEAN_PARAM:
                    BeanParamItem beanParamItem = (BeanParamItem) item;
                    ResultHandle beanParamElementHandle = beanParamItem.extract(creator, param);
                    Supplier<FieldDescriptor> newBeanParamDescriptorField = classContext
                            .getLazyBeanParameterDescriptors(beanParamItem.className());
                    addSubBeanParamData(jandexMethod, creator, invoEnricher, invocationBuilder, classContext,
                            beanParamItem.items(), beanParamElementHandle, target, index, restClientInterfaceClassName, client,
                            invocationEnricherClient, formParams,
                            newBeanParamDescriptorField, multipart,
                            beanParamItem.className());
                    break;
                case QUERY_PARAM:
                    QueryParamItem queryParam = (QueryParamItem) item;
                    creator.assign(target,
                            addQueryParam(jandexMethod, creator, target, queryParam.name(),
                                    queryParam.extract(creator, param),
                                    queryParam.getValueType(),
                                    index, client,
                                    getGenericTypeFromParameter(creator, beanParamDescriptorField, item.fieldName()),
                                    getAnnotationsFromParameter(creator, beanParamDescriptorField, item.fieldName())));
                    break;
                case COOKIE:
                    CookieParamItem cookieParam = (CookieParamItem) item;
                    addCookieParam(invoEnricher, invocationBuilder,
                            cookieParam.getCookieName(),
                            cookieParam.extract(invoEnricher, invoEnricher.getMethodParam(1)),
                            cookieParam.getParamType(), invocationEnricherClient,
                            getGenericTypeFromParameter(invoEnricher, beanParamDescriptorField, item.fieldName()),
                            getAnnotationsFromParameter(invoEnricher, beanParamDescriptorField, item.fieldName()));
                    break;
                case HEADER_PARAM:
                    HeaderParamItem headerParam = (HeaderParamItem) item;
                    addHeaderParam(invoEnricher, invocationBuilder,
                            headerParam.getHeaderName(),
                            headerParam.extract(invoEnricher, invoEnricher.getMethodParam(1)),
                            headerParam.getParamType(), invocationEnricherClient,
                            getGenericTypeFromParameter(invoEnricher, beanParamDescriptorField, item.fieldName()),
                            getAnnotationsFromParameter(invoEnricher, beanParamDescriptorField, item.fieldName()));
                    break;
                case PATH_PARAM:
                    PathParamItem pathParam = (PathParamItem) item;
                    addPathParam(creator, target,
                            pathParam.getPathParamName(),
                            pathParam.extract(creator, param), pathParam.getParamType(), client,
                            getGenericTypeFromParameter(creator, beanParamDescriptorField, item.fieldName()),
                            getAnnotationsFromParameter(creator, beanParamDescriptorField, item.fieldName()));
                    break;
                case FORM_PARAM:
                    FormParamItem formParam = (FormParamItem) item;
                    addFormParam(creator, formParam.getFormParamName(), formParam.extract(creator, param),
                            formParam.getParamType(), formParam.getParamSignature(), restClientInterfaceClassName, client,
                            formParams,
                            getGenericTypeFromParameter(creator, beanParamDescriptorField, item.fieldName()),
                            getAnnotationsFromParameter(creator, beanParamDescriptorField, item.fieldName()),
                            multipart, formParam.getMimeType(), formParam.getFileName(),
                            beanParamClass + "." + formParam.getSourceName());
                    break;
                default:
                    throw new IllegalStateException("Unimplemented");
            }
        }
    }

    private ResultHandle getGenericTypeFromParameter(BytecodeCreator creator, Supplier<FieldDescriptor> supplier,
            String name) {
        // Will return Map<String, ParameterDescriptorFromClassSupplier.ParameterDescriptor>
        ResultHandle map = creator.invokeInterfaceMethod(ofMethod(Supplier.class, "get", Object.class),
                creator.readStaticField(supplier.get()));
        // Will return ParameterDescriptorFromClassSupplier.ParameterDescriptor;
        ResultHandle value = creator.invokeInterfaceMethod(ofMethod(Map.class, "get", Object.class, Object.class),
                map, creator.load(name));
        // if (value != null) return value.genericType;
        AssignableResultHandle genericType = creator.createVariable(java.lang.reflect.Type.class);
        BranchResult ifBranch = creator.ifNotNull(value);
        BytecodeCreator ifNotNull = ifBranch.trueBranch();
        ifNotNull.assign(genericType, ifNotNull.readInstanceField(
                FieldDescriptor.of(ParameterDescriptorFromClassSupplier.ParameterDescriptor.class, "genericType",
                        java.lang.reflect.Type.class),
                value));
        // if (value == null) return null;
        BytecodeCreator ifNull = ifBranch.falseBranch();
        ifNull.assign(genericType, ifNull.loadNull());
        return genericType;
    }

    private ResultHandle getGenericTypeFromArray(BytecodeCreator creator, Supplier<FieldDescriptor> supplier,
            int paramIdx) {
        ResultHandle value = creator.invokeInterfaceMethod(ofMethod(Supplier.class, "get", Object.class),
                creator.readStaticField(supplier.get()));
        return creator.readArrayValue(creator.checkCast(value, java.lang.reflect.Type[].class), paramIdx);
    }

    private ResultHandle getAnnotationsFromParameter(BytecodeCreator creator, Supplier<FieldDescriptor> supplier,
            String name) {
        // Will return Map<String, ParameterDescriptorFromClassSupplier.ParameterDescriptor>
        ResultHandle map = creator.invokeInterfaceMethod(ofMethod(Supplier.class, "get", Object.class),
                creator.readStaticField(supplier.get()));
        // Will return ParameterDescriptorFromClassSupplier.ParameterDescriptor;
        ResultHandle value = creator.invokeInterfaceMethod(ofMethod(Map.class, "get", Object.class, Object.class),
                map, creator.load(name));
        // if (value != null) return value.genericType;
        AssignableResultHandle annotations = creator.createVariable(Annotation[].class);
        BranchResult ifBranch = creator.ifNotNull(value);
        BytecodeCreator ifNotNull = ifBranch.trueBranch();
        ifNotNull.assign(annotations, ifNotNull.readInstanceField(
                FieldDescriptor.of(ParameterDescriptorFromClassSupplier.ParameterDescriptor.class, "annotations",
                        Annotation[].class),
                value));
        // if (value == null) return null;
        BytecodeCreator ifNull = ifBranch.falseBranch();
        ifNull.assign(annotations, ifNull.loadNull());
        return annotations;
    }

    private ResultHandle getAnnotationsFromArray(BytecodeCreator creator, Supplier<FieldDescriptor> supplier,
            int paramIdx) {
        ResultHandle value = creator.invokeInterfaceMethod(ofMethod(Supplier.class, "get", Object.class),
                creator.readStaticField(supplier.get()));
        return creator.readArrayValue(creator.checkCast(value, Annotation[][].class), paramIdx);
    }

    private boolean areFormParamsDefinedIn(List<Item> beanParamItems) {
        for (Item item : beanParamItems) {
            switch (item.type()) {
                case FORM_PARAM:
                    return true;
                case BEAN_PARAM:
                    if (areFormParamsDefinedIn(((BeanParamItem) item).items())) {
                        return true;
                    }
                    break;
            }
        }

        return false;
    }

    // takes a result handle to target as one of the parameters, returns a result handle to a modified target
    private ResultHandle addQueryParam(MethodInfo jandexMethod, BytecodeCreator methodCreator,
            ResultHandle webTarget,
            String paramName,
            ResultHandle queryParamHandle,
            Type type,
            IndexView index,
            // this client or containing client if we're in a subresource
            ResultHandle client,
            ResultHandle genericType,
            ResultHandle paramAnnotations) {

        AssignableResultHandle result = methodCreator.createVariable(WebTarget.class);
        BranchResult isParamNull = methodCreator.ifNull(queryParamHandle);
        BytecodeCreator notNullParam = isParamNull.falseBranch();
        if (isMap(type, index)) {
            var resolvesTypes = resolveMapTypes(type, index, jandexMethod);
            var keyType = resolvesTypes.getKey();
            if (!ResteasyReactiveDotNames.STRING.equals(keyType.name())) {
                throw new IllegalArgumentException(
                        "Map parameter types must have String keys. Offending method is: " + jandexMethod);
            }
            notNullParam.assign(result, webTarget);
            // Loop through the keys
            ResultHandle keySet = notNullParam.invokeInterfaceMethod(ofMethod(Map.class, "keySet", Set.class),
                    queryParamHandle);
            ResultHandle keysIterator = notNullParam.invokeInterfaceMethod(
                    ofMethod(Set.class, "iterator", Iterator.class), keySet);
            BytecodeCreator loopCreator = notNullParam.whileLoop(c -> iteratorHasNext(c, keysIterator)).block();
            ResultHandle key = loopCreator.invokeInterfaceMethod(
                    ofMethod(Iterator.class, "next", Object.class), keysIterator);
            // get the value and convert
            ResultHandle value = loopCreator.invokeInterfaceMethod(ofMethod(Map.class, "get", Object.class, Object.class),
                    queryParamHandle, key);
            var valueType = resolvesTypes.getValue();
            String componentType = valueType.name().toString();
            ResultHandle paramArray;
            if (isCollection(valueType, index)) {
                if (valueType.kind() == PARAMETERIZED_TYPE) {
                    Type paramType = valueType.asParameterizedType().arguments().get(0);
                    if ((paramType.kind() == CLASS) || (paramType.kind() == PARAMETERIZED_TYPE)) {
                        componentType = paramType.name().toString();
                    }
                }
                if (componentType == null) {
                    componentType = DotNames.OBJECT.toString();
                }
                paramArray = loopCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(ToObjectArray.class, "collection", Object[].class, Collection.class),
                        value);
            } else {
                paramArray = loopCreator
                        .invokeStaticMethod(ofMethod(ToObjectArray.class, "value", Object[].class, Object.class),
                                value);
            }
            // get the new WebTarget
            addQueryParamToWebTarget(loopCreator, key, result, client, genericType, paramAnnotations,
                    paramArray, componentType, result);
        } else {
            ResultHandle paramArray;
            String componentType = null;
            if (type.kind() == Type.Kind.ARRAY) {
                componentType = type.asArrayType().constituent().name().toString();
                paramArray = notNullParam.checkCast(queryParamHandle, Object[].class);
            } else if (isCollection(type, index)) {
                if (type.kind() == PARAMETERIZED_TYPE) {
                    Type paramType = type.asParameterizedType().arguments().get(0);
                    if ((paramType.kind() == CLASS) || (paramType.kind() == PARAMETERIZED_TYPE)) {
                        componentType = paramType.name().toString();
                    }
                }
                if (componentType == null) {
                    componentType = DotNames.OBJECT.toString();
                }
                paramArray = notNullParam.invokeStaticMethod(
                        MethodDescriptor.ofMethod(ToObjectArray.class, "collection", Object[].class, Collection.class),
                        queryParamHandle);
            } else {
                componentType = type.name().toString();
                paramArray = notNullParam.invokeStaticMethod(
                        MethodDescriptor.ofMethod(ToObjectArray.class, "value", Object[].class, Object.class),
                        queryParamHandle);
            }

            addQueryParamToWebTarget(notNullParam, notNullParam.load(paramName), webTarget, client, genericType,
                    paramAnnotations, paramArray, componentType, result);
        }

        isParamNull.trueBranch().assign(result, webTarget);

        return result;
    }

    private Map.Entry<Type, Type> resolveMapTypes(Type type, IndexView index, MethodInfo jandexMethod) {
        if (type.name().equals(ResteasyReactiveDotNames.MAP)) {
            if (type.kind() != PARAMETERIZED_TYPE) {
                throw new IllegalArgumentException(
                        "Raw Map parameter types are not supported. Offending method is: " + jandexMethod);
            }
            var parameterizedType = type.asParameterizedType();
            var arguments = parameterizedType.arguments();
            return new AbstractMap.SimpleEntry<>(arguments.get(0), arguments.get(1));
        } else if (type.name().equals(ResteasyReactiveDotNames.MULTI_VALUED_MAP)) {
            if (type.kind() != PARAMETERIZED_TYPE) {
                throw new IllegalArgumentException(
                        "Raw MultivaluedMap parameter types are not supported. Offending method is: " + jandexMethod);
            }
            var parameterizedType = type.asParameterizedType();
            var arguments = parameterizedType.arguments();
            return new AbstractMap.SimpleEntry<>(arguments.get(0), ParameterizedType.create(ResteasyReactiveDotNames.LIST,
                    new Type[] { arguments.get(1) }, null));
        }
        // TODO: we could support this if necessary by looking up the resolved types via JandexUtil.resolveTypeParameters
        throw new IllegalArgumentException("Unsupported Map type '" + type.name() + "'. Offending method is: " + jandexMethod);
    }

    private BranchResult iteratorHasNext(BytecodeCreator creator, ResultHandle iterator) {
        return creator.ifTrue(
                creator.invokeInterfaceMethod(ofMethod(Iterator.class, "hasNext", boolean.class), iterator));
    }

    private void addQueryParamToWebTarget(BytecodeCreator creator, ResultHandle paramName,
            ResultHandle webTarget,
            ResultHandle client, ResultHandle genericType,
            ResultHandle paramAnnotations, ResultHandle paramArray,
            String componentType,
            AssignableResultHandle resultVariable) {
        ResultHandle convertedParamArray = creator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(RestClientBase.class, "convertParamArray", Object[].class, Object[].class,
                        Class.class, java.lang.reflect.Type.class, Annotation[].class),
                client, paramArray, creator.loadClassFromTCCL(componentType), genericType, paramAnnotations);

        creator.assign(resultVariable, creator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(WebTarget.class, "queryParam", WebTarget.class,
                        String.class, Object[].class),
                webTarget, paramName, convertedParamArray));
    }

    private boolean isCollection(Type type, IndexView index) {
        return isAssignableFrom(COLLECTION, type.name(), index);
    }

    private boolean isMap(Type type, IndexView index) {
        return isAssignableFrom(MAP, type.name(), index);
    }

    private void addHeaderParam(BytecodeCreator invoBuilderEnricher, AssignableResultHandle invocationBuilder,
            String paramName, ResultHandle headerParamHandle, String paramType, ResultHandle client,
            ResultHandle genericType, ResultHandle annotations) {

        BytecodeCreator notNullValue = invoBuilderEnricher.ifNull(headerParamHandle).falseBranch();

        headerParamHandle = notNullValue.invokeVirtualMethod(
                MethodDescriptor.ofMethod(RestClientBase.class, "convertParam", Object.class,
                        Object.class, Class.class, java.lang.reflect.Type.class, Annotation[].class),
                client, headerParamHandle,
                notNullValue.loadClassFromTCCL(paramType), genericType, annotations);

        notNullValue.assign(invocationBuilder,
                notNullValue.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "header", Invocation.Builder.class,
                                String.class, Object.class),
                        invocationBuilder, notNullValue.load(paramName), headerParamHandle));
    }

    private void addPathParam(BytecodeCreator methodCreator, AssignableResultHandle methodTarget,
            String paramName, ResultHandle pathParamHandle, String parameterType, ResultHandle client,
            ResultHandle genericType, ResultHandle parameterAnnotations) {
        ResultHandle handle = methodCreator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(RestClientBase.class, "convertParam", Object.class,
                        Object.class, Class.class, java.lang.reflect.Type.class, Annotation[].class),
                client, pathParamHandle,
                methodCreator.loadClassFromTCCL(parameterType), genericType, parameterAnnotations);
        methodCreator.assign(methodTarget,
                methodCreator.invokeInterfaceMethod(WEB_TARGET_RESOLVE_TEMPLATE_METHOD,
                        methodTarget,
                        methodCreator.load(paramName), handle));
    }

    private void addFormParam(BytecodeCreator methodCreator, String paramName, ResultHandle formParamHandle,
            String parameterType, String parameterGenericType,
            String restClientInterfaceClassName, ResultHandle client, AssignableResultHandle formParams,
            ResultHandle genericType,
            ResultHandle parameterAnnotations, boolean multipart,
            String partType, String partFilename, String errorLocation) {
        if (multipart) {
            handleMultipartField(paramName, partType, partFilename, parameterType, parameterGenericType, formParamHandle,
                    formParams, methodCreator,
                    client, restClientInterfaceClassName, parameterAnnotations, genericType,
                    errorLocation);
        } else {
            BytecodeCreator notNullValue = methodCreator.ifNull(formParamHandle).falseBranch();
            ResultHandle convertedFormParam = convertParamToString(notNullValue, client, formParamHandle, parameterType,
                    genericType, parameterAnnotations);
            BytecodeCreator parameterIsStringBranch = checkStringParam(notNullValue, convertedFormParam,
                    restClientInterfaceClassName, errorLocation);
            parameterIsStringBranch.invokeInterfaceMethod(MULTIVALUED_MAP_ADD, formParams,
                    notNullValue.load(paramName), convertedFormParam);
        }
    }

    private BytecodeCreator checkStringParam(BytecodeCreator notNullValue, ResultHandle convertedFormParam,
            String restClientInterfaceClassName, String errorLocation) {
        ResultHandle isString = notNullValue.instanceOf(convertedFormParam, String.class);
        BranchResult isStringBranch = notNullValue.ifTrue(isString);
        isStringBranch.falseBranch().throwException(IllegalStateException.class,
                "Form element '" + errorLocation
                        + "' could not be converted to 'String' for REST Client interface '"
                        + restClientInterfaceClassName + "'. A proper implementation of '"
                        + ParamConverter.class.getName() + "' needs to be returned by a '"
                        + ParamConverterProvider.class.getName()
                        + "' that is registered with the client via the @RegisterProvider annotation on the REST Client interface.");
        return isStringBranch.trueBranch();
    }

    private ResultHandle convertParamToString(BytecodeCreator notNullValue, ResultHandle client,
            ResultHandle formParamHandle, String parameterType,
            ResultHandle genericType, ResultHandle parameterAnnotations) {
        return notNullValue.invokeVirtualMethod(
                MethodDescriptor.ofMethod(RestClientBase.class, "convertParam", Object.class,
                        Object.class, Class.class, java.lang.reflect.Type.class, Annotation[].class),
                client, formParamHandle,
                notNullValue.loadClassFromTCCL(parameterType), genericType, parameterAnnotations);
    }

    private void addCookieParam(BytecodeCreator invoBuilderEnricher, AssignableResultHandle invocationBuilder,
            String paramName, ResultHandle cookieParamHandle, String paramType, ResultHandle client,
            ResultHandle genericType, ResultHandle annotations) {

        BytecodeCreator notNullValue = invoBuilderEnricher.ifNull(cookieParamHandle).falseBranch();

        cookieParamHandle = notNullValue.invokeVirtualMethod(
                MethodDescriptor.ofMethod(RestClientBase.class, "convertParam", Object.class,
                        Object.class, Class.class, java.lang.reflect.Type.class, Annotation[].class),
                client, cookieParamHandle,
                notNullValue.loadClassFromTCCL(paramType), genericType, annotations);
        notNullValue.assign(invocationBuilder,
                notNullValue.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "cookie", Invocation.Builder.class, String.class,
                                String.class),
                        invocationBuilder, notNullValue.load(paramName), cookieParamHandle));
    }

    private enum ReturnCategory {
        BLOCKING,
        COMPLETION_STAGE,
        UNI,
        MULTI,
        REST_MULTI,
        COROUTINE
    }

    private static class SubResourceParameter {
        final MethodParameter methodParameter;
        final String typeName;

        final Type type;
        final FieldDescriptor field;

        final Supplier<FieldDescriptor> paramAnnotationsField;

        final Supplier<FieldDescriptor> genericsParametersField;

        final int paramIndex;

        private SubResourceParameter(MethodParameter methodParameter, String typeName, Type type, FieldDescriptor field,
                Supplier<FieldDescriptor> paramAnnotationsField, Supplier<FieldDescriptor> genericsParametersField,
                int paramIndex) {
            this.methodParameter = methodParameter;
            this.typeName = typeName;
            this.type = type;
            this.field = field;
            this.paramAnnotationsField = paramAnnotationsField;
            this.genericsParametersField = genericsParametersField;
            this.paramIndex = paramIndex;
        }
    }

}
