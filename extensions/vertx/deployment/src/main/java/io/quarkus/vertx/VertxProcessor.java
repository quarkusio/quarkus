/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.vertx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FunctionCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.quarkus.arc.Arc;
import org.jboss.quarkus.arc.ArcContainer;
import org.jboss.quarkus.arc.InjectableBean;
import org.jboss.quarkus.arc.InstanceHandle;
import org.jboss.quarkus.arc.processor.AnnotationStore;
import org.jboss.quarkus.arc.processor.AnnotationsTransformer;
import org.jboss.quarkus.arc.processor.BeanDeploymentValidator;
import org.jboss.quarkus.arc.processor.BeanInfo;
import org.jboss.quarkus.arc.processor.DotNames;
import org.jboss.quarkus.arc.processor.ScopeInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDeploymentValidatorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.vertx.runtime.ConsumeEvent;
import io.quarkus.vertx.runtime.EventConsumerInvoker;
import io.quarkus.vertx.runtime.VertxConfiguration;
import io.quarkus.vertx.runtime.VertxProducer;
import io.quarkus.vertx.runtime.VertxTemplate;
import io.vertx.core.eventbus.Message;

class VertxProcessor {

    private static final Logger LOGGER = Logger.getLogger(VertxProcessor.class.getName());

    private static final DotName CONSUME_EVENT = DotName.createSimple(ConsumeEvent.class.getName());
    private static final DotName MESSAGE = DotName.createSimple(Message.class.getName());
    private static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    private static final String INVOKER_SUFFIX = "_VertxInvoker";

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem build() {
        // This one may not be required after Vert.x 3.6.0 lands
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "java.util.LinkedHashMap"));

        return SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                .addNativeImageSystemProperty("vertx.disableDnsResolver", "true")
                .addRuntimeReinitializedClass("io.netty.handler.codec.http2.Http2CodecUtil")
                .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectEncoder")
                .addRuntimeInitializedClass("io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder")
                .addRuntimeInitializedClass("io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addRuntimeInitializedClass("io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED") //TODO: make configurable
                .build();
    }

    /**
     * The Vert.x configuration, if set.
     */
    VertxConfiguration vertx;

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return new AdditionalBeanBuildItem(false, VertxProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void build(VertxTemplate template, BeanContainerBuildItem beanContainer, BuildProducer<FeatureBuildItem> feature,
            List<EventConsumerBusinessMethodItem> messageConsumerBusinessMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            AnnotationProxyBuildItem annotationProxy) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.VERTX));
        Map<String, ConsumeEvent> messageConsumerConfigurations = new HashMap<>();
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(true, name, data));
            }
        };
        for (EventConsumerBusinessMethodItem businessMethod : messageConsumerBusinessMethods) {
            String invokerClass = generateInvoker(businessMethod.getBean(), businessMethod.getMethod(), classOutput);
            messageConsumerConfigurations.put(invokerClass,
                    annotationProxy.builder(businessMethod.getConsumeEvent(), ConsumeEvent.class)
                            .withDefaultValue("value", businessMethod.getBean().getBeanClass().toString()).build());
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, invokerClass));
        }
        template.configureVertx(beanContainer.getValue(), vertx, messageConsumerConfigurations);
    }

    @BuildStep
    public UnremovableBeanBuildItem unremovableBeans() {
        return new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(CONSUME_EVENT));
    }

    @BuildStep
    BeanDeploymentValidatorBuildItem beanDeploymentValidator(
            BuildProducer<EventConsumerBusinessMethodItem> messageConsumerBusinessMethods) {

        return new BeanDeploymentValidatorBuildItem(new BeanDeploymentValidator() {

            @Override
            public void validate(ValidationContext validationContext) {
                // We need to collect all business methods annotated with @MessageConsumer first
                AnnotationStore annotationStore = validationContext.get(Key.ANNOTATION_STORE);
                for (BeanInfo bean : validationContext.get(Key.BEANS)) {
                    if (bean.isClassBean()) {
                        // TODO: inherited business methods?
                        for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                            AnnotationInstance consumeEvent = annotationStore.getAnnotation(method, CONSUME_EVENT);
                            if (consumeEvent != null) {
                                // Validate method params and return type
                                List<Type> params = method.parameters();
                                if (params.size() != 1) {
                                    throw new IllegalStateException(String.format(
                                            "Event consumer business method must accept exactly one parameter: %s [method: %s, bean:%s",
                                            params, method, bean));
                                }
                                messageConsumerBusinessMethods
                                        .produce(new EventConsumerBusinessMethodItem(bean, method, consumeEvent));
                                LOGGER.debugf("Found event consumer business method %s declared on %s", method, bean);
                            }
                        }
                    }
                }
            }
        });
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (context.getAnnotations().isEmpty()) {
                    // Class with no annotations but with a method annotated with @ConsumeMessage
                    if (context.getTarget().asClass().annotations().containsKey(CONSUME_EVENT)) {
                        LOGGER.debugf(
                                "Found event consumer business methods on a class %s with no scope annotation - adding @Singleton",
                                context.getTarget());
                        context.transform().add(Singleton.class).done();
                    }
                }
            }
        });
    }

    private String generateInvoker(BeanInfo bean, MethodInfo method, ClassOutput classOutput) {

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + DotNames.simpleName(bean.getImplClazz());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        String targetPackage = DotNames.packageName(bean.getImplClazz().name());

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(EventConsumerInvoker.class).build();

        MethodCreator invoke = invokerCreator.getMethodCreator("invoke", void.class, Message.class);
        // InjectableBean<Foo: bean = Arc.container().bean("1");
        // InstanceHandle<Foo> handle = Arc.container().instance(bean);
        // handle.get().foo(message);
        ResultHandle containerHandle = invoke
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle beanHandle = invoke.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "bean", InjectableBean.class, String.class),
                containerHandle, invoke.load(bean.getIdentifier()));
        ResultHandle instanceHandle = invoke.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, InjectableBean.class),
                containerHandle, beanHandle);
        ResultHandle beanInstanceHandle = invoke
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);

        Type paramType = method.parameters().get(0);
        if (paramType.name().equals(MESSAGE)) {
            // Parameter is io.vertx.core.eventbus.Message
            invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class, Message.class),
                    beanInstanceHandle, invoke.getMethodParam(0));
        } else {
            // Parameter is payload
            ResultHandle payloadHandle = invoke.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Message.class, "body", Object.class), invoke.getMethodParam(0));
            ResultHandle replyHandle = invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(),
                            method.returnType().name().toString(), paramType.name().toString()),
                    beanInstanceHandle, payloadHandle);
            if (replyHandle != null) {
                if (method.returnType().name().equals(COMPLETION_STAGE)) {
                    // If the return type is CompletionStage use thenAccept()
                    FunctionCreator func = invoke.createFunction(Consumer.class);
                    BytecodeCreator funcBytecode = func.getBytecode();
                    funcBytecode.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Message.class, "reply", void.class, Object.class),
                            invoke.getMethodParam(0),
                            funcBytecode.getMethodParam(0));
                    funcBytecode.returnValue(null);
                    // returnValue.thenAccept(reply -> Message.reply(reply))
                    invoke.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(CompletionStage.class, "thenAccept", CompletionStage.class,
                                    Consumer.class),
                            replyHandle, func.getInstance());
                } else {
                    // Message.reply(returnValue)
                    invoke.invokeInterfaceMethod(MethodDescriptor.ofMethod(Message.class, "reply", void.class, Object.class),
                            invoke.getMethodParam(0),
                            replyHandle);
                }
            }
        }

        // handle.destroy() - destroy dependent instance afterwards
        if (bean.getScope() == ScopeInfo.DEPENDENT) {
            invoke.invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "destroy", void.class),
                    instanceHandle);
        }
        invoke.returnValue(null);

        invokerCreator.close();
        return generatedName.replace('/', '.');
    }
}
