package io.quarkus.kubernetes.client.deployment;

import javax.inject.Inject;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

public class KubernetesClientProcessor {

    @Inject
    BuildProducer<FeatureBuildItem> featureProducer;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;

    @BuildStep
    public void process(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.KUBERNETES_CLIENT));

        final String[] modelClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple("io.fabric8.kubernetes.api.model.KubernetesResource"))
                .stream()
                .map(c -> c.name().toString())
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

        reflectiveClasses
                .produce(new ReflectiveClassBuildItem(true, false, "io.fabric8.kubernetes.internal.KubernetesDeserializer"));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.KUBERNETES_CLIENT));
    }
}
