package io.quarkus.smallrye.reactivemessaging.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.BLOCKING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.SMALLRYE_BLOCKING;
import static io.quarkus.smallrye.reactivemessaging.deployment.ReactiveMessagingDotNames.TRANSACTIONAL;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.CustomScopeAnnotationsBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
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
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.InjectedChannelBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.InjectedEmitterBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.items.MediatorBuildItem;
import io.quarkus.smallrye.reactivemessaging.runtime.DuplicatedContextConnectorFactory;
import io.quarkus.smallrye.reactivemessaging.runtime.DuplicatedContextConnectorFactoryInterceptor;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusMediatorConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.QuarkusWorkerPoolRegistry;
import io.quarkus.smallrye.reactivemessaging.runtime.ReactiveMessagingConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingLifecycle;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder;
import io.quarkus.smallrye.reactivemessaging.runtime.SmallRyeReactiveMessagingRecorder.SmallRyeReactiveMessagingContext;
import io.quarkus.smallrye.reactivemessaging.runtime.WorkerConfiguration;
import io.quarkus.smallrye.reactivemessaging.runtime.devmode.DevModeSupportConnectorFactory;
import io.quarkus.smallrye.reactivemessaging.runtime.devmode.DevModeSupportConnectorFactoryInterceptor;
import io.smallrye.reactive.messaging.EmitterConfiguration;
import io.smallrye.reactive.messaging.Invoker;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.health.SmallRyeReactiveMessagingLivenessCheck;
import io.smallrye.reactive.messaging.health.SmallRyeReactiveMessagingReadinessCheck;
import io.smallrye.reactive.messaging.health.SmallRyeReactiveMessagingStartupCheck;
import io.smallrye.reactive.messaging.providers.extension.ChannelConfiguration;

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
                    Map<DotName, List<AnnotationInstance>> annotations = clazz.annotationsMap();
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
    public AnnotationsTransformerBuildItem enableMetrics(Optional<MetricsCapabilityBuildItem> metricsCapability,
            ReactiveMessagingConfiguration configuration) {
        boolean isMetricEnabled = metricsCapability.isPresent() && configuration.metricsEnabled;
        boolean useMicrometer = isMetricEnabled && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER);
        LOGGER.debug("Metrics Enabled: " + isMetricEnabled + "; Using Micrometer: " + useMicrometer);
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(AnnotationsTransformer.TransformationContext ctx) {
                // Remove the MicrometerDecorator that requires the Micrometer API
                if (ctx.getTarget().asClass().name().equals(ReactiveMessagingDotNames.MICROMETER_DECORATOR)) {
                    if (!isMetricEnabled || !useMicrometer) {
                        ctx.transform()
                                .removeAll()
                                .add(Vetoed.class).done();
                    }
                }
                // Remove the MetricDecorator that requires the MP Metrics API
                if (ctx.getTarget().asClass().name().equals(ReactiveMessagingDotNames.METRIC_DECORATOR)) {
                    if (!isMetricEnabled || useMicrometer) {
                        ctx.transform()
                                .removeAll()
                                .add(Vetoed.class).done();
                    }
                }
            }
        });
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
            List<InjectedEmitterBuildItem> emitterFields,
            List<InjectedChannelBuildItem> channelFields,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            ReactiveMessagingConfiguration conf) {

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);

        List<QuarkusMediatorConfiguration> mediatorConfigurations = new ArrayList<>(mediatorMethods.size());
        List<WorkerConfiguration> workerConfigurations = new ArrayList<>();
        List<EmitterConfiguration> emittersConfigurations = new ArrayList<>();
        List<ChannelConfiguration> channelConfigurations = new ArrayList<>();

        /*
         * Go through the collected MediatorMethods and build up the corresponding MediaConfiguration
         * This includes generating an invoker for each method
         * The configuration will then be captured and used at static init time to push data into smallrye
         */
        for (MediatorBuildItem mediatorMethod : mediatorMethods) {
            MethodInfo methodInfo = mediatorMethod.getMethod();
            BeanInfo bean = mediatorMethod.getBean();

            if (methodInfo.hasAnnotation(BLOCKING) || methodInfo.hasAnnotation(SMALLRYE_BLOCKING)
                    || methodInfo.hasAnnotation(TRANSACTIONAL)) {
                // Just in case both annotation are used, use @Blocking value.
                String poolName = Blocking.DEFAULT_WORKER_POOL;

                // If the method is annotated with the SmallRye Reactive Messaging @Blocking, extract the worker pool name if any
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
                 * We could potentially lift this restriction with some extra CDI bean generation, but it's probably not worth
                 * it
                 */
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, generatedInvokerName));
                mediatorConfiguration
                        .setInvokerClass((Class<? extends Invoker>) recorderContext.classProxy(generatedInvokerName));
            } catch (IllegalArgumentException e) {
                throw new DeploymentException(e); // needed to pass the TCK
            }
        }

        for (InjectedEmitterBuildItem it : emitterFields) {
            emittersConfigurations.add(it.getEmitterConfig());
        }
        for (InjectedChannelBuildItem it : channelFields) {
            channelConfigurations.add(it.getChannelConfig());
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(SmallRyeReactiveMessagingContext.class)
                .supplier(recorder.createContext(mediatorConfigurations, workerConfigurations, emittersConfigurations,
                        channelConfigurations))
                .done());
    }

    private boolean isSuspendMethod(MethodInfo methodInfo) {
        if (!methodInfo.parameterTypes().isEmpty()) {
            return methodInfo.parameterType(methodInfo.parametersCount() - 1).name()
                    .equals(ReactiveMessagingDotNames.CONTINUATION);
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
        for (Type i : method.parameterTypes()) {
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
            // the method parameter needs to be of type Object because that is what is used as the call site in SmallRye
            // Reactive Messaging
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

                int parametersCount = method.parametersCount();
                String[] argTypes = new String[parametersCount];
                ResultHandle[] args = new ResultHandle[parametersCount];
                for (int i = 0; i < parametersCount; i++) {
                    // the only method argument of io.smallrye.reactive.messaging.Invoker is an object array, so we need to pull out
                    // each argument and put it in the target method arguments array
                    args[i] = invoke.readArrayValue(invoke.getMethodParam(0), i);
                    argTypes[i] = method.parameterType(i).name().toString();
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
            // the method parameter type needs to be Object, because that is what is used as the call site in SmallRye
            // Reactive Messaging
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
                ResultHandle[] args = new ResultHandle[method.parametersCount()];
                ResultHandle array = invoke.getMethodParam(1);
                for (int i = 0; i < method.parametersCount() - 1; ++i) {
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
        if (QuarkusClassLoader.isClassPresentAtRuntime("kotlinx.coroutines.future.FutureKt")) {
            return new CoroutineConfigurationBuildItem(true);
        } else {
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

    @BuildStep
    void duplicatedContextSupport(CombinedIndexBuildItem index, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<AnnotationsTransformerBuildItem> transformations) {
        beans.produce(new AdditionalBeanBuildItem(DuplicatedContextConnectorFactory.class,
                DuplicatedContextConnectorFactoryInterceptor.class));

        transformations.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext ctx) {
                ClassInfo clazz = ctx.getTarget().asClass();
                if (doesImplement(clazz, ReactiveMessagingDotNames.INCOMING_CONNECTOR_FACTORY, index.getIndex())) {
                    ctx.transform().add(DuplicatedContextConnectorFactory.class).done();
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

}
