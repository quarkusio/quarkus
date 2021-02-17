package io.quarkus.resteasy.reactive.client.deployment;

import static io.quarkus.deployment.Feature.RESTEASY_REACTIVE_JAXRS_CLIENT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.COMPLETION_STAGE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.WEB_APPLICATION_EXCEPTION;

import java.io.Closeable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
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

    public static final MethodDescriptor STRING_REPLACE_METHOD = MethodDescriptor.ofMethod(String.class, "replace",
            String.class,
            CharSequence.class, CharSequence.class);
    public static final MethodDescriptor STRING_VALUE_OF_METHOD = MethodDescriptor.ofMethod(String.class, "valueOf",
            String.class, Object.class);

    @BuildStep
    void addFeature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(RESTEASY_REACTIVE_JAXRS_CLIENT));
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
       }
       ```
       Generates the following (with MicroProfile enricher):
       ```
       public class BaseClient$$QuarkusRestClientInterface implements Closeable, BaseClient {
          final WebTarget target;
       
          public BaseClient$$QuarkusRestClientInterface(WebTarget var1) {
             WebTarget var2 = var1.path("");
             this.target = var2;
          }
       
          public void close() {
             ((WebTargetImpl)this.target).getRestClient().close();
          }
       
          public Response executeBasePost() {
             WebTarget var1 = this.target;
             String var2 = "/base";
             var1 = var1.path(var2);
             NoOpHeaderFiller var4 = NoOpHeaderFiller.INSTANCE;
             DefaultClientHeadersFactoryImpl var5 = new DefaultClientHeadersFactoryImpl();
             Class[] var3 = new Class[0];
             Method var6 = BaseClient.class.getMethod("executeBasePost", var3);
             MicroProfileRestRequestClientFilter var7 = new MicroProfileRestRequestClientFilter((HeaderFiller)var4, (ClientHeadersFactory)var5, var6);
             var1 = (WebTarget)((Configurable)var1).register(var7);
             String[] var8 = new String[]{"application/json"};
             Builder var9 = var1.request(var8);
       
             try {
                return (Response)var9.method("POST", Response.class);
             } catch (ProcessingException var12) {
                Throwable var11 = ((Throwable)var12).getCause();
                if (!(var11 instanceof WebApplicationException)) {
                   throw (Throwable)var12;
                } else {
                   throw var11;
                }
             }
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
        for (ResourceMethod i : restClientInterface.getMethods()) {
            if (i.getHttpMethod() == null) {
                subResource = true;
                break;
            }
        }
        if (subResource) {
            return null;
        }
        // TODO: ATM this may create a web target on each call of a method (each request)
        // TODO: optimize it
        String name = restClientInterface.getClassName() + "$$QuarkusRestClientInterface";
        MethodDescriptor ctorDesc = MethodDescriptor.ofConstructor(name, WebTarget.class.getName());
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                name, null, Object.class.getName(),
                Closeable.class.getName(), restClientInterface.getClassName())) {

            FieldDescriptor targetFieldDescriptor = FieldDescriptor.of(name, "target", WebTarget.class);
            c.getFieldCreator(targetFieldDescriptor).setModifiers(Modifier.FINAL);

            MethodCreator ctor = c.getMethodCreator(ctorDesc);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());

            AssignableResultHandle globalTarget = ctor.createVariable(WebTarget.class);

            ctor.assign(globalTarget,
                    ctor.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                            ctor.getMethodParam(0), ctor.load(restClientInterface.getPath())));

            for (JaxrsClientEnricherBuildItem enricher : enrichers) {
                enricher.getEnricher().forClass(ctor, globalTarget, interfaceClass, index);
            }
            ctor.writeInstanceField(targetFieldDescriptor, ctor.getThis(), globalTarget);
            ctor.returnValue(null);

            // create `void close()` method:
            MethodCreator closeCreator = c.getMethodCreator(MethodDescriptor.ofMethod(Closeable.class, "close", void.class));
            ResultHandle webTarget = closeCreator.readInstanceField(targetFieldDescriptor, closeCreator.getThis());
            ResultHandle webTargetImpl = closeCreator.checkCast(webTarget, WebTargetImpl.class);
            ResultHandle restClient = closeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(WebTargetImpl.class, "getRestClient", ClientImpl.class), webTargetImpl);
            closeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ClientImpl.class, "close", void.class), restClient);
            closeCreator.returnValue(null);

            // create methods from the jaxrs interface
            int methodIndex = 0;
            for (ResourceMethod method : restClientInterface.getMethods()) {
                methodIndex++;

                String[] javaMethodParameters = new String[method.getParameters().length];
                for (int i = 0; i < method.getParameters().length; i++) {
                    MethodParameter param = method.getParameters()[i];
                    javaMethodParameters[i] = param.declaredType != null ? param.declaredType : param.type;
                }

                MethodCreator methodCreator = c.getMethodCreator(method.getName(), method.getSimpleReturnType(),
                        javaMethodParameters);
                MethodInfo jandexMethod = getJavaMethod(interfaceClass, method, method.getParameters(), index)
                        .orElseThrow(() -> new RuntimeException(
                                "Failed to find matching java method for " + method + " on " + interfaceClass
                                        + ". It may have unresolved parameter types (generics)"));

                AssignableResultHandle target = methodCreator.createVariable(WebTarget.class);

                methodCreator.assign(target, methodCreator.readInstanceField(targetFieldDescriptor, methodCreator.getThis()));

                Integer bodyParameterIdx = null;

                Map<MethodDescriptor, ResultHandle> invocationBuilderEnrichers = new HashMap<>();

                AssignableResultHandle path = methodCreator.createVariable(String.class);
                methodCreator.assign(path, methodCreator.load(method.getPath()));

                for (int paramIdx = 0; paramIdx < method.getParameters().length; ++paramIdx) {
                    MethodParameter param = method.getParameters()[paramIdx];
                    if (param.parameterType == ParameterType.QUERY) {
                        //TODO: converters
                        methodCreator.assign(target, addQueryParam(methodCreator, target, param.name,
                                methodCreator.getMethodParam(paramIdx)));
                    } else if (param.parameterType == ParameterType.BEAN) {
                        ClientBeanParamInfo beanParam = (ClientBeanParamInfo) param;
                        MethodDescriptor enricherMethod = MethodDescriptor.ofMethod(name,
                                method.getName() + "$$" + methodIndex + "$$enrichInvocationBuilder$$" + paramIdx,
                                Invocation.Builder.class,
                                Invocation.Builder.class, param.type);
                        MethodCreator enricherMethodCreator = c.getMethodCreator(enricherMethod);

                        AssignableResultHandle invocationBuilderRef = enricherMethodCreator
                                .createVariable(Invocation.Builder.class);
                        enricherMethodCreator.assign(invocationBuilderRef, enricherMethodCreator.getMethodParam(0));
                        addBeanParamData(methodCreator, enricherMethodCreator,
                                invocationBuilderRef, beanParam.getItems(),
                                methodCreator.getMethodParam(paramIdx), target);

                        enricherMethodCreator.returnValue(invocationBuilderRef);
                        invocationBuilderEnrichers.put(enricherMethod, methodCreator.getMethodParam(paramIdx));
                    } else if (param.parameterType == ParameterType.PATH) {
                        ResultHandle paramPlaceholder = methodCreator.load(String.format("{%s}", param.name));
                        ResultHandle pathParamValue = methodCreator.invokeStaticMethod(STRING_VALUE_OF_METHOD,
                                methodCreator.getMethodParam(paramIdx));

                        ResultHandle newPath = methodCreator.invokeVirtualMethod(STRING_REPLACE_METHOD, path, paramPlaceholder,
                                pathParamValue);
                        methodCreator.assign(path, newPath);
                    } else if (param.parameterType == ParameterType.BODY) {
                        bodyParameterIdx = paramIdx;
                    } else if (param.parameterType == ParameterType.HEADER) {
                        MethodDescriptor enricherMethod = MethodDescriptor.ofMethod(name,
                                method.getName() + "$$" + methodIndex + "$$enrichInvocationBuilder$$" + paramIdx,
                                Invocation.Builder.class,
                                Invocation.Builder.class, param.type);
                        MethodCreator enricherMethodCreator = c.getMethodCreator(enricherMethod);

                        AssignableResultHandle invocationBuilderRef = enricherMethodCreator
                                .createVariable(Invocation.Builder.class);
                        enricherMethodCreator.assign(invocationBuilderRef, enricherMethodCreator.getMethodParam(0));
                        addHeaderParam(enricherMethodCreator, invocationBuilderRef, param.name,
                                enricherMethodCreator.getMethodParam(1));
                        enricherMethodCreator.returnValue(invocationBuilderRef);
                        invocationBuilderEnrichers.put(enricherMethod, methodCreator.getMethodParam(paramIdx));
                    }
                }

                if (method.getPath() != null) {
                    methodCreator.assign(target,
                            methodCreator.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                                    target, path));
                }

                for (JaxrsClientEnricherBuildItem enricher : enrichers) {
                    enricher.getEnricher().forMethod(methodCreator, interfaceClass, jandexMethod, target, index,
                            generatedClassBuildItemBuildProducer, methodIndex);
                }

                ResultHandle builder;
                if (method.getProduces() == null || method.getProduces().length == 0) { // this should never happen!
                    builder = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class), target);
                } else {

                    ResultHandle array = methodCreator.newArray(String.class, method.getProduces().length);
                    for (int i = 0; i < method.getProduces().length; ++i) {
                        methodCreator.writeArrayValue(array, i, methodCreator.load(method.getProduces()[i]));
                    }
                    builder = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class, String[].class),
                            target, array);
                }

                for (Map.Entry<MethodDescriptor, ResultHandle> invocationBuilderEnricher : invocationBuilderEnrichers
                        .entrySet()) {
                    builder = methodCreator.invokeVirtualMethod(invocationBuilderEnricher.getKey(), methodCreator.getThis(),
                            builder, invocationBuilderEnricher.getValue());
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
