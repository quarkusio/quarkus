package io.quarkus.arc.deployment;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.CreationException;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanConfiguratorBase;
import io.quarkus.arc.runtime.ArcRecorder;
import io.quarkus.core.deployment.action.impl.LambdaTransliterator;
import io.quarkus.core.deployment.action.impl.ServiceValueRetentionBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.FieldVar;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.RuntimeValue;

public class SyntheticBeansProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void initStatic(ArcRecorder recorder, List<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistration, BuildProducer<BeanConfiguratorBuildItem> configurators,
            BuildProducer<StaticBytecodeRecorderBuildItem> staticRecorderProducer) {

        Map<String, Function<SyntheticCreationalContext<?>, ?>> creationFunctions = new LinkedHashMap<>();
        Map<String, Supplier<ActiveResult>> checkActiveSuppliers = new LinkedHashMap<>();

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            if (bean.hasRecorderInstance() && bean.isStaticInit()) {
                configureSyntheticBean(recorder, staticRecorderProducer, null, creationFunctions, checkActiveSuppliers,
                        beanRegistration, bean);
            }
        }
        // Init the map of bean instances
        recorder.initStaticSupplierBeans(creationFunctions, checkActiveSuppliers);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(SyntheticBeansRuntimeInitBuildItem.class)
    @BuildStep
    ServiceStartBuildItem initRuntime(ArcRecorder recorder,
            List<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistration, BuildProducer<BeanConfiguratorBuildItem> configurators,
            BuildProducer<MainBytecodeRecorderBuildItem> runtimeRecorderProducer) {

        Map<String, Function<SyntheticCreationalContext<?>, ?>> creationFunctions = new LinkedHashMap<>();
        Map<String, Supplier<ActiveResult>> checkActiveSuppliers = new LinkedHashMap<>();

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            if (bean.hasRecorderInstance() && !bean.isStaticInit()) {
                configureSyntheticBean(recorder, null, runtimeRecorderProducer, creationFunctions, checkActiveSuppliers,
                        beanRegistration, bean);
            }
        }
        recorder.initRuntimeSupplierBeans(creationFunctions, checkActiveSuppliers);
        return new ServiceStartBuildItem("runtime-bean-init");
    }

    @BuildStep
    void initRegular(List<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistration, BuildProducer<BeanConfiguratorBuildItem> configurators) {

        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            if (!bean.hasRecorderInstance()) {
                configureSyntheticBean(null, null, null, null, null, beanRegistration, bean);
            }
        }
    }

    /**
     * Emit retention markers for CDI service-value bean keys.
     * These keys are consumed lazily after startup and must not be cleared
     * by the between-phase or post-startup {@code retainServiceValues()} calls.
     * They are self-draining: each bean's creation function calls
     * {@code removeServiceValue()} on first access.
     */
    @BuildStep
    void trackServiceValueRetention(List<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ServiceValueRetentionBuildItem> retentionProducer) {
        Set<String> keys = new HashSet<>();
        for (SyntheticBeanBuildItem bean : syntheticBeans) {
            Class<?> serviceType = bean.configurator().getServiceType();
            if (serviceType != null) {
                List<String> nameParts = bean.configurator().getServiceNameParts();
                keys.add(LambdaTransliterator.serviceKey(serviceType, nameParts));
            }
        }
        if (!keys.isEmpty()) {
            retentionProducer.produce(new ServiceValueRetentionBuildItem(keys, true));
        }
    }

    /**
     * Configure a single synthetic bean, handling both recorder-based and
     * service-value-based creation methods.
     *
     * @param recorder the Arc recorder (null for regular/non-recorder beans)
     * @param staticRecorderProducer producer for static-init recorder items (null if not static-init)
     * @param runtimeRecorderProducer producer for runtime-init recorder items (null if not runtime-init)
     * @param creationFunctions map to populate with creation functions (null for regular beans)
     * @param checkActiveSuppliers map to populate with check-active suppliers (null for regular beans)
     * @param beanRegistration the bean registration phase
     * @param bean the synthetic bean build item
     */
    private void configureSyntheticBean(ArcRecorder recorder,
            BuildProducer<StaticBytecodeRecorderBuildItem> staticRecorderProducer,
            BuildProducer<MainBytecodeRecorderBuildItem> runtimeRecorderProducer,
            Map<String, Function<SyntheticCreationalContext<?>, ?>> creationFunctions,
            Map<String, Supplier<ActiveResult>> checkActiveSuppliers, BeanRegistrationPhaseBuildItem beanRegistration,
            SyntheticBeanBuildItem bean) {
        String name = bean.name();
        if (bean.configurator().getServiceType() != null) {
            // Bridge from service graph: the CDI bean reads the service value directly
            // from the startup context using the service key. No wrapper action needed —
            // the creation function runs lazily on first CDI access, after all startup
            // steps (including the service action) have completed.
            Class<?> serviceType = bean.configurator().getServiceType();
            List<String> serviceNameParts = bean.configurator().getServiceNameParts();
            String serviceKey = serviceType.getName() + ":" + String.join("/", serviceNameParts);

            // the creation function reads from the startup context lazily on first CDI access;
            // null is passed for StartupContext at build time — the recorder emits code that
            // passes the deploy method's StartupContext parameter at runtime
            creationFunctions.put(name, recorder.createServiceValueFunction(serviceKey, null));
        } else if (bean.configurator().getRuntimeValue() != null) {
            creationFunctions.put(name, recorder.createFunction(bean.configurator().getRuntimeValue()));
        } else if (bean.configurator().getSupplier() != null) {
            creationFunctions.put(name, recorder.createFunction(bean.configurator().getSupplier()));
        } else if (bean.configurator().getFunction() != null) {
            creationFunctions.put(name, bean.configurator().getFunction());
        } else if (bean.configurator().getRuntimeProxy() != null) {
            creationFunctions.put(name, recorder.createFunction(bean.configurator().getRuntimeProxy()));
        }
        BeanConfigurator<?> configurator = beanRegistration.getContext().configure(bean.configurator().getImplClazz())
                .read(bean.configurator());
        if (bean.hasRecorderInstance()) {
            configurator.creator(creator(name, bean));
        }
        if (bean.hasCheckActiveSupplier()) {
            configurator.checkActive(checkActive(name, bean));
            checkActiveSuppliers.put(name, bean.configurator().getCheckActive());
        }
        configurator.done();
    }

    private Consumer<BeanConfiguratorBase.CreateGeneration> creator(String name, SyntheticBeanBuildItem bean) {
        return new Consumer<BeanConfiguratorBase.CreateGeneration>() {
            @Override
            public void accept(BeanConfiguratorBase.CreateGeneration cg) {
                BlockCreator b0 = cg.createMethod();

                FieldVar staticMap = Expr.staticField(FieldDesc.of(ArcRecorder.class, "syntheticBeanProviders"));
                LocalVar function = b0.localVar("function", b0.withMap(staticMap).get(Const.of(name)));
                // Throw an exception if no supplier is found
                b0.ifNull(function, b1 -> {
                    b1.throw_(CreationException.class, createMessage("Synthetic bean instance for ", name, bean));
                });
                b0.return_(b0.invokeInterface(MethodDesc.of(Function.class, "apply", Object.class, Object.class),
                        function, cg.syntheticCreationalContext()));
            }
        };
    }

    private Consumer<BeanConfiguratorBase.CheckActiveGeneration> checkActive(String name, SyntheticBeanBuildItem bean) {
        return new Consumer<BeanConfiguratorBase.CheckActiveGeneration>() {
            @Override
            public void accept(BeanConfiguratorBase.CheckActiveGeneration cag) {
                BlockCreator b0 = cag.checkActiveMethod();

                FieldVar staticMap = Expr.staticField(FieldDesc.of(ArcRecorder.class, "syntheticBeanCheckActive"));
                LocalVar supplier = b0.localVar("supplier", b0.withMap(staticMap).get(Const.of(name)));
                b0.ifNull(supplier, b1 -> {
                    b1.throw_(CreationException.class, createMessage("ActiveResult of synthetic bean for ", name, bean));
                });
                b0.return_(b0.invokeInterface(MethodDesc.of(Supplier.class, "get", Object.class), supplier));
            }
        };
    }

    /**
     * A {@link RuntimeValue} subclass that implements {@link BytecodeRecorderImpl.ReturnedProxy}.
     * <p>
     * At deployment time, this proxy is passed to recorder methods. The recorder sees
     * it as a {@code ReturnedProxy} and generates bytecode that retrieves the actual
     * {@code RuntimeValue} wrapper from the startup context at the stored key.
     *
     * @param <T> the service type
     */
    private static final class ServiceRuntimeValueProxy<T> extends RuntimeValue<T>
            implements BytecodeRecorderImpl.ReturnedProxy {
        private final String rvKey;
        private final boolean staticInit;

        /**
         * Construct a new instance.
         *
         * @param rvKey the startup context key for the {@code RuntimeValue} wrapper
         * @param staticInit whether this proxy targets the static-init phase
         */
        ServiceRuntimeValueProxy(String rvKey, boolean staticInit) {
            this.rvKey = rvKey;
            this.staticInit = staticInit;
        }

        @Override
        public String __returned$proxy$key() {
            return rvKey;
        }

        @Override
        public boolean __static$$init() {
            return staticInit;
        }
    }

    private String createMessage(String description, String name, SyntheticBeanBuildItem bean) {
        StringBuilder builder = new StringBuilder();
        builder.append(description);
        builder.append(bean.configurator().getImplClazz());
        builder.append(" not initialized yet: ");
        builder.append(name);
        if (!bean.isStaticInit()) {
            builder.append("\n\t- a synthetic bean initialized during RUNTIME_INIT must not be accessed during STATIC_INIT");
            builder.append(
                    "\n\t- RUNTIME_INIT build steps that require access to synthetic beans initialized during RUNTIME_INIT should consume the SyntheticBeansRuntimeInitBuildItem");
        }
        return builder.toString();
    }

}
