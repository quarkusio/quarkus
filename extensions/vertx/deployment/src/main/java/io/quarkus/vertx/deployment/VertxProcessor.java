package io.quarkus.vertx.deployment;

import static io.quarkus.vertx.deployment.VertxConstants.CONSUME_EVENT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.Capabilities;
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
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.runtime.VertxProducer;
import io.quarkus.vertx.runtime.VertxRecorder;
import io.vertx.reactivex.core.AbstractVerticle;

class VertxProcessor {

    private static final Logger LOGGER = Logger.getLogger(VertxProcessor.class.getName());

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    void featureAndCapability(BuildProducer<FeatureBuildItem> feature, BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.VERTX));
        capability.produce(new CapabilityBuildItem(Capabilities.RESTEASY_JSON_EXTENSION));
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(VertxProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    VertxBuildItem build(CoreVertxBuildItem internalVertx, VertxRecorder recorder, BeanContainerBuildItem beanContainer,
            List<EventConsumerBusinessMethodItem> messageConsumerBusinessMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            AnnotationProxyBuildItem annotationProxy, LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown,
            BuildProducer<ServiceStartBuildItem> serviceStart,
            List<MessageCodecBuildItem> codecs, RecorderContext recorderContext) {
        Map<String, ConsumeEvent> messageConsumerConfigurations = new HashMap<>();
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        for (EventConsumerBusinessMethodItem businessMethod : messageConsumerBusinessMethods) {
            String invokerClass = EventBusConsumer.generateInvoker(businessMethod.getBean(), businessMethod.getMethod(),
                    businessMethod.getConsumeEvent(), classOutput);
            messageConsumerConfigurations.put(invokerClass,
                    annotationProxy.builder(businessMethod.getConsumeEvent(), ConsumeEvent.class)
                            .withDefaultValue("value", businessMethod.getBean().getBeanClass().toString())
                            .build(classOutput));
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, invokerClass));
        }

        Map<Class<?>, Class<?>> codecByClass = new HashMap<>();
        for (MessageCodecBuildItem messageCodecItem : codecs) {
            codecByClass.put(recorderContext.classProxy(messageCodecItem.getType()),
                    recorderContext.classProxy(messageCodecItem.getCodec()));
        }

        recorder.configureVertx(internalVertx.getVertx(), messageConsumerConfigurations,
                launchMode.getLaunchMode(),
                shutdown, codecByClass);
        serviceStart.produce(new ServiceStartBuildItem("vertx"));
        return new VertxBuildItem(recorder.forceStart(internalVertx.getVertx()));
    }

    @BuildStep
    public UnremovableBeanBuildItem unremovableBeans() {
        return new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(CONSUME_EVENT));
    }

    @BuildStep
    void validateBeanDeployment(
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<EventConsumerBusinessMethodItem> messageConsumerBusinessMethods,
            BuildProducer<ValidationErrorBuildItem> errors) {

        // We need to collect all business methods annotated with @MessageConsumer first
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        for (BeanInfo bean : validationPhase.getContext().beans().classBeans()) {
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

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (!BuiltinScope.isIn(context.getAnnotations())
                        && context.getTarget().asClass().annotations().containsKey(CONSUME_EVENT)) {
                    // Class with no built-in scope annotation but with a method annotated with @ConsumeMessage
                    LOGGER.debugf(
                            "Found event consumer business methods on a class %s with no scope annotation - adding @Singleton",
                            context.getTarget());
                    context.transform().add(Singleton.class).done();
                }
            }
        });
    }

    @BuildStep
    void registerRxVerticleClasses(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (ClassInfo ci : indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(AbstractVerticle.class.getName()))) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ci.toString()));
        }
    }

}
