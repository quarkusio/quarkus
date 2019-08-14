package io.quarkus.kubernetes.client.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.KubernetesClientProducer;
import io.quarkus.kubernetes.client.runtime.KubernetesClientRecorder;

public class KubernetesClientProcessor {

    private static final DotName WATCHER = DotName.createSimple("io.fabric8.kubernetes.client.Watcher");

    @Inject
    BuildProducer<FeatureBuildItem> featureProducer;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;

    KubernetesClientBuildConfig buildConfig;

    @Record(STATIC_INIT)
    @BuildStep
    public void process(ApplicationIndexBuildItem applicationIndex, CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListenerBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildItem,
            KubernetesClientRecorder recorder) {
        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.KUBERNETES_CLIENT));

        Set<String> watchedClasses = new HashSet<>();
        // make sure the watchers fully (and not weakly) register Kubernetes classes for reflection
        applicationIndex.getIndex().getAllKnownImplementors(WATCHER)
                .forEach(c -> {
                    try {
                        final List<Type> watcherGenericTypes = JandexUtil.resolveTypeParameters(c.name(),
                                WATCHER, combinedIndexBuildItem.getIndex());
                        if (watcherGenericTypes.size() == 1) {
                            watchedClasses.add(watcherGenericTypes.get(0).name().toString());
                        }
                    } catch (IllegalStateException ignored) {
                    } // no need to handle cases when the generic types could not be determined
                });
        if (!watchedClasses.isEmpty()) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, watchedClasses.toArray(new String[0])));
        }

        final String[] modelClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple("io.fabric8.kubernetes.api.model.KubernetesResource"))
                .stream()
                .map(c -> c.name().toString())
                .filter(c -> !watchedClasses.contains(c))
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.weakClass(modelClasses));

        final String[] doneables = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple("io.fabric8.kubernetes.api.model.Doneable"))
                .stream()
                .map(c -> c.name().toString())
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.weakClass(doneables));

        final String[] deserializerClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("com.fasterxml.jackson.databind.JsonDeserializer"))
                .stream()
                .map(c -> c.name().toString())
                .filter(s -> s.startsWith("io.fabric8.kubernetes"))
                .toArray(String[]::new);
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, deserializerClasses));

        final String[] serializerClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("com.fasterxml.jackson.databind.JsonSerializer"))
                .stream()
                .map(c -> c.name().toString())
                .filter(s -> s.startsWith("io.fabric8.kubernetes"))
                .toArray(String[]::new);
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, serializerClasses));

        reflectiveClasses
                .produce(new ReflectiveClassBuildItem(true, false, "io.fabric8.kubernetes.api.model.IntOrString"));
        reflectiveClasses
                .produce(new ReflectiveClassBuildItem(true, false, "io.fabric8.kubernetes.internal.KubernetesDeserializer"));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.KUBERNETES_CLIENT));

        // wire up the KubernetesClient bean support
        additionalBeanBuildItemBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(KubernetesClientProducer.class));
        beanContainerListenerBuildItem.produce(new BeanContainerListenerBuildItem(
                recorder.setBuildConfig(buildConfig)));
    }
}
