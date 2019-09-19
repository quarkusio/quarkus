package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
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
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.OnOverflow;
import io.smallrye.reactive.messaging.annotations.Stream;

/**
 * @author Martin Kouba
 */
public class SmallRyeReactiveMessagingProcessor {

    private static final Logger LOGGER = Logger
            .getLogger("io.quarkus.smallrye-reactive-messaging.deployment.processor");

    static final DotName NAME_INCOMING = DotName.createSimple(Incoming.class.getName());
    static final DotName NAME_OUTGOING = DotName.createSimple(Outgoing.class.getName());
    static final DotName NAME_STREAM = DotName.createSimple(Stream.class.getName());
    static final DotName NAME_EMITTER = DotName.createSimple(Emitter.class.getName());
    static final DotName ON_OVERFLOW = DotName.createSimple(OnOverflow.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_MESSAGING);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        // We add the connector and stream qualifiers to make them part of the index.
        return new AdditionalBeanBuildItem(SmallRyeReactiveMessagingLifecycle.class, Connector.class, Stream.class);
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformBeanScope(BeanArchiveIndexBuildItem index) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(AnnotationsTransformer.TransformationContext ctx) {
                if (ctx.isClass()) {
                    if (BuiltinScope.isDeclaredOn(ctx.getTarget().asClass())) {
                        return;
                    }
                    ClassInfo clazz = ctx.getTarget().asClass();
                    Map<DotName, List<AnnotationInstance>> annotations = clazz.annotations();
                    if (annotations.containsKey(NAME_INCOMING)
                            || annotations.containsKey(NAME_OUTGOING)) {
                        LOGGER.debugf(
                                "Found reactive messaging annotations on a class %s with no scope defined - adding @Dependent",
                                ctx.getTarget());
                        ctx.transform().add(Dependent.class).done();
                    }
                }
            }
        });
    }

    @BuildStep
    void validateBeanDeployment(
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<MediatorBuildItem> mediatorMethods,
            BuildProducer<EmitterBuildItem> emitters,
            BuildProducer<ValidationErrorBuildItem> errors) {

        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);

        // We need to collect all business methods annotated with @Incoming/@Outgoing first
        for (BeanInfo bean : validationPhase.getContext().get(BuildExtension.Key.BEANS)) {
            if (bean.isClassBean()) {
                // TODO: add support for inherited business methods
                for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                    AnnotationInstance incoming = annotationStore.getAnnotation(method, NAME_INCOMING);
                    AnnotationInstance outgoing = annotationStore.getAnnotation(method, NAME_OUTGOING);
                    if (incoming != null || outgoing != null) {
                        if (incoming != null && incoming.value().asString().isEmpty()) {
                            validationPhase.getContext().addDeploymentProblem(
                                    new DeploymentException("Empty @Incoming annotation on method " + method));
                        }
                        if (outgoing != null && outgoing.value().asString().isEmpty()) {
                            validationPhase.getContext().addDeploymentProblem(
                                    new DeploymentException("Empty @Outgoing annotation on method " + method));
                        }
                        // TODO: validate method params and return type?
                        mediatorMethods.produce(new MediatorBuildItem(bean, method));
                        LOGGER.debugf("Found mediator business method %s declared on %s", method, bean);
                    }
                }
            }
        }

        for (InjectionPointInfo injectionPoint : validationPhase.getContext()
                .get(BuildExtension.Key.INJECTION_POINTS)) {
            if (injectionPoint.getRequiredType().name().equals(NAME_EMITTER)) {
                AnnotationInstance stream = injectionPoint.getRequiredQualifier(NAME_STREAM);
                if (stream != null) {
                    // Stream.value() is mandatory
                    String name = stream.value().asString();
                    Optional<AnnotationInstance> maybeOverflow = annotationStore.getAnnotations(injectionPoint.getTarget())
                            .stream()
                            .filter(ai -> ON_OVERFLOW.equals(ai.name()))
                            .filter(ai -> {
                                if (ai.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER && injectionPoint.isParam()) {
                                    return ai.target().asMethodParameter().position() == injectionPoint.getPosition();
                                }
                                return true;
                            })
                            .findAny();
                    LOGGER.debugf("Emitter injection point '%s' detected, stream name: '%s'",
                            injectionPoint.getTargetInfo(), name);

                    if (maybeOverflow.isPresent()) {
                        AnnotationInstance annotation = maybeOverflow.get();
                        AnnotationValue maybeBufferSize = annotation.value("bufferSize");
                        int bufferSize = maybeBufferSize != null ? maybeBufferSize.asInt() : 0;
                        emitters.produce(
                                EmitterBuildItem.of(name,
                                        annotation.value().asString(),
                                        bufferSize));
                    } else {
                        emitters.produce(EmitterBuildItem.of(name));
                    }
                }
            }
        }
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

        for (EmitterBuildItem it : emitterFields) {
            int defaultBufferSize = ConfigProviderResolver.instance().getConfig()
                    .getOptionalValue("smallrye.messaging.emitter.default-buffer-size", Integer.class).orElse(127);
            if (it.getOverflow() != null) {
                recorder.configureEmitter(beanContainer.getValue(), it.getName(), it.getOverflow(), it.getBufferSize(),
                        defaultBufferSize);
            } else {
                recorder.configureEmitter(beanContainer.getValue(), it.getName(), null, 0, defaultBufferSize);
            }
        }

        recorder.registerMediators(beanClassToBeanId, beanContainer.getValue());
    }

}
