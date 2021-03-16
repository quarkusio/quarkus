package io.quarkus.resteasy.reactive.client.deployment;

import static io.quarkus.deployment.Feature.RESTEASY_REACTIVE_JAXRS_CLIENT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.WEB_APPLICATION_EXCEPTION;

import java.io.Closeable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.impl.AsyncInvokerImpl;
import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
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
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.arc.processor.Types;
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
import io.quarkus.resteasy.reactive.client.deployment.beanparam.BeanParamItem;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.ClientBeanParamInfo;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.CookieParamItem;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.HeaderParamItem;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.Item;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.QueryParamItem;
import io.quarkus.resteasy.reactive.client.runtime.ClientResponseBuilderFactory;
import io.quarkus.resteasy.reactive.client.runtime.ResteasyReactiveClientRecorder;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusFactoryCreator;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.SerializersUtil;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.runtime.RuntimeValue;

public class JaxrsClientProcessor {

    private static final Logger log = Logger.getLogger(JaxrsClientProcessor.class);

    private static final MethodDescriptor WEB_TARGET_RESOLVE_TEMPLATE_METHOD = MethodDescriptor.ofMethod(WebTarget.class,
            "resolveTemplate",
            WebTarget.class,
            String.class, Object.class);

    @BuildStep
    void addFeature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(RESTEASY_REACTIVE_JAXRS_CLIENT));
    }

    @BuildStep
    void registerClientResponseBuilder(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        serviceProviders.produce(new ServiceProviderBuildItem(ResponseBuilderFactory.class.getName(),
                ClientResponseBuilderFactory.class.getName()));
    }

    @BuildStep
    void initializeRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(AsyncInvokerImpl.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupClientProxies(ResteasyReactiveClientRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            List<JaxrsClientEnricherBuildItem> enricherBuildItems,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            ResteasyReactiveConfig config,
            RecorderContext recorderContext,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer,
            List<RestClientDefaultProducesBuildItem> defaultConsumes,
            List<RestClientDefaultConsumesBuildItem> defaultProduces) {
        String defaultConsumesType = defaultMediaType(defaultConsumes, MediaType.APPLICATION_OCTET_STREAM);
        String defaultProducesType = defaultMediaType(defaultProduces, MediaType.TEXT_PLAIN);

        Serialisers serialisers = recorder.createSerializers();

        SerializersUtil.setupSerializers(recorder, reflectiveClassBuildItemBuildProducer, messageBodyReaderBuildItems,
                messageBodyWriterBuildItems, beanContainerBuildItem, applicationResultBuildItem, serialisers,
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
                .setConfig(new org.jboss.resteasy.reactive.common.ResteasyReactiveConfig(config.inputBufferSize.asLongValue(),
                        config.singleDefaultProduces, config.defaultProduces))
                .setAdditionalReaders(additionalReaders)
                .setHttpAnnotationToMethod(result.getHttpAnnotationToMethod())
                .setInjectableBeans(new HashMap<>())
                .setFactoryCreator(new QuarkusFactoryCreator(recorder, beanContainerBuildItem.getValue()))
                .setAdditionalWriters(additionalWriters)
                .setDefaultBlocking(applicationResultBuildItem.getResult().isBlocking())
                .setHasRuntimeConverters(false)
                .setDefaultProduces(defaultProducesType)
                .build();

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
                            enricherBuildItems, generatedClassBuildItemBuildProducer, clazz, index, defaultConsumesType);
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
            Class readerClass = additionalReader.getHandlerClass();
            ResourceReader reader = new ResourceReader();
            reader.setBuiltin(true);
            reader.setFactory(recorder.factory(readerClass.getName(), beanContainerBuildItem.getValue()));
            reader.setMediaTypeStrings(Collections.singletonList(additionalReader.getMediaType()));
            recorder.registerReader(serialisers, additionalReader.getEntityClass().getName(), reader);
            reflectiveClassBuildItemBuildProducer
                    .produce(new ReflectiveClassBuildItem(true, false, false, readerClass.getName()));
        }

        for (AdditionalReaderWriter.Entry entry : additionalWriters.get()) {
            Class writerClass = entry.getHandlerClass();
            ResourceWriter writer = new ResourceWriter();
            writer.setBuiltin(true);
            writer.setFactory(recorder.factory(writerClass.getName(), beanContainerBuildItem.getValue()));
            writer.setMediaTypeStrings(Collections.singletonList(entry.getMediaType()));
            recorder.registerWriter(serialisers, entry.getEntityClass().getName(), writer);
            reflectiveClassBuildItemBuildProducer
                    .produce(new ReflectiveClassBuildItem(true, false, false, writerClass.getName()));
        }

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
    public void registerInvocationCallbacks(CombinedIndexBuildItem index, ResteasyReactiveClientRecorder recorder) {

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
     */
    private RuntimeValue<Function<WebTarget, ?>> generateClientInvoker(RecorderContext recorderContext,
            RestClientInterface restClientInterface, List<JaxrsClientEnricherBuildItem> enrichers,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, ClassInfo interfaceClass,
            IndexView index, String defaultMediaType) {
        boolean subResource = false;
        //if the interface contains sub resource locator methods we ignore it
        // TODO: support subresources
        for (ResourceMethod i : restClientInterface.getMethods()) {
            if (i.getHttpMethod() == null) {
                subResource = true;
                break;
            }
        }
        if (subResource) {
            return null;
        }

        String name = restClientInterface.getClassName() + "$$QuarkusRestClientInterface";
        MethodDescriptor ctorDesc = MethodDescriptor.ofConstructor(name, WebTarget.class.getName());
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                name, null, Object.class.getName(),
                Closeable.class.getName(), restClientInterface.getClassName())) {

            //
            // initialize basic WebTarget in constructor
            //

            MethodCreator constructor = c.getMethodCreator(ctorDesc);
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), constructor.getThis());

            AssignableResultHandle baseTarget = constructor.createVariable(WebTarget.class);
            constructor.assign(baseTarget,
                    constructor.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                            constructor.getMethodParam(0), constructor.load(restClientInterface.getPath())));

            for (JaxrsClientEnricherBuildItem enricher : enrichers) {
                enricher.getEnricher().forClass(constructor, baseTarget, interfaceClass, index);
            }

            //
            // go through all the methods of the jaxrs interface. Create specific WebTargets (in the constructor) and methods
            //
            int methodIndex = 0;
            List<FieldDescriptor> webTargets = new ArrayList<>();
            for (ResourceMethod method : restClientInterface.getMethods()) {
                methodIndex++;

                // constructor: initializing the immutable part of the method-specific web target
                FieldDescriptor webTargetForMethod = FieldDescriptor.of(name, "target" + methodIndex, WebTarget.class);
                c.getFieldCreator(webTargetForMethod).setModifiers(Modifier.FINAL);
                webTargets.add(webTargetForMethod);

                AssignableResultHandle constructorTarget = createWebTargetForMethod(constructor, baseTarget, method);
                constructor.writeInstanceField(webTargetForMethod, constructor.getThis(), constructorTarget);

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

                // generate implementation for a method from jaxrs interface:
                MethodCreator methodCreator = c.getMethodCreator(method.getName(), method.getSimpleReturnType(),
                        javaMethodParameters);

                AssignableResultHandle methodTarget = methodCreator.createVariable(WebTarget.class);
                methodCreator.assign(methodTarget,
                        methodCreator.readInstanceField(webTargetForMethod, methodCreator.getThis()));

                Integer bodyParameterIdx = null;
                Map<MethodDescriptor, ResultHandle> invocationBuilderEnrichers = new HashMap<>();

                for (int paramIdx = 0; paramIdx < method.getParameters().length; ++paramIdx) {
                    MethodParameter param = method.getParameters()[paramIdx];
                    if (param.parameterType == ParameterType.QUERY) {
                        //TODO: converters

                        // query params have to be set on a method-level web target (they vary between invocations)
                        methodCreator.assign(methodTarget, addQueryParam(methodCreator, methodTarget, param.name,
                                methodCreator.getMethodParam(paramIdx)));
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
                                methodCreator.getMethodParam(paramIdx), methodTarget);

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

                for (JaxrsClientEnricherBuildItem enricher : enrichers) {
                    enricher.getEnricher()
                            .forMethod(c, constructor, methodCreator, interfaceClass, jandexMethod, builder,
                                    index, generatedClassBuildItemBuildProducer, methodIndex);
                }

                Type returnType = jandexMethod.returnType();
                boolean completionStage = false;
                String simpleReturnType = method.getSimpleReturnType();

                ResultHandle genericReturnType = null;
                if (returnType.kind() == Type.Kind.PARAMETERIZED_TYPE) {

                    ParameterizedType paramType = returnType.asParameterizedType();
                    if (paramType.name().equals(COMPLETION_STAGE)) {
                        completionStage = true;

                        // CompletionStage has one type argument:
                        if (paramType.arguments().isEmpty()) {
                            simpleReturnType = Object.class.getName();
                        } else {
                            Type type = paramType.arguments().get(0);
                            if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                                ResultHandle currentThread = methodCreator
                                        .invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD);
                                ResultHandle tccl = methodCreator.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL,
                                        currentThread);
                                genericReturnType = Types.getParameterizedType(methodCreator, tccl, type.asParameterizedType());
                            } else {
                                simpleReturnType = type.toString();
                            }
                        }
                    } else {
                        ResultHandle currentThread = methodCreator.invokeStaticMethod(MethodDescriptors.THREAD_CURRENT_THREAD);
                        ResultHandle tccl = methodCreator.invokeVirtualMethod(MethodDescriptors.THREAD_GET_TCCL, currentThread);
                        ResultHandle parameterizedType = Types.getParameterizedType(methodCreator, tccl,
                                paramType);

                        genericReturnType = methodCreator.newInstance(
                                MethodDescriptor.ofConstructor(GenericType.class, java.lang.reflect.Type.class),
                                parameterizedType);
                    }
                }

                ResultHandle result;

                String mediaTypeValue = defaultMediaType;

                // if a JAXRS method throws an exception, unwrap the ProcessingException and throw the exception instead
                // Similarly with WebApplicationException
                TryBlock tryBlock = methodCreator.tryBlock();

                List<Type> exceptionTypes = jandexMethod.exceptions();
                Set<DotName> exceptions = new HashSet<>();
                exceptions.add(WEB_APPLICATION_EXCEPTION);
                for (Type exceptionType : exceptionTypes) {
                    exceptions.add(exceptionType.name());
                }

                CatchBlockCreator catchBlock = tryBlock.addCatch(ProcessingException.class);
                ResultHandle caughtException = catchBlock.getCaughtException();
                ResultHandle cause = catchBlock.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Throwable.class, "getCause", Throwable.class),
                        caughtException);
                for (DotName exception : exceptions) {
                    catchBlock.ifTrue(catchBlock.instanceOf(cause, exception.toString()))
                            .trueBranch().throwException(cause);
                }

                catchBlock.throwException(caughtException);

                if (bodyParameterIdx != null) {
                    String[] consumes = method.getConsumes();
                    if (consumes != null && consumes.length > 0) {

                        if (consumes.length > 1) {
                            throw new IllegalArgumentException(
                                    "Multiple `@Consumes` values used in a MicroProfile Rest Client: " +
                                            restClientInterface.getClassName()
                                            + " Unable to determine a single `Content-Type`.");
                        }
                        mediaTypeValue = consumes[0];
                    }
                    ResultHandle mediaType = tryBlock.invokeStaticMethod(
                            MethodDescriptor.ofMethod(MediaType.class, "valueOf", MediaType.class, String.class),
                            tryBlock.load(mediaTypeValue));

                    ResultHandle entity = tryBlock.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Entity.class, "entity", Entity.class, Object.class, MediaType.class),
                            tryBlock.getMethodParam(bodyParameterIdx),
                            mediaType);

                    if (completionStage) {
                        ResultHandle async = tryBlock.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(Invocation.Builder.class, "async", AsyncInvoker.class),
                                builder);
                        // with entity
                        if (genericReturnType != null) {
                            result = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(CompletionStageRxInvoker.class, "method",
                                            CompletionStage.class, String.class,
                                            Entity.class, GenericType.class),
                                    async, tryBlock.load(method.getHttpMethod()), entity,
                                    genericReturnType);
                        } else {
                            result = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(CompletionStageRxInvoker.class, "method", CompletionStage.class,
                                            String.class,
                                            Entity.class, Class.class),
                                    async, tryBlock.load(method.getHttpMethod()), entity,
                                    tryBlock.loadClass(simpleReturnType));
                        }
                    } else {
                        if (genericReturnType != null) {
                            result = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                            Entity.class, GenericType.class),
                                    builder, tryBlock.load(method.getHttpMethod()), entity,
                                    genericReturnType);
                        } else {
                            result = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                            Entity.class, Class.class),
                                    builder, tryBlock.load(method.getHttpMethod()), entity,
                                    tryBlock.loadClass(simpleReturnType));
                        }
                    }
                } else {

                    if (completionStage) {
                        ResultHandle async = tryBlock.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(Invocation.Builder.class, "async", AsyncInvoker.class),
                                builder);
                        if (genericReturnType != null) {
                            result = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(CompletionStageRxInvoker.class, "method",
                                            CompletionStage.class, String.class,
                                            GenericType.class),
                                    async, tryBlock.load(method.getHttpMethod()), genericReturnType);
                        } else {
                            result = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(CompletionStageRxInvoker.class, "method", CompletionStage.class,
                                            String.class,
                                            Class.class),
                                    async, tryBlock.load(method.getHttpMethod()),
                                    tryBlock.loadClass(simpleReturnType));
                        }
                    } else {
                        if (genericReturnType != null) {
                            result = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                            GenericType.class),
                                    builder, tryBlock.load(method.getHttpMethod()), genericReturnType);
                        } else {
                            result = tryBlock.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                            Class.class),
                                    builder, tryBlock.load(method.getHttpMethod()),
                                    tryBlock.loadClass(simpleReturnType));
                        }
                    }
                }
                tryBlock.returnValue(result);
            }

            constructor.returnValue(null);

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
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                creatorName, null, Object.class.getName(), Function.class.getName())) {

            MethodCreator apply = c
                    .getMethodCreator(MethodDescriptor.ofMethod(creatorName, "apply", Object.class, Object.class));
            apply.returnValue(apply.newInstance(ctorDesc, apply.getMethodParam(0)));
        }
        return recorderContext.newInstance(creatorName);

    }

    private AssignableResultHandle createWebTargetForMethod(MethodCreator constructor, AssignableResultHandle baseTarget,
            ResourceMethod method) {
        AssignableResultHandle target = constructor.createVariable(WebTarget.class);
        constructor.assign(target, baseTarget);

        if (method.getPath() != null) {
            AssignableResultHandle path = constructor.createVariable(String.class);
            constructor.assign(path, constructor.load(method.getPath()));
            constructor.assign(target,
                    constructor.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                            target, path));
        }
        return target;
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
            AssignableResultHandle target // can only be used in the current method, not in `invocationBuilderEnricher`
    ) {
        BytecodeCreator creator = methodCreator.ifNotNull(param).trueBranch();
        BytecodeCreator invoEnricher = invocationBuilderEnricher.ifNotNull(invocationBuilderEnricher.getMethodParam(1))
                .trueBranch();
        for (Item item : beanParamItems) {
            switch (item.type()) {
                case BEAN_PARAM:
                    BeanParamItem beanParamItem = (BeanParamItem) item;
                    ResultHandle beanParamElementHandle = beanParamItem.extract(creator, param);
                    addBeanParamData(creator, invoEnricher, invocationBuilder, beanParamItem.items(),
                            beanParamElementHandle, target);
                    break;
                case QUERY_PARAM:
                    QueryParamItem queryParam = (QueryParamItem) item;
                    creator.assign(target,
                            addQueryParam(creator, target, queryParam.name(), queryParam.extract(creator, param)));
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
            String paramName, ResultHandle queryParamHandle) {
        ResultHandle array = methodCreator.newArray(Object.class, 1);
        methodCreator.writeArrayValue(array, 0, queryParamHandle);
        ResultHandle alteredTarget = methodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(WebTarget.class, "queryParam", WebTarget.class,
                        String.class, Object[].class),
                target, methodCreator.load(paramName), array);
        return alteredTarget;
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

}
