package io.quarkus.smallrye.context.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ManagedExecutorInitializedBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationProvider;
import io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationRecorder;
import io.smallrye.context.SmallRyeManagedExecutor;

/**
 * The deployment processor for MP-CP applications
 */
class SmallRyeContextPropagationProcessor {

    @BuildStep
    void registerBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans
                .produce(AdditionalBeanBuildItem.unremovableOf(SmallRyeContextPropagationProvider.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void buildStatic(SmallRyeContextPropagationRecorder recorder)
            throws ClassNotFoundException, IOException {
        List<ThreadContextProvider> discoveredProviders = new ArrayList<>();
        List<ContextManagerExtension> discoveredExtensions = new ArrayList<>();
        for (Class<?> provider : ServiceUtil.classesNamedIn(Thread.currentThread().getContextClassLoader(),
                "META-INF/services/" + ThreadContextProvider.class.getName())) {
            try {
                discoveredProviders.add((ThreadContextProvider) provider.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to instantiate declared ThreadContextProvider class: " + provider.getName(),
                        e);
            }
        }
        for (Class<?> extension : ServiceUtil.classesNamedIn(Thread.currentThread().getContextClassLoader(),
                "META-INF/services/" + ContextManagerExtension.class.getName())) {
            try {
                discoveredExtensions.add((ContextManagerExtension) extension.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to instantiate declared ThreadContextProvider class: " + extension.getName(),
                        e);
            }
        }

        recorder.configureStaticInit(discoveredProviders, discoveredExtensions);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void build(SmallRyeContextPropagationRecorder recorder,
            ExecutorBuildItem executorBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ManagedExecutorInitializedBuildItem> managedExecutorInitialized,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_CONTEXT_PROPAGATION));

        recorder.configureRuntime(executorBuildItem.getExecutorProxy());

        // Synthetic bean for ManagedExecutor
        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(SmallRyeManagedExecutor.class)
                        .scope(ApplicationScoped.class)
                        .addType(ManagedExecutor.class)
                        .defaultBean()
                        .unremovable()
                        .supplier(recorder.initializeManagedExecutor(executorBuildItem.getExecutorProxy()))
                        .setRuntimeInit().done());

        // This should be removed at some point after Quarkus 1.7
        managedExecutorInitialized.produce(new ManagedExecutorInitializedBuildItem());
    }
}
