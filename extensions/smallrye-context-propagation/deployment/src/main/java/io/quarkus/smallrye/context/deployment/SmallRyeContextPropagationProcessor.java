package io.quarkus.smallrye.context.deployment;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.smallrye.context.deployment.spi.ThreadContextProviderBuildItem;
import io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationProvider;
import io.quarkus.smallrye.context.runtime.SmallRyeContextPropagationRecorder;
import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.api.ThreadContextConfig;

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
    void buildStatic(SmallRyeContextPropagationRecorder recorder, List<ThreadContextProviderBuildItem> threadContextProviders)
            throws ClassNotFoundException, IOException {
        List<ThreadContextProvider> discoveredProviders = new ArrayList<>();
        List<ContextManagerExtension> discoveredExtensions = new ArrayList<>();
        List<Class<?>> providers = threadContextProviders.stream().map(ThreadContextProviderBuildItem::getProvider)
                .collect(Collectors.toCollection(ArrayList::new));
        ServiceUtil.classesNamedIn(Thread.currentThread().getContextClassLoader(),
                "META-INF/services/" + ThreadContextProvider.class.getName()).forEach(providers::add);
        for (Class<?> provider : providers) {
            try {
                discoveredProviders.add((ThreadContextProvider) provider.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate declared ThreadContextProvider class: " + provider.getName(),
                        e);
            }
        }
        for (Class<?> extension : ServiceUtil.classesNamedIn(Thread.currentThread().getContextClassLoader(),
                "META-INF/services/" + ContextManagerExtension.class.getName())) {
            try {
                discoveredExtensions.add((ContextManagerExtension) extension.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
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
            ShutdownContextBuildItem shutdownContextBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_CONTEXT_PROPAGATION));

        recorder.configureRuntime(executorBuildItem.getExecutorProxy(), shutdownContextBuildItem);

        // Synthetic bean for ManagedExecutor
        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(SmallRyeManagedExecutor.class)
                        .scope(ApplicationScoped.class)
                        .addType(ManagedExecutor.class)
                        .defaultBean()
                        .unremovable()
                        .supplier(recorder.initializeManagedExecutor(executorBuildItem.getExecutorProxy()))
                        .setRuntimeInit().done());
    }

    // transform IPs for ManagedExecutor/ThreadContext that use config annotation and don't yet have @NamedInstance
    @BuildStep
    InjectionPointTransformerBuildItem transformInjectionPoint() {
        return new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {
            @Override
            public boolean appliesTo(Type requiredType) {
                // filter the type, we only care about ManagedExecutor/ThreadContext injection points
                DotName typeName = requiredType.name();
                return DotNames.MANAGED_EXECUTOR.equals(typeName) || DotNames.THREAD_CONTEXT.equals(typeName);
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                // we don't care about any injection point that has custom qualifiers on it (including @NamedInstance)
                if (transformationContext.getQualifiers().stream()
                        .anyMatch(ann -> !ann.name().equals(io.quarkus.arc.processor.DotNames.ANY)
                                && !ann.name().equals(io.quarkus.arc.processor.DotNames.DEFAULT))) {
                    return;
                }
                // create a unique name based on the injection point
                String mpConfigIpName;
                AnnotationTarget target = transformationContext.getTarget();
                final String nameDelimiter = "/";
                switch (target.kind()) {
                    case FIELD:
                        mpConfigIpName = target.asField().declaringClass().name().toString()
                                + nameDelimiter
                                + target.asField().name();
                        break;
                    case METHOD_PARAMETER:
                        mpConfigIpName = target.asMethodParameter().method().declaringClass().name().toString()
                                + nameDelimiter
                                + target.asMethodParameter().method().name()
                                + nameDelimiter
                                + (target.asMethodParameter().position() + 1);
                        break;
                    // any other value is unexpected and we skip that
                    default:
                        return;
                }
                AnnotationInstance meConfigInstance = Annotations.find(transformationContext.getAllAnnotations(),
                        DotNames.MANAGED_EXECUTOR_CONFIG);
                AnnotationInstance tcConfigInstance = Annotations.find(transformationContext.getAllAnnotations(),
                        DotNames.THREAD_CONTEXT_CONFIG);

                if (meConfigInstance != null || tcConfigInstance != null) {
                    // add @NamedInstance with the generated name
                    transformationContext.transform()
                            .add(DotNames.NAMED_INSTANCE, AnnotationValue.createStringValue("value", mpConfigIpName)).done();
                }
            }
        });
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createSynthBeansForConfiguredInjectionPoints(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            SmallRyeContextPropagationRecorder recorder, BeanDiscoveryFinishedBuildItem bdFinishedBuildItem) {
        Map<String, ExecutorConfig> executorMap = new HashMap<>();
        Set<String> unconfiguredExecutorIPs = new HashSet<>();
        Map<String, ThreadConfig> threadContextMap = new HashMap<>();
        Set<String> unconfiguredContextIPs = new HashSet<>();
        for (InjectionPointInfo ipInfo : bdFinishedBuildItem.getInjectionPoints()) {
            AnnotationInstance namedAnnotation = ipInfo.getRequiredQualifier(DotNames.NAMED_INSTANCE);
            // only look for IP with @NamedInstance on it because the IP transformation made sure it's there
            if (namedAnnotation == null) {
                continue;
            }
            // furthermore, we only look for any IP that doesn't have other custom qualifier
            if (ipInfo.getRequiredQualifiers().stream()
                    .anyMatch(ann -> !ann.name().equals(DotNames.NAMED_INSTANCE)
                            && !ann.name().equals(io.quarkus.arc.processor.DotNames.ANY)
                            && !ann.name().equals(io.quarkus.arc.processor.DotNames.DEFAULT))) {
                continue;
            }

            AnnotationInstance meConfigInstance = Annotations.find(extractAnnotations(ipInfo.getTarget()),
                    DotNames.MANAGED_EXECUTOR_CONFIG);
            AnnotationInstance tcConfigInstance = Annotations.find(extractAnnotations(ipInfo.getTarget()),
                    DotNames.THREAD_CONTEXT_CONFIG);

            // get the name from @NamedInstance qualifier
            String nameValue = namedAnnotation.value().asString();

            if (meConfigInstance == null && tcConfigInstance == null) {
                // injection point with @NamedInstance on it but no configuration
                if (ipInfo.getType().name().equals(DotNames.MANAGED_EXECUTOR)) {
                    unconfiguredExecutorIPs.add(nameValue);
                } else {
                    unconfiguredContextIPs.add(nameValue);
                }
                continue;
            }
            // we are looking for injection points with @ManagedExecutorConfig/@ThreadContextConfig
            if (meConfigInstance != null || tcConfigInstance != null) {
                if (meConfigInstance != null) {
                    // parse ME config annotation and store in a map
                    executorMap.putIfAbsent(nameValue,
                            new ExecutorConfig(meConfigInstance.value("cleared"),
                                    meConfigInstance.value("propagated"),
                                    meConfigInstance.value("maxAsync"),
                                    meConfigInstance.value("maxQueued")));

                } else if (tcConfigInstance != null) {
                    // parse TC config annotation
                    threadContextMap.putIfAbsent(nameValue,
                            new ThreadConfig(tcConfigInstance.value("cleared"),
                                    tcConfigInstance.value("propagated"),
                                    tcConfigInstance.value("unchanged")));
                }
            }
        }
        // check all unconfigured IPs, if we also found same name and configured ones, then drop these from the set
        unconfiguredExecutorIPs.removeAll(unconfiguredExecutorIPs.stream()
                .filter((name) -> (executorMap.containsKey(name)))
                .collect(Collectors.toSet()));

        unconfiguredContextIPs.removeAll(unconfiguredContextIPs.stream()
                .filter((name) -> (threadContextMap.containsKey(name)))
                .collect(Collectors.toSet()));

        // add beans for configured ManagedExecutors
        for (Map.Entry<String, ExecutorConfig> entry : executorMap.entrySet()) {
            syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(ManagedExecutor.class)
                    .defaultBean()
                    .unremovable()
                    .setRuntimeInit()
                    .scope(ApplicationScoped.class)
                    .addQualifier().annotation(DotNames.NAMED_INSTANCE).addValue("value", entry.getKey())
                    .done()
                    .supplier(recorder.initializeConfiguredManagedExecutor(entry.getValue().cleared,
                            entry.getValue().propagated, entry.getValue().maxAsync, entry.getValue().maxQueued))
                    // disposers should be unnecessary as all beans run on Quarkus thread pool
                    .done());
        }

        // add beans for unconfigured ManagedExecutors
        for (String ipName : unconfiguredExecutorIPs) {
            syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(ManagedExecutor.class)
                    .defaultBean()
                    .unremovable()
                    .setRuntimeInit()
                    .scope(ApplicationScoped.class)
                    .addQualifier().annotation(DotNames.NAMED_INSTANCE).addValue("value", ipName).done()
                    .supplier(recorder.initializeConfiguredManagedExecutor(
                            ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.cleared(),
                            ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.propagated(),
                            ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync(),
                            ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued()))
                    // disposers should be unnecessary as all beans run on Quarkus thread pool
                    .done());
        }

        // add beans for configured ThreadContext
        for (Map.Entry<String, ThreadConfig> entry : threadContextMap.entrySet()) {
            syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(ThreadContext.class)
                    .defaultBean()
                    .unremovable()
                    .setRuntimeInit()
                    .scope(ApplicationScoped.class)
                    .addQualifier().annotation(DotNames.NAMED_INSTANCE).addValue("value", entry.getKey())
                    .done()
                    .supplier(recorder.initializeConfiguredThreadContext(entry.getValue().cleared,
                            entry.getValue().propagated,
                            entry.getValue().unchanged))
                    // disposers should be unnecessary as all beans run on Quarkus thread pool
                    .done());
        }

        // add beans for unconfigured ThreadContext
        for (String ipName : unconfiguredContextIPs) {
            syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(ThreadContext.class)
                    .defaultBean()
                    .unremovable()
                    .setRuntimeInit()
                    .scope(ApplicationScoped.class)
                    .addQualifier().annotation(DotNames.NAMED_INSTANCE).addValue("value", ipName).done()
                    .supplier(recorder.initializeConfiguredThreadContext(
                            ThreadContextConfig.Literal.DEFAULT_INSTANCE.cleared(),
                            ThreadContextConfig.Literal.DEFAULT_INSTANCE.propagated(),
                            ThreadContextConfig.Literal.DEFAULT_INSTANCE.unchanged()))
                    // disposers should be unnecessary as all beans run on Quarkus thread pool
                    .done());
        }
    }

    private Collection<AnnotationInstance> extractAnnotations(AnnotationTarget target) {
        switch (target.kind()) {
            case FIELD:
                return target.asField().annotations();
            case METHOD_PARAMETER:
                return target.asMethodParameter().method().annotations();
            // any other value is unexpected and we skip that
            default:
                return Collections.EMPTY_SET;
        }
    }

    class ExecutorConfig {

        String[] cleared;
        String[] propagated;
        int maxAsync;
        int maxQueued;

        ExecutorConfig(AnnotationValue cleared, AnnotationValue propagated, AnnotationValue maxAsync,
                AnnotationValue maxQueued) {
            this.cleared = cleared == null ? ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.cleared() : cleared.asStringArray();
            this.propagated = propagated == null ? ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.propagated()
                    : propagated.asStringArray();
            this.maxAsync = maxAsync == null ? ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync() : maxAsync.asInt();
            this.maxQueued = maxQueued == null ? ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued() : maxQueued.asInt();
        }
    }

    class ThreadConfig {
        String[] cleared;
        String[] propagated;
        String[] unchanged;

        ThreadConfig(AnnotationValue cleared, AnnotationValue propagated, AnnotationValue unchanged) {
            this.cleared = cleared == null ? ThreadContextConfig.Literal.DEFAULT_INSTANCE.cleared() : cleared.asStringArray();
            this.propagated = propagated == null ? ThreadContextConfig.Literal.DEFAULT_INSTANCE.propagated()
                    : propagated.asStringArray();
            this.unchanged = unchanged == null ? ThreadContextConfig.Literal.DEFAULT_INSTANCE.unchanged()
                    : unchanged.asStringArray();
        }
    }
}
