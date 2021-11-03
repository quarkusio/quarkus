package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.BLOCKING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.SMALLRYE_BLOCKING;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.TransformedAnnotationsBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusMediatorConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusWorkerPoolRegistry;
import io.quarkus.smallrye.reactivemessaging.runtime.ReactiveMessagingConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder.SmallRyeReactiveMessagingContext;
import io.quarkus.smallrye.reactivemessaging.runtime.WorkerConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.devmode.DevModeSupportConnectorFactory;
import io.quarkus.smallrye.reactivemessaging.runtime.devmode.DevModeSupportConnectorFactoryInterceptor;
import io.smallrye.reactive.messaging.Invoker;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.extension.ChannelConfiguration;
import io.smallrye.reactive.messaging.extension.EmitterConfiguration;
import io.smallrye.reactive.messaging.health.SmallRyeReactiveMessagingLivenessCheck;
import io.smallrye.reactive.messaging.health.SmallRyeReactiveMessagingReadinessCheck;
import io.smallrye.reactive.messaging.health.SmallRyeReactiveMessagingStartupCheck;

public class SmallRyeReactiveMessagingProcessor {

    private static final Logger LOGGER = Logger
            .getLogger("io.quarkus.smallrye-reactive-messaging.deployment.processor");

    static final String INVOKER_SUFFIX = "_SmallRyeMessagingInvoker";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_REACTIVE_MESSAGING);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        // We add the connector and channel qualifiers to make them part of the index.
        return new AdditionalBeanBuildItem(SmallRyeReactiveMessagingLifecycle.class, Connector.class,
                Channel.class, io.smallrye.reactive.messaging.annotations.Channel.class,
                QuarkusWorkerPoolRegistry.class);
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
                            || annotations.containsKey(ReactiveMessagingDotNames.OUTGOING)
                            || annotations.containsKey(ReactiveMessagingDotNames.CHANNEL)) {
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
    void collectComponents(BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            TransformedAnnotationsBuildItem transformedAnnotations,
            BuildProducer<MediatorBuildItem> mediatorMethods,
            BuildProducer<EmitterBuildItem> emitters,
            BuildProducer<ChannelBuildItem> channels,
            BuildProducer<ValidationErrorBuildItem> validationErrors,
            BuildProducer<ConfigDescriptionBuildItem> configDescriptionBuildItemBuildProducer) {

        // We need to collect all business methods annotated with @Incoming/@Outgoing first
        for (BeanInfo bean : beanDiscoveryFinished.beanStream().classBeans()) {
            // TODO: add support for inherited business methods
            for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                // @Incoming is repeatable
                AnnotationInstance incoming = transformedAnnotations.getAnnotation(method,
                        ReactiveMessagingDotNames.INCOMING);
                AnnotationInstance incomings = transformedAnnotations.getAnnotation(method,
                        ReactiveMessagingDotNames.INCOMINGS);
                AnnotationInstance outgoing = transformedAnnotations.getAnnotation(method,
                        ReactiveMessagingDotNames.OUTGOING);
                AnnotationInstance blocking = transformedAnnotations.getAnnotation(method,
                        BLOCKING);
                if (incoming != null || incomings != null || outgoing != null) {
                    if (incoming != null && incoming.value().asString().isEmpty()) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new DeploymentException("Empty @Incoming annotation on method " + method)));
                    }
                    if (incoming != null) {
                        configDescriptionBuildItemBuildProducer.produce(new ConfigDescriptionBuildItem(
                                "mp.messaging.incoming." + incoming.value().asString() + ".connector", String.class, null,
                                "The connector to use", null, null, ConfigPhase.BUILD_TIME));
                    }
                    if (outgoing != null && outgoing.value().asString().isEmpty()) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new DeploymentException("Empty @Outgoing annotation on method " + method)));
                    }
                    if (outgoing != null) {
                        configDescriptionBuildItemBuildProducer.produce(new ConfigDescriptionBuildItem(
                                "mp.messaging.outgoing." + outgoing.value().asString() + ".connector", String.class, null,
                                "The connector to use", null, null, ConfigPhase.BUILD_TIME));
                    }
                    if (isSynthetic(method.flags())) {
                        continue;
                    }
                    // TODO: validate method params and return type?
                    mediatorMethods.produce(new MediatorBuildItem(bean, method));
                    LOGGER.debugf("Found mediator business method %s declared on %s", method, bean);
                } else if (blocking != null) {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new DeploymentException(
                                    "@Blocking used on " + method + " which has no @Incoming or @Outgoing annotation")));
                }
            }
        }

        for (InjectionPointInfo injectionPoint : beanDiscoveryFinished.getInjectionPoints()) {
            Optional<AnnotationInstance> broadcast = getAnnotation(transformedAnnotations, injectionPoint,
                    ReactiveMessagingDotNames.BROADCAST);
            Optional<AnnotationInstance> channel = getAnnotation(transformedAnnotations, injectionPoint,
                    ReactiveMessagingDotNames.CHANNEL);
            Optional<AnnotationInstance> legacyChannel = getAnnotation(transformedAnnotations, injectionPoint,
                    ReactiveMessagingDotNames.LEGACY_CHANNEL);
            boolean isEmitter = injectionPoint.getRequiredType().name().equals(ReactiveMessagingDotNames.EMITTER);
            boolean isMutinyEmitter = injectionPoint.getRequiredType().name()
                    .equals(ReactiveMessagingDotNames.MUTINY_EMITTER);
            boolean isLegacyEmitter = injectionPoint.getRequiredType().name()
                    .equals(ReactiveMessagingDotNames.LEGACY_EMITTER);

            // New emitter from the spec, or Mutiny emitter
            if (isEmitter || isMutinyEmitter) {
                if (!channel.isPresent()) {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new DeploymentException(
                                    "Invalid emitter injection - @Channel is required for " + injectionPoint
                                            .getTargetInfo())));
                } else {
                    String channelName = channel.get().value().asString();
                    Optional<AnnotationInstance> overflow = getAnnotation(transformedAnnotations, injectionPoint,
                            ReactiveMessagingDotNames.ON_OVERFLOW);
                    createEmitter(emitters,
                            injectionPoint, channelName, overflow, broadcast);
                }
            }

            // Deprecated Emitter from SmallRye (emitter, channel and on overflow have been added to the spec)
            if (isLegacyEmitter) {
                if (!legacyChannel.isPresent()) {
                    validationErrors.produce(new ValidationErrorBuildItem(
                            new DeploymentException(
                                    "Invalid emitter injection - @Channel is required for " + injectionPoint
                                            .getTargetInfo())));
                } else {
                    String channelName = legacyChannel.get().value().asString();
                    Optional<AnnotationInstance> overflow = getAnnotation(transformedAnnotations, injectionPoint,
                            ReactiveMessagingDotNames.LEGACY_ON_OVERFLOW);
                    createEmitter(emitters, injectionPoint, channelName, overflow, broadcast);
                }
            }

            if (channel.isPresent() && !(isEmitter || isMutinyEmitter)) {
                String name = channel.get().value().asString();
                if (name != null && !name.trim().isEmpty()) {
                    channels.produce(ChannelBuildItem.of(name));
                }
            }

            if (legacyChannel.isPresent() && !isLegacyEmitter) {
                String name = legacyChannel.get().value().asString();
                if (name != null && !name.trim().isEmpty()) {
                    channels.produce(ChannelBuildItem.of(name));
                }
            }
        }

    }

    private boolean isSynthetic(int mod) {
        return (mod & Opcodes.ACC_SYNTHETIC) != 0;
    }

    private Optional<AnnotationInstance> getAnnotation(TransformedAnnotationsBuildItem transformedAnnotations,
            InjectionPointInfo injectionPoint,
            DotName annotationName) {
        Collection<AnnotationInstance> annotations = transformedAnnotations.getAnnotations(injectionPoint.getTarget());
        for (AnnotationInstance annotation : annotations) {
            if (annotationName.equals(annotation.name())) {
                // For method parameter we must check the position
                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER
                        && injectionPoint.isParam()
                        && annotation.target().asMethodParameter().position() == injectionPoint.getPosition()) {
                    return Optional.of(annotation);
                } else if (annotation.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER) {
                    // For other kind, no need to check anything else
                    return Optional.of(annotation);
                }
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void createEmitter(BuildProducer<EmitterBuildItem> emitters,
            InjectionPointInfo injectionPoint,
            String channelName,
            Optional<AnnotationInstance> overflow,
            Optional<AnnotationInstance> broadcast) {
        LOGGER.debugf("Emitter injection point '%s' detected, channel name: '%s'",
                injectionPoint.getTargetInfo(), channelName);

        boolean hasBroadcast = false;
        int awaitSubscribers = -1;
        int bufferSize = -1;
        String strategy = null;
        if (broadcast.isPresent()) {
            hasBroadcast = true;
            AnnotationValue value = broadcast.get().value();
            awaitSubscribers = value == null ? 0 : value.asInt();
        }

        if (overflow.isPresent()) {
            AnnotationInstance annotation = overflow.get();
            AnnotationValue maybeBufferSize = annotation.value("bufferSize");
            bufferSize = maybeBufferSize == null ? 0 : maybeBufferSize.asInt();
            strategy = annotation.value().asString();
        }

        boolean isMutinyEmitter = injectionPoint.getRequiredType().name()
                .equals(ReactiveMessagingDotNames.MUTINY_EMITTER);
        emitters.produce(
                EmitterBuildItem
                        .of(channelName, isMutinyEmitter, strategy, bufferSize, hasBroadcast, awaitSubscribers));
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
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            ReactiveMessagingConfiguration configuration) {
        boolean isMetricEnabled = metricsCapability.isPresent() && configuration.metricsEnabled;
        boolean useMicrometer = isMetricEnabled && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER);
        if (!isMetricEnabled || useMicrometer) {
            LOGGER.debug("Metrics Enabled: " + isMetricEnabled + "; Using Micrometer: " + useMicrometer);

            // Remove the MetricDecorator that requires the MP Metrics API
            AnnotationsTransformerBuildItem veto = new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
                @Override
                public boolean appliesTo(AnnotationTarget.Kind kind) {
                    return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
                }

                @Override
                public void transform(AnnotationsTransformer.TransformationContext ctx) {
                    if (ctx.getTarget().asClass().name().equals(ReactiveMessagingDotNames.METRIC_DECORATOR)) {
                        ctx.transform()
                                .removeAll()
                                .add(Vetoed.class).done();
                    }
                }
            });
            transformers.produce(veto);
        }
    }

    @BuildStep
    public void enableHealth(ReactiveMessagingBuildTimeConfig buildTimeConfig,
            BuildProducer<HealthBuildItem> producer) {
        producer.produce(
                new HealthBuildItem(SmallRyeReactiveMessagingLivenessCheck.class.getName(),
                        buildTimeConfig.healthEnabled));
        producer.produce(
                new HealthBuildItem(SmallRyeReactiveMessagingReadinessCheck.class.getName(),
                        buildTimeConfig.healthEnabled));
        producer.produce(
                new HealthBuildItem(SmallRyeReactiveMessagingStartupCheck.class.getName(),
                        buildTimeConfig.healthEnabled));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(SmallRyeReactiveMessagingRecorder recorder, RecorderContext recorderContext,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            List<MediatorBuildItem> mediatorMethods,
            List<EmitterBuildItem> emitterFields,
            List<ChannelBuildItem> channelFields,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            ReactiveMessagingConfiguration conf) {

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);

        List<QuarkusMediatorConfiguration> mediatorConfigurations = new ArrayList<>(mediatorMethods.size());
        List<WorkerConfiguration> workerConfigurations = new ArrayList<>();
        List<EmitterConfiguration> emittersConfiguratons = new ArrayList<>();
        List<ChannelConfiguration> channelConfigurations = new ArrayList<>();

        /*
         * Go through the collected MediatorMethods and build up the corresponding MediaConfiguration
         * This includes generating an invoker for each method
         * The configuration will then be captured and used at static init time to push data into smallrye
         */
        for (MediatorBuildItem mediatorMethod : mediatorMethods) {
            MethodInfo methodInfo = mediatorMethod.getMethod();
            BeanInfo bean = mediatorMethod.getBean();

            if (methodInfo.hasAnnotation(BLOCKING) || methodInfo.hasAnnotation(SMALLRYE_BLOCKING)) {
                // Just in case both annotation are used, use @Blocking value.
                String poolName = Blocking.DEFAULT_WORKER_POOL;
                if (methodInfo.hasAnnotation(ReactiveMessagingDotNames.BLOCKING)) {
                    AnnotationInstance blocking = methodInfo.annotation(ReactiveMessagingDotNames.BLOCKING);
                    poolName = blocking.value() == null ? Blocking.DEFAULT_WORKER_POOL : blocking.value().asString();
                }
                workerConfigurations.add(new WorkerConfiguration(methodInfo.declaringClass().toString(),
                        methodInfo.name(), poolName));
            }

            try {
                boolean isSuspendMethod = isSuspendMethod(methodInfo);

                QuarkusMediatorConfiguration mediatorConfiguration = QuarkusMediatorConfigurationUtil
                        .create(methodInfo, isSuspendMethod, bean, recorderContext,
                                Thread.currentThread().getContextClassLoader(), conf.strict);
                mediatorConfigurations.add(mediatorConfiguration);

                String generatedInvokerName = generateInvoker(bean, methodInfo, isSuspendMethod, mediatorConfiguration,
                        classOutput);
                /*
                 * We need to register the invoker's constructor for reflection since it will be called inside smallrye.
                 * We could potentially lift this restriction with some extra CDI bean generation but it's probably not worth it
                 */
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, generatedInvokerName));
                mediatorConfiguration
                        .setInvokerClass((Class<? extends Invoker>) recorderContext.classProxy(generatedInvokerName));
            } catch (IllegalArgumentException e) {
                throw new DeploymentException(e); // needed to pass the TCK
            }
        }

        for (EmitterBuildItem it : emitterFields) {
            emittersConfiguratons.add(it.getEmitterConfig());
        }
        for (ChannelBuildItem it : channelFields) {
            channelConfigurations.add(it.getChannelConfig());
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(SmallRyeReactiveMessagingContext.class)
                .supplier(recorder.createContext(mediatorConfigurations, workerConfigurations, emittersConfiguratons,
                        channelConfigurations))
                .done());
    }

    private boolean isSuspendMethod(MethodInfo methodInfo) {
        if (!methodInfo.parameters().isEmpty()) {
            if (methodInfo.parameters().get(methodInfo.parameters().size() - 1).name()
                    .equals(ReactiveMessagingDotNames.CONTINUATION)) {
                return true;
            }
        }
        return false;
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
    private String generateInvoker(BeanInfo bean, MethodInfo method, boolean isSuspendMethod,
            QuarkusMediatorConfiguration mediatorConfiguration,
            ClassOutput classOutput) {
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
        String targetPackage = DotNames.internalPackageNameWithTrailingSlash(bean.getImplClazz().name());
        String generatedName = targetPackage + baseName
                + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        if (isSuspendMethod
                && ((mediatorConfiguration.getIncoming().isEmpty()) && (mediatorConfiguration.getOutgoing() != null))) {
            // TODO: this restriction needs to be lifted
            throw new IllegalStateException(
                    "Currently suspend methods for Reactive Messaging are not supported on methods that are only annotated with @Outgoing");
        }

        if (!isSuspendMethod) {
            generateStandardInvoker(method, classOutput, generatedName);
        } else if (!mediatorConfiguration.getIncoming().isEmpty()) {
            generateSubscribingCoroutineInvoker(method, classOutput, generatedName);
        }

        return generatedName.replace('/', '.');
    }

    private void generateStandardInvoker(MethodInfo method, ClassOutput classOutput, String generatedName) {
        try (ClassCreator invoker = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(Invoker.class)
                .build()) {

            String beanInstanceType = method.declaringClass().name().toString();
            FieldDescriptor beanInstanceField = invoker.getFieldCreator("beanInstance", beanInstanceType)
                    .getFieldDescriptor();

            // generate a constructor that takes the bean instance as an argument
            // the method type needs to be Object because that is what is used as the call site in SmallRye Reactive Messaging
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
    }

    private void generateSubscribingCoroutineInvoker(MethodInfo method, ClassOutput classOutput, String generatedName) {
        try (ClassCreator invoker = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(ReactiveMessagingDotNames.ABSTRACT_SUBSCRIBING_COROUTINE_INVOKER.toString())
                .build()) {

            // generate a constructor that takes the bean instance as an argument
            // the method type needs to be Object because that is what is used as the call site in SmallRye Reactive Messaging
            try (MethodCreator ctor = invoker.getMethodCreator("<init>", void.class, Object.class)) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(
                        MethodDescriptor.ofConstructor(
                                ReactiveMessagingDotNames.ABSTRACT_SUBSCRIBING_COROUTINE_INVOKER.toString(),
                                Object.class.getName()),
                        ctor.getThis(),
                        ctor.getMethodParam(0));
                ctor.returnValue(null);
            }

            try (MethodCreator invoke = invoker.getMethodCreator("invokeBean", Object.class, Object.class, Object[].class,
                    ReactiveMessagingDotNames.CONTINUATION.toString())) {
                ResultHandle[] args = new ResultHandle[method.parameters().size()];
                ResultHandle array = invoke.getMethodParam(1);
                for (int i = 0; i < method.parameters().size() - 1; ++i) {
                    args[i] = invoke.readArrayValue(array, i);
                }
                args[args.length - 1] = invoke.getMethodParam(2);
                ResultHandle result = invoke.invokeVirtualMethod(method, invoke.getMethodParam(0), args);
                invoke.returnValue(result);
            }
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void devmodeSupport(CombinedIndexBuildItem index, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<AnnotationsTransformerBuildItem> transformations) {
        beans.produce(new AdditionalBeanBuildItem(DevModeSupportConnectorFactory.class,
                DevModeSupportConnectorFactoryInterceptor.class));

        transformations.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                ClassInfo clazz = ctx.getTarget().asClass();
                if (doesImplement(clazz, ReactiveMessagingDotNames.INCOMING_CONNECTOR_FACTORY, index.getIndex())
                        || doesImplement(clazz, ReactiveMessagingDotNames.OUTGOING_CONNECTOR_FACTORY, index.getIndex())) {
                    ctx.transform().add(DevModeSupportConnectorFactory.class).done();
                }
            }

            private boolean doesImplement(ClassInfo clazz, DotName iface, IndexView index) {
                while (clazz != null && !clazz.name().equals(ReactiveMessagingDotNames.OBJECT)) {
                    if (clazz.interfaceNames().contains(iface)) {
                        return true;
                    }

                    clazz = index.getClassByName(clazz.superName());
                }

                return false;
            }
        }));
    }

    @BuildStep
    CoroutineConfigurationBuildItem producesCoroutineConfiguration() {
        try {
            Class.forName("kotlinx.coroutines.future.FutureKt", false, getClass().getClassLoader());
            return new CoroutineConfigurationBuildItem(true);
        } catch (ClassNotFoundException e) {
            return new CoroutineConfigurationBuildItem(false);
        }
    }

    @BuildStep
    void produceCoroutineScope(
            CoroutineConfigurationBuildItem coroutineConfigurationBuildItem,
            BuildProducer<AdditionalBeanBuildItem> buildItemBuildProducer) {
        if (coroutineConfigurationBuildItem.isEnabled()) {
            buildItemBuildProducer.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(
                            "io.quarkus.smallrye.reactivemessaging.runtime.kotlin.ApplicationCoroutineScope")
                    .setUnremovable().build());
        }
    }

    private void ensureKotlinCoroutinesEnabled(CoroutineConfigurationBuildItem coroutineConfigurationBuildItem,
            MethodInfo method) {
        if (!coroutineConfigurationBuildItem.isEnabled()) {
            String format = String.format(
                    "Method %s.%s is suspendable but kotlinx-coroutines-jdk8 dependency not detected",
                    method.declaringClass().name(), method.name());
            throw new IllegalStateException(format);
        }
    }

    public static final class CoroutineConfigurationBuildItem extends SimpleBuildItem {
        private final boolean isEnabled;

        public CoroutineConfigurationBuildItem(boolean isEnabled) {
            this.isEnabled = isEnabled;
        }

        public boolean isEnabled() {
            return isEnabled;
        }
    }

}
