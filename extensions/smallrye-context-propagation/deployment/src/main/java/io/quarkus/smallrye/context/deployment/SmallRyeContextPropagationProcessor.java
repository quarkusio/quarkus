package io.quarkus.smallrye.context.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationProvider;
import io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationRecorder;

/**
 * The deployment processor for MP-CP applications
 */
class SmallRyeContextPropagationProcessor {
    private static final Logger log = Logger.getLogger(SmallRyeContextPropagationProcessor.class.getName());

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
        for (Class<?> provider : ServiceUtil.classesNamedIn(SmallRyeContextPropagationRecorder.class.getClassLoader(),
                "META-INF/services/" + ThreadContextProvider.class.getName())) {
            try {
                discoveredProviders.add((ThreadContextProvider) provider.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to instantiate declared ThreadContextProvider class: " + provider.getName(),
                        e);
            }
        }
        for (Class<?> extension : ServiceUtil.classesNamedIn(SmallRyeContextPropagationRecorder.class.getClassLoader(),
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
            BeanContainerBuildItem beanContainer,
            ExecutorBuildItem executorBuildItem,
            BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_CONTEXT_PROPAGATION));

        recorder.configureRuntime(beanContainer.getValue(), executorBuildItem.getExecutorProxy());
    }
}
