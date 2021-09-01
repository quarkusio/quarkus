package io.quarkus.jaxrs.client.reactive.deployment;

import static io.quarkus.deployment.Feature.JAXRS_CLIENT_REACTIVE;
import static org.jboss.resteasy.reactive.common.processor.EndpointIndexer.extractProducesConsumesValues;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.CONSUMES;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OBJECT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.UNI;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
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
import org.jboss.resteasy.reactive.client.handlers.ClientObservabilityHandler;
import org.jboss.resteasy.reactive.client.impl.AsyncInvokerImpl;
import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.UniInvoker;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.client.processor.beanparam.BeanParamItem;
import org.jboss.resteasy.reactive.client.processor.beanparam.ClientBeanParamInfo;
import org.jboss.resteasy.reactive.client.processor.beanparam.CookieParamItem;
import org.jboss.resteasy.reactive.client.processor.beanparam.HeaderParamItem;
import org.jboss.resteasy.reactive.client.processor.beanparam.Item;
import org.jboss.resteasy.reactive.client.processor.beanparam.QueryParamItem;
import org.jboss.resteasy.reactive.client.processor.scanning.ClientEndpointIndexer;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.ResponseBuilderFactory;
import org.jboss.resteasy.reactive.common.core.Serialisers;
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
import org.jboss.resteasy.reactive.common.processor.HashUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.arc.processor.Types;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.AssignableResultHandle;
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
import io.quarkus.jaxrs.client.reactive.runtime.MultipartFormUtils;
import io.quarkus.jaxrs.client.reactive.runtime.ToObjectArray;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusFactoryCreator;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.SerializersUtil;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderOverrideBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterOverrideBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.multipart.MultipartForm;

public class JaxrsClientReactiveProcessor {

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

    @BuildStep
    void addFeature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(JAXRS_CLIENT_REACTIVE));
    }

    @BuildStep
    void registerClientResponseBuilder(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        serviceProviders.produce(new ServiceProviderBuildItem(ResponseBuilderFactory.class.getName(),
                ClientResponseBuilderFactory.class.getName()));

        serviceProviders.produce(new ServiceProviderBuildItem(ClientBuilder.class.getName(),
                ClientBuilderImpl.class.getName()));

    }

    @BuildStep
    void initializeRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(AsyncInvokerImpl.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupClientProxies(JaxrsClientReactiveRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            List<MessageBodyReaderOverrideBuildItem> messageBodyReaderOverrideBuildItems,
            List<MessageBodyWriterOverrideBuildItem> messageBodyWriterOverrideBuildItems,
            List<JaxrsClientReactiveEnricherBuildItem> enricherBuildItems,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            Capabilities capabilities, Optional<MetricsCapabilityBuildItem> metricsCapability,
            ResteasyReactiveConfig config,
            RecorderContext recorderContext,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer,
            List<RestClientDefaultProducesBuildItem> defaultConsumes,
            List<RestClientDefaultConsumesBuildItem> defaultProduces,
            List<RestClientDisableSmartDefaultProduces> disableSmartDefaultProduces) {
        String defaultConsumesType = defaultMediaType(defaultConsumes, MediaType.APPLICATION_OCTET_STREAM);
        String defaultProducesType = defaultMediaType(defaultProduces, MediaType.TEXT_PLAIN);

        Serialisers serialisers = recorder.createSerializers();

        SerializersUtil.setupSerializers(recorder, reflectiveClassBuildItemBuildProducer, messageBodyReaderBuildItems,
                messageBodyWriterBuildItems, messageBodyReaderOverrideBuildItems, messageBodyWriterOverrideBuildItems,
                beanContainerBuildItem, applicationResultBuildItem, serialisers,
                RuntimeType.CLIENT);

        if (!resourceScanningResultBuildItem.isPresent()
                || resourceScanningResultBuildItem.get().getResult().getClientInterfaces().isEmpty()) {
            recorder.setupClientProxies(new HashMap<>(), Collections.emptyMap());
            return;
        }
        ResourceScanningResult result = resourceScanningResultBuildItem.get().getResult();

        AdditionalReaders additionalReaders = new AdditionalReaders();
        AdditionalWriters additionalWriters = new AdditionalWriters();

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        ClientEndpointIndexer clientEndpointIndexer = new ClientEndpointIndexer.Builder()
                .setIndex(index)
                .setExistingConverters(new HashMap<>())
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
                .build();

        boolean observabilityIntegrationNeeded = (capabilities.isPresent(Capability.OPENTELEMETRY_TRACER) ||
                (metricsCapability.isPresent()
                        && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)));

        Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations = new HashMap<>();
        Map<String, String> failures = new HashMap<>();
        for (Map.Entry<DotName, String> i : result.getClientInterfaces().entrySet()) {
            ClassInfo clazz = index.getClassByName(i.getKey());
            //these interfaces can also be clients
            //so we generate client proxies for them
            MaybeRestClientInterface maybeClientProxy = clientEndpointIndexer.createClientProxy(clazz,
                    i.getValue());
            if (maybeClientProxy.exists()) {
                RestClientInterface clientProxy = maybeClientProxy.getRestClientInterface();
                try {
                    RuntimeValue<Function<WebTarget, ?>> proxyProvider = generateClientInvoker(recorderContext, clientProxy,
                            enricherBuildItems, generatedClassBuildItemBuildProducer, clazz, index, defaultConsumesType,
                            result.getHttpAnnotationToMethod(), observabilityIntegrationNeeded);
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
            Class<?> readerClass = additionalReader.getHandlerClass();
            ResourceReader reader = new ResourceReader();
            reader.setBuiltin(true);
            reader.setFactory(recorder.factory(readerClass.getName(), beanContainerBuildItem.getValue()));
            reader.setMediaTypeStrings(Collections.singletonList(additionalReader.getMediaType()));
            recorder.registerReader(serialisers, additionalReader.getEntityClass().getName(), reader);
            reflectiveClassBuildItemBuildProducer
                    .produce(new ReflectiveClassBuildItem(true, false, false, readerClass.getName()));
        }

        for (AdditionalReaderWriter.Entry entry : additionalWriters.get()) {
            Class<?> writerClass = entry.getHandlerClass();
            ResourceWriter writer = new ResourceWriter();
            writer.setBuiltin(true);
            writer.setFactory(recorder.factory(writerClass.getName(), beanContainerBuildItem.getValue()));
            writer.setMediaTypeStrings(Collections.singletonList(entry.getMediaType()));
            recorder.registerWriter(serialisers, entry.getEntityClass().getName(), writer);
            reflectiveClassBuildItemBuildProducer
                    .produce(new ReflectiveClassBuildItem(true, false, false, writerClass.getName()));
        }

    }

    private org.jboss.resteasy.reactive.common.ResteasyReactiveConfig createRestReactiveConfig(ResteasyReactiveConfig config) {
        Config mpConfig = ConfigProvider.getConfig();

        return new org.jboss.resteasy.reactive.common.ResteasyReactiveConfig(
                getEffectivePropertyValue("input-buffer-size", config.inputBufferSize.asLongValue(), Long.class, mpConfig),
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
    private RuntimeValue<Function<WebTarget, ?>> generateClientInvoker(RecorderContext recorderContext,
            RestClientInterface restClientInterface, List<JaxrsClientReactiveEnricherBuildItem> enrichers,
            BuildProducer<GeneratedClassBuildItem> generatedClasses, ClassInfo interfaceClass,
            IndexView index, String defaultMediaType, Map<DotName, String> httpAnnotationToMethod,
            boolean observabilityIntegrationNeeded) {

        String name = restClientInterface.getClassName() + "$$QuarkusRestClientInterface";
        MethodDescriptor ctorDesc = MethodDescriptor.ofConstructor(name, WebTarget.class.getName());
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClasses, true),
                name, null, Object.class.getName(),
                Closeable.class.getName(), restClientInterface.getClassName())) {

            //
            // initialize basic WebTarget in constructor
            //

            MethodCreator constructor = c.getMethodCreator(ctorDesc);
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), constructor.getThis());

            MethodCreator clinit = c.getMethodCreator(MethodDescriptor.ofMethod(name, "<clinit>", void.class));
            clinit.setModifiers(Opcodes.ACC_STATIC);

            AssignableResultHandle baseTarget = constructor.createVariable(WebTarget.class);
            constructor.assign(baseTarget,
                    constructor.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                            constructor.getMethodParam(0), constructor.load(restClientInterface.getPath())));

            for (JaxrsClientReactiveEnricherBuildItem enricher : enrichers) {
                enricher.getEnricher().forClass(constructor, baseTarget, interfaceClass, index);
            }

            //
            // go through all the methods of the jaxrs interface. Create specific WebTargets (in the constructor) and methods
            //
            int methodIndex = 0;
            List<FieldDescriptor> webTargets = new ArrayList<>();
            for (ResourceMethod method : restClientInterface.getMethods()) {
                methodIndex++;

                // finding corresponding jandex method, used by enricher (MicroProfile enricher stores it in a field
                // to later fill in context with corresponding java.lang.reflect.Method
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
                    Type returnType = jandexMethod.returnType();
                    if (returnType.kind() != Type.Kind.CLASS) {
                        // sort of sub-resource method that returns a thing that isn't a class
                        throw new IllegalArgumentException("Sub resource type is not a class: " + returnType.name().toString());
                    }
                    ClassInfo subResourceInterface = index.getClassByName(returnType.name());
                    if (!Modifier.isInterface(subResourceInterface.flags())) {
                        throw new IllegalArgumentException(
                                "Sub resource type is not an interface: " + returnType.name().toString());
                    }
                    // generate implementation for a method from the jaxrs interface:
                    MethodCreator methodCreator = c.getMethodCreator(method.getName(), method.getSimpleReturnType(),
                            javaMethodParameters);

                    String subName = subResourceInterface.name().toString() + HashUtil.sha1(name) + methodIndex;
                    try (ClassCreator sub = new ClassCreator(
                            new GeneratedClassGizmoAdaptor(generatedClasses, true),
                            subName, null, Object.class.getName(), subResourceInterface.name().toString())) {

                        ResultHandle subInstance = methodCreator.newInstance(MethodDescriptor.ofConstructor(subName));

                        MethodCreator subConstructor = null;
                        MethodCreator subClinit = null;
                        if (!enrichers.isEmpty()) {
                            subConstructor = sub.getMethodCreator(MethodDescriptor.ofConstructor(subName));
                            subConstructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class),
                                    subConstructor.getThis());
                            subClinit = sub.getMethodCreator(MethodDescriptor.ofMethod(subName, "<clinit>", void.class));
                            subClinit.setModifiers(Opcodes.ACC_STATIC);
                        }

                        Map<Integer, FieldDescriptor> paramFields = new HashMap<>();
                        for (int i = 0; i < method.getParameters().length; i++) {
                            FieldDescriptor paramField = sub.getFieldCreator("param" + i, method.getParameters()[i].type)
                                    .setModifiers(Modifier.PUBLIC)
                                    .getFieldDescriptor();
                            methodCreator.writeInstanceField(paramField, subInstance, methodCreator.getMethodParam(i));
                            paramFields.put(i, paramField);
                        }

                        ResultHandle multipartForm = null;

                        int subMethodIndex = 0;
                        for (ResourceMethod subMethod : method.getSubResourceMethods()) {
                            if (subMethod.getHttpMethod() == null) {
                                continue;
                            }
                            MethodInfo jandexSubMethod = getJavaMethod(subResourceInterface, subMethod,
                                    subMethod.getParameters(), index)
                                            .orElseThrow(() -> new RuntimeException(
                                                    "Failed to find matching java method for " + method + " on "
                                                            + interfaceClass
                                                            + ". It may have unresolved parameter types (generics)"));
                            subMethodIndex++;
                            // WebTarget field in the root stub implementation (not to recreate it on each call):

                            // initializing the web target in the root stub constructor:
                            FieldDescriptor webTargetForSubMethod = FieldDescriptor.of(name,
                                    "target" + methodIndex + "_" + subMethodIndex,
                                    WebTarget.class);
                            c.getFieldCreator(webTargetForSubMethod).setModifiers(Modifier.FINAL);
                            webTargets.add(webTargetForSubMethod);

                            AssignableResultHandle constructorTarget = createWebTargetForMethod(constructor, baseTarget,
                                    method);
                            if (subMethod.getPath() != null) {
                                appendPath(constructor, subMethod.getPath(), constructorTarget);
                            }

                            constructor.writeInstanceField(webTargetForSubMethod, constructor.getThis(), constructorTarget);

                            // set the sub stub target field value to the target created above:
                            FieldDescriptor subWebTarget = sub.getFieldCreator("target" + subMethodIndex, WebTarget.class)
                                    .setModifiers(Modifier.PUBLIC)
                                    .getFieldDescriptor();
                            methodCreator.writeInstanceField(subWebTarget, subInstance,
                                    methodCreator.readInstanceField(webTargetForSubMethod, methodCreator.getThis()));

                            MethodCreator subMethodCreator = sub.getMethodCreator(subMethod.getName(),
                                    jandexSubMethod.returnType().name().toString(),
                                    parametersAsStringArray(jandexSubMethod));

                            AssignableResultHandle methodTarget = subMethodCreator.createVariable(WebTarget.class);
                            subMethodCreator.assign(methodTarget,
                                    subMethodCreator.readInstanceField(subWebTarget, subMethodCreator.getThis()));

                            ResultHandle bodyParameterValue = null;
                            AssignableResultHandle formParams = null;
                            Map<MethodDescriptor, ResultHandle> invocationBuilderEnrichers = new HashMap<>();

                            // first go through parameters of the root stub method, we have them copied to fields in the sub stub
                            for (int paramIdx = 0; paramIdx < method.getParameters().length; ++paramIdx) {
                                MethodParameter param = method.getParameters()[paramIdx];
                                ResultHandle paramValue = subMethodCreator.readInstanceField(paramFields.get(paramIdx),
                                        subMethodCreator.getThis());
                                if (param.parameterType == ParameterType.QUERY) {
                                    //TODO: converters

                                    // query params have to be set on a method-level web target (they vary between invocations)
                                    subMethodCreator.assign(methodTarget,
                                            addQueryParam(subMethodCreator, methodTarget, param.name,
                                                    paramValue, jandexMethod.parameters().get(paramIdx), index));
                                } else if (param.parameterType == ParameterType.BEAN) {
                                    // bean params require both, web-target and Invocation.Builder, modifications
                                    // The web target changes have to be done on the method level.
                                    // Invocation.Builder changes are offloaded to a separate method
                                    // so that we can generate bytecode for both, web target and invocation builder modifications
                                    // at once
                                    ClientBeanParamInfo beanParam = (ClientBeanParamInfo) param;
                                    MethodDescriptor handleBeanParamDescriptor = MethodDescriptor.ofMethod(subName,
                                            subMethod.getName() + "$$" + methodIndex + "$$handleBeanParam$$" + paramIdx,
                                            Invocation.Builder.class,
                                            Invocation.Builder.class, param.type);
                                    MethodCreator handleBeanParamMethod = sub.getMethodCreator(handleBeanParamDescriptor);

                                    AssignableResultHandle invocationBuilderRef = handleBeanParamMethod
                                            .createVariable(Invocation.Builder.class);
                                    handleBeanParamMethod.assign(invocationBuilderRef, handleBeanParamMethod.getMethodParam(0));
                                    addBeanParamData(subMethodCreator, handleBeanParamMethod,
                                            invocationBuilderRef, beanParam.getItems(),
                                            paramValue, methodTarget, index);

                                    handleBeanParamMethod.returnValue(invocationBuilderRef);
                                    invocationBuilderEnrichers.put(handleBeanParamDescriptor, paramValue);
                                } else if (param.parameterType == ParameterType.PATH) {
                                    // methodTarget = methodTarget.resolveTemplate(paramname, paramvalue);
                                    subMethodCreator.assign(methodTarget,
                                            subMethodCreator.invokeInterfaceMethod(WEB_TARGET_RESOLVE_TEMPLATE_METHOD,
                                                    methodTarget,
                                                    subMethodCreator.load(param.name), paramValue));
                                } else if (param.parameterType == ParameterType.BODY) {
                                    // just store the index of parameter used to create the body, we'll use it later
                                    bodyParameterValue = paramValue;
                                } else if (param.parameterType == ParameterType.HEADER) {
                                    // headers are added at the invocation builder level
                                    MethodDescriptor handleHeaderDescriptor = MethodDescriptor.ofMethod(subName,
                                            subMethod.getName() + "$$" + subMethodIndex + "$$handleHeader$$" + paramIdx,
                                            Invocation.Builder.class,
                                            Invocation.Builder.class, param.type);
                                    MethodCreator handleHeaderMethod = sub.getMethodCreator(handleHeaderDescriptor);

                                    AssignableResultHandle invocationBuilderRef = handleHeaderMethod
                                            .createVariable(Invocation.Builder.class);
                                    handleHeaderMethod.assign(invocationBuilderRef, handleHeaderMethod.getMethodParam(0));
                                    addHeaderParam(handleHeaderMethod, invocationBuilderRef, param.name,
                                            handleHeaderMethod.getMethodParam(1));
                                    handleHeaderMethod.returnValue(invocationBuilderRef);
                                    invocationBuilderEnrichers.put(handleHeaderDescriptor, paramValue);
                                } else if (param.parameterType == ParameterType.FORM) {
                                    formParams = createIfAbsent(subMethodCreator, formParams);
                                    subMethodCreator.invokeInterfaceMethod(MULTIVALUED_MAP_ADD, formParams,
                                            subMethodCreator.load(param.name), paramValue);
                                } else if (param.parameterType == ParameterType.MULTI_PART_FORM) {
                                    if (multipartForm != null) {
                                        throw new IllegalArgumentException("MultipartForm data set twice for method "
                                                + jandexSubMethod.declaringClass().name() + "#" + jandexSubMethod.name());
                                    }
                                    multipartForm = createMultipartForm(subMethodCreator, paramValue,
                                            jandexMethod.parameters().get(paramIdx).asClassType(), index);
                                }
                            }
                            // handle sub-method parameters:
                            for (int paramIdx = 0; paramIdx < subMethod.getParameters().length; ++paramIdx) {
                                MethodParameter param = subMethod.getParameters()[paramIdx];
                                if (param.parameterType == ParameterType.QUERY) {
                                    //TODO: converters

                                    // query params have to be set on a method-level web target (they vary between invocations)
                                    subMethodCreator.assign(methodTarget,
                                            addQueryParam(subMethodCreator, methodTarget, param.name,
                                                    subMethodCreator.getMethodParam(paramIdx),
                                                    jandexSubMethod.parameters().get(paramIdx), index));
                                } else if (param.parameterType == ParameterType.BEAN) {
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
                                    MethodCreator handleBeanParamMethod = c.getMethodCreator(handleBeanParamDescriptor);

                                    AssignableResultHandle invocationBuilderRef = handleBeanParamMethod
                                            .createVariable(Invocation.Builder.class);
                                    handleBeanParamMethod.assign(invocationBuilderRef, handleBeanParamMethod.getMethodParam(0));
                                    addBeanParamData(subMethodCreator, handleBeanParamMethod,
                                            invocationBuilderRef, beanParam.getItems(),
                                            subMethodCreator.getMethodParam(paramIdx), methodTarget, index);

                                    handleBeanParamMethod.returnValue(invocationBuilderRef);
                                    invocationBuilderEnrichers.put(handleBeanParamDescriptor,
                                            subMethodCreator.getMethodParam(paramIdx));
                                } else if (param.parameterType == ParameterType.PATH) {
                                    // methodTarget = methodTarget.resolveTemplate(paramname, paramvalue);
                                    subMethodCreator.assign(methodTarget,
                                            subMethodCreator.invokeInterfaceMethod(WEB_TARGET_RESOLVE_TEMPLATE_METHOD,
                                                    methodTarget,
                                                    subMethodCreator.load(param.name),
                                                    subMethodCreator.getMethodParam(paramIdx)));
                                } else if (param.parameterType == ParameterType.BODY) {
                                    // just store the index of parameter used to create the body, we'll use it later
                                    bodyParameterValue = subMethodCreator.getMethodParam(paramIdx);
                                } else if (param.parameterType == ParameterType.HEADER) {
                                    // headers are added at the invocation builder level
                                    MethodDescriptor handleHeaderDescriptor = MethodDescriptor.ofMethod(subName,
                                            subMethod.getName() + "$$" + subMethodIndex + "$$handleHeader$$" + paramIdx,
                                            Invocation.Builder.class,
                                            Invocation.Builder.class, param.type);
                                    MethodCreator handleHeaderMethod = c.getMethodCreator(handleHeaderDescriptor);

                                    AssignableResultHandle invocationBuilderRef = handleHeaderMethod
                                            .createVariable(Invocation.Builder.class);
                                    handleHeaderMethod.assign(invocationBuilderRef, handleHeaderMethod.getMethodParam(0));
                                    addHeaderParam(handleHeaderMethod, invocationBuilderRef, param.name,
                                            handleHeaderMethod.getMethodParam(1));
                                    handleHeaderMethod.returnValue(invocationBuilderRef);
                                    invocationBuilderEnrichers.put(handleHeaderDescriptor,
                                            subMethodCreator.getMethodParam(paramIdx));
                                } else if (param.parameterType == ParameterType.FORM) {
                                    formParams = createIfAbsent(subMethodCreator, formParams);
                                    subMethodCreator.invokeInterfaceMethod(MULTIVALUED_MAP_ADD, formParams,
                                            subMethodCreator.load(param.name),
                                            subMethodCreator.getMethodParam(paramIdx));
                                } else if (param.parameterType == ParameterType.MULTI_PART_FORM) {
                                    if (multipartForm != null) {
                                        throw new IllegalArgumentException("MultipartForm data set twice for method "
                                                + jandexSubMethod.declaringClass().name() + "#" + jandexSubMethod.name());
                                    }
                                    multipartForm = createMultipartForm(subMethodCreator,
                                            subMethodCreator.getMethodParam(paramIdx),
                                            jandexSubMethod.parameters().get(paramIdx), index);
                                }

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
                                        .forSubResourceMethod(sub, subConstructor, subClinit, subMethodCreator, interfaceClass,
                                                subResourceInterface, jandexSubMethod, jandexMethod, builder, index,
                                                generatedClasses, methodIndex, subMethodIndex);
                            }

                            String[] consumes = extractProducesConsumesValues(
                                    jandexSubMethod.declaringClass().classAnnotation(CONSUMES), method.getConsumes());
                            consumes = extractProducesConsumesValues(jandexSubMethod.annotation(CONSUMES), consumes);
                            handleReturn(subResourceInterface, defaultMediaType,
                                    getHttpMethod(jandexSubMethod, subMethod.getHttpMethod(), httpAnnotationToMethod),
                                    consumes, jandexSubMethod, subMethodCreator, formParams, multipartForm, bodyParameterValue,
                                    builder);
                        }

                        if (subConstructor != null) {
                            subConstructor.returnValue(null);
                            subClinit.returnValue(null);
                        }

                        methodCreator.returnValue(subInstance);
                    }
                } else {

                    // constructor: initializing the immutable part of the method-specific web target
                    FieldDescriptor webTargetForMethod = FieldDescriptor.of(name, "target" + methodIndex, WebTargetImpl.class);
                    c.getFieldCreator(webTargetForMethod).setModifiers(Modifier.FINAL);
                    webTargets.add(webTargetForMethod);

                    AssignableResultHandle constructorTarget = createWebTargetForMethod(constructor, baseTarget, method);
                    constructor.writeInstanceField(webTargetForMethod, constructor.getThis(), constructorTarget);
                    if (observabilityIntegrationNeeded) {
                        String templatePath = MULTIPLE_SLASH_PATTERN.matcher(restClientInterface.getPath() + method.getPath())
                                .replaceAll("/");
                        constructor.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(WebTargetImpl.class, "setPreClientSendHandler", void.class,
                                        ClientRestHandler.class),
                                constructor.readInstanceField(webTargetForMethod, constructor.getThis()),
                                constructor.newInstance(
                                        MethodDescriptor.ofConstructor(ClientObservabilityHandler.class, String.class),
                                        constructor.load(templatePath)));
                    }

                    // generate implementation for a method from jaxrs interface:
                    MethodCreator methodCreator = c.getMethodCreator(method.getName(), method.getSimpleReturnType(),
                            javaMethodParameters);

                    AssignableResultHandle methodTarget = methodCreator.createVariable(WebTarget.class);
                    methodCreator.assign(methodTarget,
                            methodCreator.readInstanceField(webTargetForMethod, methodCreator.getThis()));

                    Integer bodyParameterIdx = null;
                    Map<MethodDescriptor, ResultHandle> invocationBuilderEnrichers = new HashMap<>();

                    ResultHandle multipartForm = null;

                    AssignableResultHandle formParams = null;

                    for (int paramIdx = 0; paramIdx < method.getParameters().length; ++paramIdx) {
                        MethodParameter param = method.getParameters()[paramIdx];
                        if (param.parameterType == ParameterType.QUERY) {
                            //TODO: converters

                            // query params have to be set on a method-level web target (they vary between invocations)
                            methodCreator.assign(methodTarget, addQueryParam(methodCreator, methodTarget, param.name,
                                    methodCreator.getMethodParam(paramIdx),
                                    jandexMethod.parameters().get(paramIdx), index));
                        } else if (param.parameterType == ParameterType.BEAN) {
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
                            MethodCreator handleBeanParamMethod = c.getMethodCreator(handleBeanParamDescriptor);

                            AssignableResultHandle invocationBuilderRef = handleBeanParamMethod
                                    .createVariable(Invocation.Builder.class);
                            handleBeanParamMethod.assign(invocationBuilderRef, handleBeanParamMethod.getMethodParam(0));
                            addBeanParamData(methodCreator, handleBeanParamMethod,
                                    invocationBuilderRef, beanParam.getItems(),
                                    methodCreator.getMethodParam(paramIdx), methodTarget, index);

                            handleBeanParamMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleBeanParamDescriptor, methodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.PATH) {
                            // methodTarget = methodTarget.resolveTemplate(paramname, paramvalue);
                            methodCreator.assign(methodTarget,
                                    methodCreator.invokeInterfaceMethod(WEB_TARGET_RESOLVE_TEMPLATE_METHOD, methodTarget,
                                            methodCreator.load(param.name), methodCreator.getMethodParam(paramIdx)));
                        } else if (param.parameterType == ParameterType.BODY) {
                            // just store the index of parameter used to create the body, we'll use it later
                            bodyParameterIdx = paramIdx;
                        } else if (param.parameterType == ParameterType.HEADER) {
                            // headers are added at the invocation builder level
                            MethodDescriptor handleHeaderDescriptor = MethodDescriptor.ofMethod(name,
                                    method.getName() + "$$" + methodIndex + "$$handleHeader$$" + paramIdx,
                                    Invocation.Builder.class,
                                    Invocation.Builder.class, param.type);
                            MethodCreator handleHeaderMethod = c.getMethodCreator(handleHeaderDescriptor);

                            AssignableResultHandle invocationBuilderRef = handleHeaderMethod
                                    .createVariable(Invocation.Builder.class);
                            handleHeaderMethod.assign(invocationBuilderRef, handleHeaderMethod.getMethodParam(0));
                            addHeaderParam(handleHeaderMethod, invocationBuilderRef, param.name,
                                    handleHeaderMethod.getMethodParam(1));
                            handleHeaderMethod.returnValue(invocationBuilderRef);
                            invocationBuilderEnrichers.put(handleHeaderDescriptor, methodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.FORM) {
                            formParams = createIfAbsent(methodCreator, formParams);
                            methodCreator.invokeInterfaceMethod(MULTIVALUED_MAP_ADD, formParams,
                                    methodCreator.load(param.name), methodCreator.getMethodParam(paramIdx));
                        } else if (param.parameterType == ParameterType.MULTI_PART_FORM) {
                            if (multipartForm != null) {
                                throw new IllegalArgumentException("MultipartForm data set twice for method "
                                        + jandexMethod.declaringClass().name() + "#" + jandexMethod.name());
                            }
                            multipartForm = createMultipartForm(methodCreator, methodCreator.getMethodParam(paramIdx),
                                    jandexMethod.parameters().get(paramIdx), index);
                        }
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
                                .forMethod(c, constructor, clinit, methodCreator, interfaceClass, jandexMethod, builder,
                                        index, generatedClasses, methodIndex);
                    }

                    handleReturn(interfaceClass, defaultMediaType, method.getHttpMethod(),
                            method.getConsumes(), jandexMethod, methodCreator, formParams, multipartForm,
                            bodyParameterIdx == null ? null : methodCreator.getMethodParam(bodyParameterIdx), builder);
                }
            }
            constructor.returnValue(null);
            clinit.returnValue(null);

            // create `void close()` method:
            MethodCreator closeCreator = c.getMethodCreator(MethodDescriptor.ofMethod(Closeable.class, "close", void.class));
            for (FieldDescriptor target : webTargets) {
                ResultHandle webTarget = closeCreator.readInstanceField(target, closeCreator.getThis());
                ResultHandle webTargetImpl = closeCreator.checkCast(webTarget, WebTargetImpl.class);
                ResultHandle restClient = closeCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(WebTargetImpl.class, "getRestClient", ClientImpl.class), webTargetImpl);
                closeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ClientImpl.class, "close", void.class), restClient);
            }
            closeCreator.returnValue(null);
        }
        String creatorName = restClientInterface.getClassName() + "$$QuarkusRestClientInterfaceCreator";
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClasses, true),
                creatorName, null, Object.class.getName(), Function.class.getName())) {

            MethodCreator apply = c
                    .getMethodCreator(MethodDescriptor.ofMethod(creatorName, "apply", Object.class, Object.class));
            apply.returnValue(apply.newInstance(ctorDesc, apply.getMethodParam(0)));
        }
        return recorderContext.newInstance(creatorName);

    }

    /*
     * Translate the class to be sent as multipart to Vertx Web MultipartForm.
     */
    private ResultHandle createMultipartForm(MethodCreator methodCreator, ResultHandle methodParam, Type formClassType,
            IndexView index) {
        AssignableResultHandle multipartForm = methodCreator.createVariable(MultipartForm.class);
        methodCreator.assign(multipartForm,
                methodCreator
                        .invokeStaticMethod(
                                MethodDescriptor.ofMethod(MultipartFormUtils.class, "create", MultipartForm.class)));

        ClassInfo formClass = index.getClassByName(formClassType.name());

        for (FieldInfo field : formClass.fields()) {
            // go field by field, ignore static fields and fail on non-public fields, only public fields are supported ATM
            if (Modifier.isStatic(field.flags())) {
                continue;
            }
            if (!Modifier.isPublic(field.flags())) {
                throw new IllegalArgumentException("Non-public field found in a multipart form data class "
                        + formClassType.name()
                        + ". Rest Client Reactive only supports multipart form classes with a list of public fields");
            }

            String formParamName = formParamName(field);
            String partType = formPartType(field);

            Type fieldType = field.type();

            ResultHandle fieldValue = methodCreator.readInstanceField(field, methodParam);

            switch (fieldType.kind()) {
                case CLASS:
                    // we support string, and send it as an attribute
                    ClassInfo fieldClass = index.getClassByName(fieldType.name());
                    if (DotNames.STRING.equals(fieldClass.name())) {
                        addString(methodCreator, multipartForm, formParamName, fieldValue);
                    } else if (is(FILE, fieldClass, index)) {
                        // file is sent as file :)
                        if (partType == null) {
                            throw new IllegalArgumentException(
                                    "No @PartType annotation found on multipart form field of type File: " +
                                            formClass.name() + "." + field.name());
                        }
                        ResultHandle filePath = methodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(File.class, "toPath", Path.class), fieldValue);
                        addFile(methodCreator, multipartForm, formParamName, partType, filePath);
                    } else if (is(PATH, fieldClass, index)) {
                        // and so is path
                        if (partType == null) {
                            throw new IllegalArgumentException(
                                    "No @PartType annotation found on multipart form field of type Path: " +
                                            formClass.name() + "." + field.name());
                        }
                        addFile(methodCreator, multipartForm, formParamName, partType, fieldValue);
                    } else if (is(BUFFER, fieldClass, index)) {
                        // and buffer
                        addBuffer(methodCreator, multipartForm, formParamName, partType, fieldValue, field);
                    } else {
                        throw new IllegalArgumentException("Unsupported multipart form field on: " + formClassType.name()
                                + "." + fieldType.name() +
                                ". Supported types are: java.lang.String, java.io.File, java.nio.Path and io.vertx.core.Buffer");
                    }
                    break;
                case ARRAY:
                    // byte[] can be sent as file too
                    Type componentType = fieldType.asArrayType().component();
                    if (componentType.kind() != Type.Kind.PRIMITIVE
                            || !byte.class.getName().equals(componentType.name().toString())) {
                        throw new IllegalArgumentException("Array of unsupported type: " + componentType.name()
                                + " on " + formClassType.name() + "." + field.name());
                    }
                    ResultHandle buffer = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(MultipartFormUtils.class, "buffer", Buffer.class, byte[].class),
                            fieldValue);
                    addBuffer(methodCreator, multipartForm, formParamName, partType, buffer, field);
                    break;
                case PRIMITIVE:
                    // primitives are converted to text and sent as attribute
                    ResultHandle string = primitiveToString(methodCreator, fieldValue, field);
                    addString(methodCreator, multipartForm, formParamName, string);
                    break;
                case VOID:
                case TYPE_VARIABLE:
                case UNRESOLVED_TYPE_VARIABLE:
                case WILDCARD_TYPE:
                case PARAMETERIZED_TYPE:
                    throw new IllegalArgumentException("Unsupported multipart form field type: " + fieldType + " in " +
                            "field class " + formClassType.name());
            }
        }

        return multipartForm;
    }

    /**
     * add file upload, see {@link MultipartForm#binaryFileUpload(String, String, String, String)} and
     * {@link MultipartForm#textFileUpload(String, String, String, String)}
     */
    private void addFile(MethodCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            String partType, ResultHandle filePath) {
        ResultHandle fileNamePath = methodCreator.invokeInterfaceMethod(PATH_GET_FILENAME, filePath);
        ResultHandle fileName = methodCreator.invokeVirtualMethod(OBJECT_TO_STRING, fileNamePath);
        ResultHandle pathString = methodCreator.invokeVirtualMethod(OBJECT_TO_STRING, filePath);
        if (partType.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM)) {
            methodCreator.assign(multipartForm,
                    // MultipartForm#binaryFileUpload(String name, String filename, String pathname, String mediaType);
                    // filename = name
                    methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(MultipartForm.class, "binaryFileUpload",
                                    MultipartForm.class, String.class, String.class, String.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), fileName,
                            pathString, methodCreator.load(partType)));
        } else {
            methodCreator.assign(multipartForm,
                    // MultipartForm#textFileUpload(String name, String filename, String pathname, String mediaType);;
                    // filename = name
                    methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(MultipartForm.class, "textFileUpload",
                                    MultipartForm.class, String.class, String.class, String.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), fileName,
                            pathString, methodCreator.load(partType)));
        }
    }

    private ResultHandle primitiveToString(MethodCreator methodCreator, ResultHandle fieldValue, FieldInfo field) {
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
                throw new IllegalArgumentException("Unsupported primitive type in mulitpart form field: "
                        + field.declaringClass().name() + "." + field.name());
        }
    }

    private void addString(MethodCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            ResultHandle fieldValue) {
        methodCreator.assign(multipartForm,
                methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(MultipartForm.class, "attribute", MultipartForm.class,
                                String.class, String.class),
                        multipartForm, methodCreator.load(formParamName), fieldValue));
    }

    private void addBuffer(MethodCreator methodCreator, AssignableResultHandle multipartForm, String formParamName,
            String partType, ResultHandle buffer, FieldInfo field) {
        if (partType == null) {
            throw new IllegalArgumentException(
                    "No @PartType annotation found on multipart form field " +
                            field.declaringClass().name() + "." + field.name());
        }
        if (partType.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM)) {
            methodCreator.assign(multipartForm,
                    // MultipartForm#binaryFileUpload(String name, String filename, String pathname, String mediaType);
                    // filename = name
                    methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(MultipartForm.class, "binaryFileUpload",
                                    MultipartForm.class, String.class, String.class, Buffer.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), methodCreator.load(formParamName),
                            buffer, methodCreator.load(partType)));
        } else {
            methodCreator.assign(multipartForm,
                    // MultipartForm#textFileUpload(String name, String filename, io.vertx.mutiny.core.buffer.Buffer content, String mediaType)
                    // filename = name
                    methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(MultipartForm.class, "textFileUpload",
                                    MultipartForm.class, String.class, String.class, Buffer.class,
                                    String.class),
                            multipartForm, methodCreator.load(formParamName), methodCreator.load(formParamName),
                            buffer, methodCreator.load(partType)));
        }
    }

    private String formPartType(FieldInfo field) {
        AnnotationInstance partType = field.annotation(ResteasyReactiveDotNames.PART_TYPE_NAME);
        if (partType != null) {
            return partType.value().asString();
        }
        return null;
    }

    private String formParamName(FieldInfo field) {
        AnnotationInstance restFormParam = field.annotation(ResteasyReactiveDotNames.REST_FORM_PARAM);
        AnnotationInstance formParam = field.annotation(ResteasyReactiveDotNames.FORM_PARAM);
        if (restFormParam != null && formParam != null) {
            throw new IllegalArgumentException("Only one of @RestFormParam, @FormParam annotations expected on a field. " +
                    "Found both on " + field.declaringClass() + "." + field.name());
        }
        if (restFormParam != null) {
            AnnotationValue value = restFormParam.value();
            if (value == null || "".equals(value.asString())) {
                return field.name();
            } else {
                return value.asString();
            }
        } else if (formParam != null) {
            return formParam.value().asString();
        } else {
            throw new IllegalArgumentException("One of @RestFormParam, @FormParam annotations expected on a field. " +
                    "No annotation found on " + field.declaringClass() + "." + field.name());
        }
    }

    private boolean is(DotName desiredClass, ClassInfo fieldClass, IndexView index) {
        if (fieldClass.name().equals(desiredClass)) {
            return true;
        }
        ClassInfo superClass;
        if (fieldClass.name().toString().equals(Object.class.getName()) ||
                (superClass = index.getClassByName(fieldClass.superName())) == null) {
            return false;
        }
        return is(desiredClass, superClass, index);
    }

    private AssignableResultHandle createIfAbsent(MethodCreator methodCreator, AssignableResultHandle formValues) {
        if (formValues == null) {
            formValues = methodCreator.createVariable(MultivaluedMap.class);
            methodCreator.assign(formValues,
                    methodCreator.newInstance(MethodDescriptor.ofConstructor(MultivaluedHashMap.class)));
        }
        return formValues;
    }

    private String[] parametersAsStringArray(MethodInfo subMethod) {
        List<Type> params = subMethod.parameters();
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
            if (subMethod.declaringClass().classAnnotation(annoToMethod.getKey()) != null) {
                return annoToMethod.getValue();
            }
        }
        return defaultMethod;
    }

    private void handleReturn(ClassInfo restClientInterface, String defaultMediaType, String httpMethod, String[] consumes,
            MethodInfo jandexMethod, MethodCreator methodCreator, ResultHandle formParams, ResultHandle multipartForm,
            ResultHandle bodyValue, AssignableResultHandle builder) {
        Type returnType = jandexMethod.returnType();
        ReturnCategory returnCategory = ReturnCategory.BLOCKING;

        String simpleReturnType = returnType.name().toString();
        ResultHandle genericReturnType = null;

        if (returnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType paramType = returnType.asParameterizedType();
            if (paramType.name().equals(COMPLETION_STAGE) || paramType.name().equals(UNI)) {
                returnCategory = paramType.name().equals(COMPLETION_STAGE) ? ReturnCategory.COMPLETION_STAGE
                        : ReturnCategory.UNI;

                // CompletionStage has one type argument:
                if (paramType.arguments().isEmpty()) {
                    simpleReturnType = Object.class.getName();
                } else {
                    Type type = paramType.arguments().get(0);
                    if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        genericReturnType = createGenericTypeFromParameterizedType(methodCreator,
                                type.asParameterizedType());
                    } else {
                        simpleReturnType = type.toString();
                    }
                }
            } else {
                genericReturnType = createGenericTypeFromParameterizedType(methodCreator, paramType);
            }
        }
        Integer continuationIndex = null;
        //TODO: there should be an SPI for this
        if (returnCategory == ReturnCategory.BLOCKING) {
            List<Type> parameters = jandexMethod.parameters();
            if (!parameters.isEmpty()) {
                Type lastParamType = parameters.get(parameters.size() - 1);
                if (lastParamType.name().equals(CONTINUATION)) {
                    continuationIndex = parameters.size() - 1;
                    returnCategory = ReturnCategory.COROUTINE;

                    try {
                        Thread.currentThread().getContextClassLoader().loadClass(UNI_KT.toString());
                    } catch (ClassNotFoundException e) {
                        //TODO: make this automatic somehow
                        throw new RuntimeException("Suspendable rest client method" + jandexMethod + " is present on class "
                                + jandexMethod.declaringClass()
                                + " however io.smallrye.reactive:mutiny-kotlin is not detected. Please add a dependency on this artifact.");
                    }

                    //we infer the return type from the param type of the continuation
                    Type type = lastParamType.asParameterizedType().arguments().get(0);
                    for (;;) {
                        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
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
                            simpleReturnType = type.toString();
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

        if (bodyValue != null || formParams != null || multipartForm != null) {
            if (countNonNulls(bodyValue, formParams, multipartForm) > 1) {
                throw new IllegalArgumentException("Attempt to pass at least two of form, multipart form " +
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
            }
            ResultHandle mediaType = tryBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(MediaType.class, "valueOf", MediaType.class, String.class),
                    tryBlock.load(mediaTypeValue));

            ResultHandle entity = tryBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Entity.class, "entity", Entity.class, Object.class, MediaType.class),
                    bodyValue != null ? bodyValue : (formParams != null ? formParams : multipartForm),
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
                            tryBlock.loadClass(simpleReturnType));
                }
            } else if (returnCategory == ReturnCategory.UNI || returnCategory == ReturnCategory.COROUTINE) {
                ResultHandle rx = tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "rx", RxInvoker.class, Class.class),
                        builder, tryBlock.loadClass(UniInvoker.class));
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
                            tryBlock.loadClass(simpleReturnType));
                }
                if (returnCategory == ReturnCategory.COROUTINE) {
                    result = tryBlock.invokeStaticMethod(
                            MethodDescriptor.ofMethod(UNI_KT.toString(), "awaitSuspending", Object.class, Uni.class,
                                    CONTINUATION.toString()),
                            result, tryBlock.getMethodParam(continuationIndex));
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
                            tryBlock.loadClass(simpleReturnType));
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
                            tryBlock.loadClass(simpleReturnType));
                }
            } else if (returnCategory == ReturnCategory.UNI || returnCategory == ReturnCategory.COROUTINE) {
                ResultHandle rx = tryBlock.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "rx", RxInvoker.class, Class.class),
                        builder, tryBlock.loadClass(UniInvoker.class));
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
                            tryBlock.loadClass(simpleReturnType));
                }
                if (returnCategory == ReturnCategory.COROUTINE) {
                    result = tryBlock.invokeStaticMethod(
                            MethodDescriptor.ofMethod(UNI_KT.toString(), "awaitSuspending", Object.class, Uni.class,
                                    CONTINUATION.toString()),
                            result, tryBlock.getMethodParam(continuationIndex));
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
                            tryBlock.loadClass(simpleReturnType));
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

    private AssignableResultHandle createWebTargetForMethod(MethodCreator constructor, AssignableResultHandle baseTarget,
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

    private Optional<MethodInfo> getJavaMethod(ClassInfo interfaceClass, ResourceMethod method,
            MethodParameter[] parameters, IndexView index) {

        for (MethodInfo methodInfo : interfaceClass.methods()) {
            if (methodInfo.name().equals(method.getName()) && methodInfo.parameters().size() == parameters.length) {
                boolean matches = true;
                for (int i = 0; i < parameters.length; i++) {
                    MethodParameter actualParam = parameters[i];
                    Type parameterType = methodInfo.parameters().get(i);
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

    private void addBeanParamData(BytecodeCreator methodCreator,
            BytecodeCreator invocationBuilderEnricher, // Invocation.Builder executePut$$enrichInvocationBuilder${noOfBeanParam}(Invocation.Builder)
            AssignableResultHandle invocationBuilder,
            List<Item> beanParamItems,
            ResultHandle param,
            AssignableResultHandle target, // can only be used in the current method, not in `invocationBuilderEnricher`
            IndexView index) {
        BytecodeCreator creator = methodCreator.ifNotNull(param).trueBranch();
        BytecodeCreator invoEnricher = invocationBuilderEnricher.ifNotNull(invocationBuilderEnricher.getMethodParam(1))
                .trueBranch();
        for (Item item : beanParamItems) {
            switch (item.type()) {
                case BEAN_PARAM:
                    BeanParamItem beanParamItem = (BeanParamItem) item;
                    ResultHandle beanParamElementHandle = beanParamItem.extract(creator, param);
                    addBeanParamData(creator, invoEnricher, invocationBuilder, beanParamItem.items(),
                            beanParamElementHandle, target, index);
                    break;
                case QUERY_PARAM:
                    QueryParamItem queryParam = (QueryParamItem) item;
                    creator.assign(target,
                            addQueryParam(creator, target, queryParam.name(),
                                    queryParam.extract(creator, param),
                                    queryParam.getValueType(),
                                    index));
                    break;
                case COOKIE:
                    CookieParamItem cookieParam = (CookieParamItem) item;
                    addCookieParam(invoEnricher, invocationBuilder,
                            cookieParam.getCookieName(),
                            cookieParam.extract(invoEnricher, invoEnricher.getMethodParam(1)));
                    break;
                case HEADER_PARAM:
                    HeaderParamItem headerParam = (HeaderParamItem) item;
                    addHeaderParam(invoEnricher, invocationBuilder,
                            headerParam.getHeaderName(),
                            headerParam.extract(invoEnricher, invoEnricher.getMethodParam(1)));
                    break;
                default:
                    throw new IllegalStateException("Unimplemented"); // TODO form params, etc
            }
        }
    }

    // takes a result handle to target as one of the parameters, returns a result handle to a modified target
    private ResultHandle addQueryParam(BytecodeCreator methodCreator,
            ResultHandle target,
            String paramName,
            ResultHandle queryParamHandle,
            Type type,
            IndexView index) {
        ResultHandle paramArray;
        if (type.kind() == Type.Kind.ARRAY) {
            paramArray = methodCreator.checkCast(queryParamHandle, Object[].class);
        } else if (isCollection(type, index)) {
            paramArray = methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(ToObjectArray.class, "collection", Object[].class, Collection.class),
                    queryParamHandle);
        } else {
            paramArray = methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(ToObjectArray.class, "value", Object[].class, Object.class),
                    queryParamHandle);
        }

        return methodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(WebTarget.class, "queryParam", WebTarget.class,
                        String.class, Object[].class),
                target, methodCreator.load(paramName), paramArray);
    }

    private boolean isCollection(Type type, IndexView index) {
        if (type.kind() == Type.Kind.PRIMITIVE) {
            return false;
        }
        ClassInfo classInfo = index.getClassByName(type.name());
        if (classInfo == null) {
            return false;
        }
        return classInfo.interfaceNames().stream().anyMatch(DotName.createSimple(Collection.class.getName())::equals);
    }

    private void addHeaderParam(BytecodeCreator invoBuilderEnricher, AssignableResultHandle invocationBuilder,
            String paramName, ResultHandle headerParamHandle) {
        invoBuilderEnricher.assign(invocationBuilder,
                invoBuilderEnricher.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "header", Invocation.Builder.class, String.class,
                                Object.class),
                        invocationBuilder, invoBuilderEnricher.load(paramName), headerParamHandle));
    }

    private void addCookieParam(BytecodeCreator invoBuilderEnricher, AssignableResultHandle invocationBuilder,
            String paramName, ResultHandle cookieParamHandle) {
        invoBuilderEnricher.assign(invocationBuilder,
                invoBuilderEnricher.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "cookie", Invocation.Builder.class, String.class,
                                String.class),
                        invocationBuilder, invoBuilderEnricher.load(paramName), cookieParamHandle));
    }

    private enum ReturnCategory {
        BLOCKING,
        COMPLETION_STAGE,
        UNI,
        COROUTINE,
    }

}
