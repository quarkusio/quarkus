package io.quarkus.vertx.deployment;

import static io.quarkus.vertx.deployment.VertxConstants.CONSUME_EVENT;
import static io.quarkus.vertx.deployment.VertxConstants.isMessage;
import static io.quarkus.vertx.deployment.VertxConstants.isMessageHeaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.deployment.CurrentContextFactoryBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.runtime.VertxEventBusConsumerRecorder;
import io.quarkus.vertx.runtime.VertxProducer;
import io.smallrye.common.annotation.RunOnVirtualThread;

class VertxProcessor {

    private static final Logger LOGGER = Logger.getLogger(VertxProcessor.class.getName());

    @BuildStep
    void featureAndCapability(BuildProducer<FeatureBuildItem> feature, BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem(Feature.VERTX));
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(VertxProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    VertxBuildItem build(CoreVertxBuildItem vertx, VertxEventBusConsumerRecorder recorder,
            List<EventConsumerBusinessMethodItem> messageConsumerBusinessMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            AnnotationProxyBuildItem annotationProxy, LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown,
            BuildProducer<ServiceStartBuildItem> serviceStart, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<MessageCodecBuildItem> codecs, LocalCodecSelectorTypesBuildItem localCodecSelectorTypes) {
        Map<String, ConsumeEvent> messageConsumerConfigurations = new HashMap<>();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        for (EventConsumerBusinessMethodItem businessMethod : messageConsumerBusinessMethods) {
            String invokerClass = EventBusConsumer.generateInvoker(businessMethod.getBean(), businessMethod.getMethod(),
                    businessMethod.getConsumeEvent(), classOutput);
            messageConsumerConfigurations.put(invokerClass,
                    annotationProxy.builder(businessMethod.getConsumeEvent(), ConsumeEvent.class)
                            .withDefaultValue("value", businessMethod.getBean().getBeanClass().toString())
                            .build(classOutput));
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(invokerClass).build());
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Map<Class<?>, Class<?>> codecByClass = new HashMap<>();
        for (MessageCodecBuildItem messageCodecItem : codecs) {
            codecByClass.put(tryLoad(messageCodecItem.getType(), tccl), tryLoad(messageCodecItem.getCodec(), tccl));
        }

        List<Class<?>> selectorTypes = new ArrayList<>();
        for (String name : localCodecSelectorTypes.getTypes()) {
            selectorTypes.add(tryLoad(name, tccl));
        }

        recorder.configureVertx(vertx.getVertx(), messageConsumerConfigurations,
                launchMode.getLaunchMode(),
                shutdown, codecByClass, selectorTypes);
        serviceStart.produce(new ServiceStartBuildItem("vertx"));
        return new VertxBuildItem(recorder.forceStart(vertx.getVertx()));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void currentContextFactory(BuildProducer<CurrentContextFactoryBuildItem> currentContextFactory,
            VertxBuildConfig buildConfig, VertxEventBusConsumerRecorder recorder) {
        if (buildConfig.customizeArcContext()) {
            currentContextFactory.produce(new CurrentContextFactoryBuildItem(recorder.currentContextFactory()));
        }
    }

    @BuildStep
    public UnremovableBeanBuildItem unremovableBeans() {
        return new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(CONSUME_EVENT));
    }

    @BuildStep
    void collectEventConsumers(
            BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<EventConsumerBusinessMethodItem> messageConsumerBusinessMethods,
            BuildProducer<BeanConfiguratorBuildItem> errors) {
        // We need to collect all business methods annotated with @ConsumeEvent first
        AnnotationStore annotationStore = beanRegistrationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        for (BeanInfo bean : beanRegistrationPhase.getContext().beans().classBeans()) {
            for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                AnnotationInstance consumeEvent = annotationStore.getAnnotation(method, CONSUME_EVENT);
                if (consumeEvent != null) {
                    // Validate method params and return type
                    List<Type> params = method.parameterTypes();
                    if (params.size() == 2) {
                        if (!isMessageHeaders(params.get(0).name())) {
                            // If there are two parameters, the first must be message headers.
                            throw new IllegalStateException(String.format(
                                    "An event consumer business method with two parameters must have MultiMap as the first parameter: %s [method: %s, bean:%s]",
                                    params, method, bean));
                        } else if (isMessage(params.get(1).name())) {
                            throw new IllegalStateException(String.format(
                                    "An event consumer business method with two parameters must not accept io.vertx.core.eventbus.Message or io.vertx.mutiny.core.eventbus.Message: %s [method: %s, bean:%s]",
                                    params, method, bean));
                        }
                    } else if (params.size() != 1) {
                        throw new IllegalStateException(String.format(
                                "An event consumer business method must accept exactly one parameter: %s [method: %s, bean:%s]",
                                params, method, bean));
                    }
                    if (method.returnType().kind() != Kind.VOID && VertxConstants.isMessage(params.get(0).name())) {
                        throw new IllegalStateException(String.format(
                                "An event consumer business method that accepts io.vertx.core.eventbus.Message or io.vertx.mutiny.core.eventbus.Message must return void [method: %s, bean:%s]",
                                method, bean));
                    }
                    if (method.hasAnnotation(RunOnVirtualThread.class) && consumeEvent.value("ordered") != null
                            && consumeEvent.value("ordered").asBoolean()) {
                        throw new IllegalStateException(String.format(
                                "An event consumer business method that cannot use @RunOnVirtualThread and set the ordered attribute to true [method: %s, bean:%s]",
                                method, bean));
                    }
                    messageConsumerBusinessMethods
                            .produce(new EventConsumerBusinessMethodItem(bean, method, consumeEvent));
                    LOGGER.debugf("Found event consumer business method %s declared on %s", method, bean);
                }
            }
        }
    }

    @BuildStep
    AutoAddScopeBuildItem autoAddScope() {
        // Add @Singleton to a class with no scope annotation but with a method annotated with @ConsumeEvent
        return AutoAddScopeBuildItem.builder().containsAnnotations(CONSUME_EVENT).defaultScope(BuiltinScope.SINGLETON)
                .reason("Found event consumer business methods").build();
    }

    @BuildStep
    void registerVerticleClasses(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // Mutiny Verticles
        for (ClassInfo ci : indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(io.smallrye.mutiny.vertx.core.AbstractVerticle.class.getName()))) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(ci.toString()).build());
        }
    }

    @BuildStep
    void faultToleranceIntegration(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        if (capabilities.isPresent(Capability.SMALLRYE_FAULT_TOLERANCE)) {
            serviceProvider.produce(new ServiceProviderBuildItem(
                    "io.smallrye.faulttolerance.core.event.loop.EventLoop",
                    "io.smallrye.faulttolerance.vertx.VertxEventLoop"));
        }
    }

    private Class<?> tryLoad(String name, ClassLoader tccl) {
        try {
            return tccl.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load type: " + name, e);
        }
    }
}
