package io.quarkus.openshift.client.deployment;

import java.util.Collections;

import org.jboss.jandex.DotName;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.impl.OpenShiftClientImpl;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.IgnoreSplitPackageBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.it.openshift.client.runtime.OpenShiftClientProducer;

public class OpenShiftClientProcessor {

    @BuildStep
    public void registerBeanProducers(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemProducer) {
        // wire up the OpenShiftClient bean support
        additionalBeanBuildItemProducer.produce(AdditionalBeanBuildItem.unremovableOf(OpenShiftClientProducer.class));
    }

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPENSHIFT_CLIENT);
    }

    @BuildStep
    public void process(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ServiceProviderBuildItem> serviceProviderProducer) {

        final String[] deserializerClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("com.fasterxml.jackson.databind.JsonDeserializer"))
                .stream()
                .map(c -> c.name().toString())
                .filter(s -> s.startsWith("io.fabric8.openshift"))
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(deserializerClasses)
                .reason(getClass().getName())
                .methods().build());

        final String[] serializerClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("com.fasterxml.jackson.databind.JsonSerializer"))
                .stream()
                .map(c -> c.name().toString())
                .filter(s -> s.startsWith("io.fabric8.openshift"))
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(serializerClasses)
                .reason(getClass().getName())
                .methods().build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(OpenShiftClientImpl.class, DefaultOpenShiftClient.class)
                .reason(getClass().getName())
                .methods().fields().build());
    }

    @BuildStep
    public IgnoreSplitPackageBuildItem splitPackages() {
        return new IgnoreSplitPackageBuildItem(Collections.singleton("io.fabric8.openshift.api.model"));
    }
}
