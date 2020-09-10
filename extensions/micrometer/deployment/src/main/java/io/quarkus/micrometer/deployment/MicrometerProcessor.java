package io.quarkus.micrometer.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.*;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.micrometer.deployment.export.PrometheusRegistryProcessor;
import io.quarkus.micrometer.runtime.ClockProvider;
import io.quarkus.micrometer.runtime.CompositeRegistryCreator;
import io.quarkus.micrometer.runtime.MeterFilterConstraint;
import io.quarkus.micrometer.runtime.MeterFilterConstraints;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.metrics.MetricsFactory;

public class MicrometerProcessor {
    private static final DotName METER_REGISTRY = DotName.createSimple(MeterRegistry.class.getName());
    private static final DotName METER_BINDER = DotName.createSimple(MeterBinder.class.getName());
    private static final DotName METER_FILTER = DotName.createSimple(MeterFilter.class.getName());
    private static final DotName NAMING_CONVENTION = DotName.createSimple(NamingConvention.class.getName());

    static class MicrometerEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return mConfig.enabled;
        }
    }

    MicrometerConfig mConfig;

    @BuildStep(onlyIf = MicrometerEnabled.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.MICROMETER);
    }

    @BuildStep(onlyIf = MicrometerEnabled.class, onlyIfNot = PrometheusRegistryProcessor.PrometheusEnabled.class)
    MetricsCapabilityBuildItem metricsCapabilityBuildItem() {
        return new MetricsCapabilityBuildItem(x -> MetricsFactory.MICROMETER.equals(x),
                null);
    }

    @BuildStep(onlyIf = { MicrometerEnabled.class, PrometheusRegistryProcessor.PrometheusEnabled.class })
    MetricsCapabilityBuildItem metricsCapabilityPrometheusBuildItem() {
        return new MetricsCapabilityBuildItem(x -> MetricsFactory.MICROMETER.equals(x),
                mConfig.export.prometheus.path);
    }

    @BuildStep(onlyIf = MicrometerEnabled.class)
    UnremovableBeanBuildItem registerAdditionalBeans(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<MicrometerRegistryProviderBuildItem> providerClasses,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Create and keep JVM/System MeterBinders
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(ClockProvider.class)
                .addBeanClass(CompositeRegistryCreator.class)
                .addBeanClass(MeterFilterConstraint.class)
                .addBeanClass(MeterFilterConstraints.class)
                .build());

        IndexView index = indexBuildItem.getIndex();

        // Find classes that define MeterRegistries, MeterBinders, and MeterFilters
        Collection<String> knownRegistries = new HashSet<>();
        collectNames(index.getAllKnownSubclasses(METER_REGISTRY), knownRegistries);

        Collection<String> knownClasses = new HashSet<>();
        knownClasses.add(METER_BINDER.toString());
        collectNames(index.getAllKnownImplementors(METER_BINDER), knownClasses);
        knownClasses.add(METER_FILTER.toString());
        collectNames(index.getAllKnownImplementors(METER_FILTER), knownClasses);
        knownClasses.add(NAMING_CONVENTION.toString());
        collectNames(index.getAllKnownImplementors(NAMING_CONVENTION), knownClasses);

        Set<String> keepMe = new HashSet<>();

        // Find and keep _producers_ of those MeterRegistries, MeterBinders, and
        // MeterFilters
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.PRODUCES)) {
            AnnotationTarget target = annotation.target();
            switch (target.kind()) {
                case METHOD:
                    MethodInfo method = target.asMethod();
                    String returnType = method.returnType().name().toString();
                    if (knownRegistries.contains(returnType)) {
                        providerClasses.produce(new MicrometerRegistryProviderBuildItem(returnType));
                        keepMe.add(method.declaringClass().name().toString());
                    } else if (knownClasses.contains(returnType)) {
                        keepMe.add(method.declaringClass().name().toString());
                    }
                    break;
                case FIELD:
                    FieldInfo field = target.asField();
                    String fieldType = field.type().name().toString();
                    if (knownRegistries.contains(fieldType)) {
                        providerClasses.produce(new MicrometerRegistryProviderBuildItem(fieldType));
                        keepMe.add(field.declaringClass().name().toString());
                    } else if (knownClasses.contains(fieldType)) {
                        keepMe.add(field.declaringClass().name().toString());
                    }
                    break;
                default:
                    break;
            }
        }

        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(keepMe));
    }

    void collectNames(Collection<ClassInfo> classes, Collection<String> names) {
        for (ClassInfo info : classes) {
            names.add(info.name().toString());
        }
    }

    @BuildStep(onlyIf = MicrometerEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    RootMeterRegistryBuildItem createRootRegistry(MicrometerRecorder recorder,
            MicrometerConfig config,
            BeanContainerBuildItem beanContainerBuildItem) {
        // BeanContainerBuildItem is present to indicate we call this after Arc is initialized

        RuntimeValue<MeterRegistry> registry = recorder.createRootRegistry(config);
        return new RootMeterRegistryBuildItem(registry);
    }

    @BuildStep(onlyIf = MicrometerEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    void registerExtensionMetrics(MicrometerRecorder recorder,
            RootMeterRegistryBuildItem rootMeterRegistryBuildItem,
            List<MetricsFactoryConsumerBuildItem> metricsFactoryConsumerBuildItems) {
        // RootMeterRegistryBuildItem is present to indicate we call this after the root registry has been initialized

        for (MetricsFactoryConsumerBuildItem item : metricsFactoryConsumerBuildItems) {
            if (item.executionTime() == ExecutionTime.STATIC_INIT) {
                recorder.registerMetrics(item.getConsumer());
            }
        }
    }

    @BuildStep(onlyIf = MicrometerEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureRegistry(MicrometerRecorder recorder,
            MicrometerConfig config,
            RootMeterRegistryBuildItem rootMeterRegistryBuildItem,
            List<MicrometerRegistryProviderBuildItem> providerClassItems,
            List<MetricsFactoryConsumerBuildItem> metricsFactoryConsumerBuildItems,
            List<MicrometerRegistryProviderBuildItem> providerClasses,
            ShutdownContextBuildItem shutdownContextBuildItem) {
        // RootMeterRegistryBuildItem is present to indicate we call this after the root registry has been initialized

        Set<Class<? extends MeterRegistry>> typeClasses = new HashSet<>();
        for (MicrometerRegistryProviderBuildItem item : providerClassItems) {
            typeClasses.add(item.getRegistryClass());
        }

        // Runtime config at play here: host+port, API keys, etc.
        recorder.configureRegistries(config, typeClasses, shutdownContextBuildItem);

        for (MetricsFactoryConsumerBuildItem item : metricsFactoryConsumerBuildItems) {
            if (item.executionTime() == ExecutionTime.RUNTIME_INIT) {
                recorder.registerMetrics(item.getConsumer());
            }
        }
    }
}
