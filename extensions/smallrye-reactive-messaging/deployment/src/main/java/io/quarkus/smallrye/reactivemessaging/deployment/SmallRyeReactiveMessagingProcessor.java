package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusMediatorConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusWorkerPoolRegistry;
import io.quarkus.smallrye.reactivemessaging.runtime.ReactiveMessagingConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder;
import io.smallrye.reactive.messaging.Invoker;
import io.smallrye.reactive.messaging.annotations.Blocking;

/**
 * 
 */
public class SmallRyeReactiveMessagingProcessor {

    private static final Logger LOGGER = Logger
            .getLogger("io.quarkus.smallrye-reactive-messaging.deployment.processor");

    static final String INVOKER_SUFFIX = "_SmallryeMessagingInvoker";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SMALLRYE_REACTIVE_MESSAGING);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        // We add the connector and channel qualifiers to make them part of the index.
        return new AdditionalBeanBuildItem(SmallRyeReactiveMessagingLifecycle.class, Connector.class,
                Channel.class, io.smallrye.reactive.messaging.annotations.Channel.class, QuarkusWorkerPoolRegistry.class);
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformBeanScope(BeanArchiveIndexBuildItem index,
            CustomScopeAnnotationsBuildItem scopes) {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(AnnotationsTransformer.TransformationContext ctx) {
                if (ctx.isClass()) {
                    ClassInfo clazz = ctx.getTarget().asClass();
                    Map<DotName, List<AnnotationInstance>> annotations = clazz.annotations();
                    if (scopes.isScopeDeclaredOn(clazz)
                            || annotations.containsKey(ReactiveMessagingDotNames.JAXRS_PATH)
                            || annotations.containsKey(ReactiveMessagingDotNames.REST_CONTROLLER)
                            || annotations.containsKey(ReactiveMessagingDotNames.JAXRS_PROVIDER)) {
                        // Skip - has a built-in scope annotation or is a JAX-RS endpoint/provider
                        return;
                    }
                    if (annotations.containsKey(ReactiveMessagingDotNames.INCOMING)
                            || annotations.containsKey(ReactiveMessagingDotNames.OUTGOING)) {
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
        for (BeanInfo bean : validationPhase.getContext().beans().classBeans()) {
            // TODO: add support for inherited business methods
            for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                AnnotationInstance incoming = annotationStore.getAnnotation(method,
                        ReactiveMessagingDotNames.INCOMING);
                AnnotationInstance outgoing = annotationStore.getAnnotation(method,
                        ReactiveMessagingDotNames.OUTGOING);
                AnnotationInstance blocking = annotationStore.getAnnotation(method,
                        ReactiveMessagingDotNames.BLOCKING);
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
                } else if (blocking != null) {
                    validationPhase.getContext().addDeploymentProblem(
                            new DeploymentException(
                                    "@Blocking used on " + method + " which has no @Incoming or @Outgoing annotation"));
                }
            }
        }

        for (InjectionPointInfo injectionPoint : validationPhase.getContext()
                .get(BuildExtension.Key.INJECTION_POINTS)) {
            // New emitter from the spec.
            if (injectionPoint.getRequiredType().name().equals(
                    ReactiveMessagingDotNames.EMITTER)) {
                AnnotationInstance instance = injectionPoint
                        .getRequiredQualifier(ReactiveMessagingDotNames.CHANNEL);
                if (instance == null) {
                    validationPhase.getContext().addDeploymentProblem(
                            new DeploymentException(
                                    "Invalid emitter injection - @Channel is required for " + injectionPoint
                                            .getTargetInfo()));
                } else {
                    String channelName = instance.value().asString();
                    Optional<AnnotationInstance> overflow = annotationStore.getAnnotations(injectionPoint.getTarget())
                            .stream()
                            .filter(ai -> ReactiveMessagingDotNames.ON_OVERFLOW
                                    .equals(ai.name()))
                            .filter(ai -> {
                                if (ai.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER && injectionPoint
                                        .isParam()) {
                                    return ai.target().asMethodParameter().position() == injectionPoint.getPosition();
                                }
                                return true;
                            })
                            .findAny();
                    createEmitter(emitters, injectionPoint, channelName, overflow);
                }
            }

            // Deprecated Emitter from SmallRye (emitter, channel and on overflow have been added to the spec)
            if (injectionPoint.getRequiredType().name()
                    .equals(ReactiveMessagingDotNames.LEGACY_EMITTER)) {
                AnnotationInstance instance = injectionPoint
                        .getRequiredQualifier(ReactiveMessagingDotNames.LEGACY_CHANNEL);
                if (instance == null) {
                    validationPhase.getContext().addDeploymentProblem(
                            new DeploymentException(
                                    "Invalid emitter injection - @Channel is required for " + injectionPoint
                                            .getTargetInfo()));
                } else {
                    String channelName = instance.value().asString();
                    Optional<AnnotationInstance> overflow = annotationStore.getAnnotations(injectionPoint.getTarget())
                            .stream()
                            .filter(ai -> ReactiveMessagingDotNames.LEGACY_ON_OVERFLOW
                                    .equals(ai.name()))
                            .filter(ai -> {
                                if (ai.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER && injectionPoint
                                        .isParam()) {
                                    return ai.target().asMethodParameter().position() == injectionPoint.getPosition();
                                }
                                return true;
                            })
                            .findAny();
                    createEmitter(emitters, injectionPoint, channelName, overflow);
                }
            }
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void createEmitter(BuildProducer<EmitterBuildItem> emitters, InjectionPointInfo injectionPoint,
            String channelName,
            Optional<AnnotationInstance> overflow) {
        LOGGER.debugf("Emitter injection point '%s' detected, channel name: '%s'",
                injectionPoint.getTargetInfo(), channelName);
        if (overflow.isPresent()) {
            AnnotationInstance annotation = overflow.get();
            AnnotationValue maybeBufferSize = annotation.value("bufferSize");
            int bufferSize = maybeBufferSize != null ? maybeBufferSize.asInt() : 0;
            emitters.produce(
                    EmitterBuildItem.of(channelName, annotation.value().asString(), bufferSize));
        } else {
            emitters.produce(EmitterBuildItem.of(channelName));
        }
    }

    @BuildStep
    public List<UnremovableBeanBuildItem> removalExclusions() {
        return Arrays.asList(
                new UnremovableBeanBuildItem(
                        new BeanClassAnnotationExclusion(
                                ReactiveMessagingDotNames.INCOMING)),
                new UnremovableBeanBuildItem(
                        new BeanClassAnnotationExclusion(
                                ReactiveMessagingDotNames.OUTGOING)));
    }

    @BuildStep
    public void enableMetrics(BuildProducer<AnnotationsTransformerBuildItem> transformers,
            Capabilities capabilities, ReactiveMessagingConfiguration configuration) {
        boolean isMetricEnabled = capabilities.isCapabilityPresent(Capabilities.METRICS) && configuration.metricsEnabled;
        if (!isMetricEnabled) {
            LOGGER.debug("Metric is disabled - vetoing the MetricDecorator");
            // We veto the Metric Decorator
            AnnotationsTransformerBuildItem veto = new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
                @Override
                public boolean appliesTo(AnnotationTarget.Kind kind) {
                    return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
                }

                @Override
                public void transform(AnnotationsTransformer.TransformationContext ctx) {
                    if (ctx.isClass() && ctx.getTarget().asClass().name().equals(
                            ReactiveMessagingDotNames.METRIC_DECORATOR)) {
                        ctx.transform().add(Vetoed.class).done();
                    }
                }
            });
            transformers.produce(veto);
        }
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(SmallRyeReactiveMessagingRecorder recorder, RecorderContext recorderContext,
            BeanContainerBuildItem beanContainer,
            List<MediatorBuildItem> mediatorMethods,
            List<EmitterBuildItem> emitterFields,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            ReactiveMessagingConfiguration conf) {

        List<QuarkusMediatorConfiguration> configurations = new ArrayList<>(mediatorMethods.size());

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);

        /*
         * Go through the collected MediatorMethods and build up the corresponding MediaConfiguration
         * This includes generating an invoker for each method
         * The configuration will then be captured and used at static init time to push data into smallrye
         */
        for (MediatorBuildItem mediatorMethod : mediatorMethods) {
            MethodInfo methodInfo = mediatorMethod.getMethod();
            BeanInfo bean = mediatorMethod.getBean();

            String generatedInvokerName = generateInvoker(bean, methodInfo, classOutput);
            /*
             * We need to register the invoker's constructor for reflection since it will be called inside smallrye.
             * We could potentially lift this restriction with some extra CDI bean generation but it's probably not worth it
             */
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, generatedInvokerName));

            if (methodInfo.hasAnnotation(ReactiveMessagingDotNames.BLOCKING)) {
                AnnotationInstance blocking = methodInfo.annotation(ReactiveMessagingDotNames.BLOCKING);
                String poolName = blocking.value() == null ? Blocking.DEFAULT_WORKER_POOL : blocking.value().asString();

                recorder.configureWorkerPool(beanContainer.getValue(), methodInfo.declaringClass().toString(),
                        methodInfo.name(), poolName);
            }

            try {
                QuarkusMediatorConfiguration mediatorConfiguration = QuarkusMediatorConfigurationUtil
                        .create(methodInfo, bean,
                                generatedInvokerName, recorderContext,
                                Thread.currentThread().getContextClassLoader());
                configurations.add(mediatorConfiguration);
            } catch (IllegalArgumentException e) {
                throw new DeploymentException(e); // needed to pass the TCK
            }
        }
        recorder.registerMediators(configurations, beanContainer.getValue());

        for (EmitterBuildItem it : emitterFields) {
            Config config = ConfigProvider.getConfig();
            int defaultBufferSize = config.getOptionalValue("mp.messaging.emitter.default-buffer-size", Integer.class)
                    .orElseGet(() -> config
                            .getOptionalValue("smallrye.messaging.emitter.default-buffer-size", Integer.class)
                            .orElse(127));
            if (it.getOverflow() != null) {
                recorder.configureEmitter(beanContainer.getValue(), it.getName(), it.getOverflow(),
                        it.getBufferSize(),
                        defaultBufferSize);
            } else {
                recorder.configureEmitter(beanContainer.getValue(), it.getName(), null, 0, defaultBufferSize);
            }
        }
    }

    /**
     * Generates an invoker class that looks like the following:
     *
     * <pre>
     * public class SomeName implements Invoker {
     *     private BeanType beanInstance;
     *
     *     public SomeName(Object var1) {
     *         this.beanInstance = var1;
     *     }
     *
     *     public Object invoke(Object[] args) {
     *         return this.beanInstance.doSomething(var1);
     *     }
     * }
     * </pre>
     */
    private String generateInvoker(BeanInfo bean, MethodInfo method, ClassOutput classOutput) {
        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + DotNames.simpleName(bean.getImplClazz().name());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String targetPackage = DotNames.packageName(bean.getImplClazz().name());
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        try (ClassCreator invoker = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(Invoker.class)
                .build()) {

            String beanInstanceType = method.declaringClass().name().toString();
            FieldDescriptor beanInstanceField = invoker.getFieldCreator("beanInstance", beanInstanceType)
                    .getFieldDescriptor();

            // generate a constructor that takes the bean instance as an argument
            // the method type needs to be Object because that is what is used as the call site in Smallrye Reactive Messaging
            try (MethodCreator ctor = invoker.getMethodCreator("<init>", void.class, Object.class)) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                ResultHandle self = ctor.getThis();
                ResultHandle beanInstance = ctor.getMethodParam(0);
                ctor.writeInstanceField(beanInstanceField, self, beanInstance);
                ctor.returnValue(null);
            }

            try (MethodCreator invoke = invoker.getMethodCreator(
                    MethodDescriptor.ofMethod(generatedName, "invoke", Object.class, Object[].class))) {

                int parametersCount = method.parameters().size();
                String[] argTypes = new String[parametersCount];
                ResultHandle[] args = new ResultHandle[parametersCount];
                for (int i = 0; i < parametersCount; i++) {
                    // the only method argument of io.smallrye.reactive.messaging.Invoker is an object array so we need to pull out
                    // each argument and put it in the target method arguments array
                    args[i] = invoke.readArrayValue(invoke.getMethodParam(0), i);
                    argTypes[i] = method.parameters().get(i).name().toString();
                }
                ResultHandle result = invoke.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(beanInstanceType, method.name(),
                                method.returnType().name().toString(), argTypes),
                        invoke.readInstanceField(beanInstanceField, invoke.getThis()), args);
                if (ReactiveMessagingDotNames.VOID.equals(method.returnType().name())) {
                    invoke.returnValue(invoke.loadNull());
                } else {
                    invoke.returnValue(result);
                }
            }
        }

        return generatedName.replace('/', '.');
    }

}
