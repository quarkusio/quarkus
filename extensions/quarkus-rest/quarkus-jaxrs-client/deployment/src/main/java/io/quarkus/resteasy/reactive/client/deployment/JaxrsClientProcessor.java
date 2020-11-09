package io.quarkus.resteasy.reactive.client.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.Serialisers;
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

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.client.runtime.ResteasyReactiveClientRecorder;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusFactoryCreator;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusInvokerFactory;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.SerializersUtil;
import io.quarkus.resteasy.reactive.common.runtime.QuarkusRestConfig;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.runtime.RuntimeValue;

public class JaxrsClientProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupClientProxies(ResteasyReactiveClientRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            ResourceScanningResultBuildItem resourceScanningResultBuildItem,
            QuarkusRestConfig config,
            RecorderContext recorderContext,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer) {

        Serialisers serialisers = recorder.createSerializers();

        SerializersUtil.setupSerializers(recorder, reflectiveClassBuildItemBuildProducer, messageBodyReaderBuildItems,
                messageBodyWriterBuildItems, beanContainerBuildItem, applicationResultBuildItem, serialisers,
                RuntimeType.CLIENT);

        if (resourceScanningResultBuildItem == null || resourceScanningResultBuildItem.getPathInterfaces().isEmpty()) {
            recorder.setupClientProxies(new HashMap<>());
            return;
        }

        AdditionalReaders additionalReaders = new AdditionalReaders();
        AdditionalWriters additionalWriters = new AdditionalWriters();

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        ClientEndpointIndexer clientEndpointIndexer = new ClientEndpointIndexer.Builder()
                .setIndex(index)
                .setEndpointInvokerFactory(new QuarkusInvokerFactory(generatedClassBuildItemBuildProducer, recorder))
                .setExistingConverters(new HashMap<>())
                .setScannedResourcePaths(resourceScanningResultBuildItem.getScannedResourcePaths())
                .setConfig(new ResteasyReactiveConfig(config.inputBufferSize.asLongValue(), config.singleDefaultProduces))
                .setAdditionalReaders(additionalReaders)
                .setHttpAnnotationToMethod(resourceScanningResultBuildItem.getHttpAnnotationToMethod())
                .setInjectableBeans(new HashMap<>())
                .setFactoryCreator(new QuarkusFactoryCreator(recorder, beanContainerBuildItem.getValue()))
                .setAdditionalWriters(additionalWriters)
                .setDefaultBlocking(applicationResultBuildItem.isBlocking())
                .setHasRuntimeConverters(false).build();

        List<RestClientInterface> clientDefinitions = new ArrayList<>();
        for (Map.Entry<DotName, String> i : resourceScanningResultBuildItem.getPathInterfaces().entrySet()) {
            ClassInfo clazz = index.getClassByName(i.getKey());
            //these interfaces can also be clients
            //so we generate client proxies for them
            RestClientInterface clientProxy = clientEndpointIndexer.createClientProxy(clazz,
                    i.getValue());
            if (clientProxy != null) {
                clientDefinitions.add(clientProxy);
            }
        }
        Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations = generateClientInvokers(recorderContext,
                clientDefinitions, generatedClassBuildItemBuildProducer);

        recorder.setupClientProxies(clientImplementations);

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

    private Map<String, RuntimeValue<Function<WebTarget, ?>>> generateClientInvokers(RecorderContext recorderContext,
            List<RestClientInterface> clientDefinitions,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {
        Map<String, RuntimeValue<Function<WebTarget, ?>>> ret = new HashMap<>();
        for (RestClientInterface restClientInterface : clientDefinitions) {
            boolean subResource = false;
            //if the interface contains sub resource locator methods we ignore it
            for (ResourceMethod i : restClientInterface.getMethods()) {
                if (i.getHttpMethod() == null) {
                    subResource = true;
                }
                break;
            }
            if (subResource) {
                continue;
            }
            String name = restClientInterface.getClassName() + "$$QuarkusRestClientInterface";
            MethodDescriptor ctorDesc = MethodDescriptor.ofConstructor(name, WebTarget.class.getName());
            try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                    name, null, Object.class.getName(), restClientInterface.getClassName())) {

                FieldDescriptor target = FieldDescriptor.of(name, "target", WebTarget.class);
                c.getFieldCreator(target).setModifiers(Modifier.FINAL);

                MethodCreator ctor = c.getMethodCreator(ctorDesc);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());

                ResultHandle res = ctor.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                        ctor.getMethodParam(0), ctor.load(restClientInterface.getPath()));
                ctor.writeInstanceField(target, ctor.getThis(), res);
                ctor.returnValue(null);

                for (ResourceMethod method : restClientInterface.getMethods()) {
                    MethodCreator m = c.getMethodCreator(method.getName(), method.getReturnType(),
                            Arrays.stream(method.getParameters()).map(s -> s.type).toArray());
                    ResultHandle tg = m.readInstanceField(target, m.getThis());
                    if (method.getPath() != null) {
                        tg = m.invokeInterfaceMethod(MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class,
                                String.class), tg, m.load(method.getPath()));
                    }

                    for (int i = 0; i < method.getParameters().length; ++i) {
                        MethodParameter p = method.getParameters()[i];
                        if (p.parameterType == ParameterType.QUERY) {
                            //TODO: converters
                            ResultHandle array = m.newArray(Object.class, 1);
                            m.writeArrayValue(array, 0, m.getMethodParam(i));
                            tg = m.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(WebTarget.class, "queryParam", WebTarget.class,
                                            String.class, Object[].class),
                                    tg, m.load(p.name), array);
                        }

                    }

                    ResultHandle builder;
                    if (method.getProduces() == null || method.getProduces().length == 0) {
                        builder = m.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class), tg);
                    } else {

                        ResultHandle array = m.newArray(String.class, method.getProduces().length);
                        for (int i = 0; i < method.getProduces().length; ++i) {
                            m.writeArrayValue(array, i, m.load(method.getProduces()[i]));
                        }
                        builder = m.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class, String[].class),
                                tg, array);
                    }
                    //TODO: async return types

                    ResultHandle result = m
                            .invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                            Class.class),
                                    builder, m.load(method.getHttpMethod()), m.loadClass(method.getSimpleReturnType()));

                    m.returnValue(result);
                }

            }
            String creatorName = restClientInterface.getClassName() + "$$QuarkusRestClientInterfaceCreator";
            try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                    creatorName, null, Object.class.getName(), Function.class.getName())) {

                MethodCreator apply = c
                        .getMethodCreator(MethodDescriptor.ofMethod(creatorName, "apply", Object.class, Object.class));
                apply.returnValue(apply.newInstance(ctorDesc, apply.getMethodParam(0)));
            }
            ret.put(restClientInterface.getClassName(), recorderContext.newInstance(creatorName));

        }
        return ret;
    }
}
