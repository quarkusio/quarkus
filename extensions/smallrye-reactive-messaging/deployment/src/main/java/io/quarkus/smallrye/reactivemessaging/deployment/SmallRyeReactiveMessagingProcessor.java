package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDeploymentValidatorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.Stream;

/**
 *
 * @author Martin Kouba
 */
public class SmallRyeReactiveMessagingProcessor {

    private static final Logger LOGGER = Logger.getLogger("io.quarkus.smallrye-reactive-messaging.deployment.processor");

    static final DotName NAME_INCOMING = DotName.createSimple(Incoming.class.getName());
    static final DotName NAME_OUTGOING = DotName.createSimple(Outgoing.class.getName());
    static final DotName NAME_STREAM = DotName.createSimple(Stream.class.getName());
    static final DotName NAME_EMITTER = DotName.createSimple(Emitter.class.getName());

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(SmallRyeReactiveMessagingLifecycle.class);
    }

    @BuildStep
    BeanDeploymentValidatorBuildItem beanDeploymentValidator(BuildProducer<MediatorBuildItem> mediatorMethods,
            BuildProducer<EmitterBuildItem> emitters,
            BuildProducer<FeatureBuildItem> feature) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_MESSAGING));

        return new BeanDeploymentValidatorBuildItem(new BeanDeploymentValidator() {

            @Override
            public void validate(ValidationContext validationContext) {

                AnnotationStore annotationStore = validationContext.get(Key.ANNOTATION_STORE);

                // We need to collect all business methods annotated with @Incoming/@Outgoing first
                for (BeanInfo bean : validationContext.get(Key.BEANS)) {
                    if (bean.isClassBean()) {
                        // TODO: add support for inherited business methods
                        for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                            if (annotationStore.hasAnnotation(method, NAME_INCOMING)
                                    || annotationStore.hasAnnotation(method, NAME_OUTGOING)) {
                                // TODO: validate method params and return type?
                                mediatorMethods.produce(new MediatorBuildItem(bean, method));
                                LOGGER.debugf("Found mediator business method %s declared on %s", method, bean);
                            }
                        }
                    }
                }

                for (InjectionPointInfo injectionPoint : validationContext.get(Key.INJECTION_POINTS)) {
                    if (injectionPoint.getRequiredType().name().equals(NAME_EMITTER)) {
                        AnnotationInstance stream = injectionPoint.getRequiredQualifier(NAME_STREAM);
                        if (stream != null) {
                            // Stream.value() is mandatory
                            String name = stream.value().asString();
                            LOGGER.debugf("Emitter injection point '%s' detected, stream name: '%s'",
                                    injectionPoint.getTargetInfo(), name);
                            emitters.produce(new EmitterBuildItem(name));
                        }
                    }
                }
            }
        });
    }

    @BuildStep
    public List<UnremovableBeanBuildItem> removalExclusions() {
        return Arrays.asList(new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(NAME_INCOMING)),
                new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(NAME_OUTGOING)));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(SmallRyeReactiveMessagingRecorder recorder, BeanContainerBuildItem beanContainer,
            List<MediatorBuildItem> mediatorMethods,
            List<EmitterBuildItem> emitterFields,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        /*
         * IMPLEMENTATION NOTE/FUTURE IMPROVEMENTS: It would be possible to replace the reflection completely and use Jandex and
         * generated
         * io.smallrye.reactive.messaging.Invoker instead. However, we would have to mirror the logic from
         * io.smallrye.reactive.messaging.MediatorConfiguration
         * too.
         */
        Map<String, String> beanClassToBeanId = new HashMap<>();
        for (MediatorBuildItem mediatorMethod : mediatorMethods) {
            String beanClass = mediatorMethod.getBean()
                    .getBeanClass()
                    .toString();
            if (!beanClassToBeanId.containsKey(beanClass)) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, beanClass));
                beanClassToBeanId.put(beanClass, mediatorMethod.getBean()
                        .getIdentifier());
            }
        }
        recorder.registerMediators(beanClassToBeanId, beanContainer.getValue(),
                emitterFields.stream().map(EmitterBuildItem::getName).collect(Collectors.toList()));
    }

}
