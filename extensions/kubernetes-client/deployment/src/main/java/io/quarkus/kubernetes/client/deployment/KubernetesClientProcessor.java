package io.quarkus.kubernetes.client.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.jackson.deployment.IgnoreJsonDeserializeClassBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientProducer;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

public class KubernetesClientProcessor {

    private static final DotName WATCHER = DotName.createSimple("io.fabric8.kubernetes.client.Watcher");
    private static final DotName KUBERNETES_RESOURCE = DotName
            .createSimple("io.fabric8.kubernetes.api.model.KubernetesResource");

    private static final Logger log = Logger.getLogger(KubernetesClientProcessor.class.getName());

    @Inject
    BuildProducer<FeatureBuildItem> featureProducer;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;

    @Inject
    BuildProducer<IgnoreJsonDeserializeClassBuildItem> ignoredJsonDeserializationClasses;

    @Inject
    BuildProducer<KubernetesRoleBuildItem> roleProducer;

    @BuildStep
    public void process(ApplicationIndexBuildItem applicationIndex, CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildItem) {

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.KUBERNETES_CLIENT));
        roleProducer.produce(new KubernetesRoleBuildItem("view"));

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
                        // when the class has no subclasses and we were not able to determine the generic types, it's likely that
                        // the watcher will fail due to not being able to deserialize the class
                        if (applicationIndex.getIndex().getAllKnownSubclasses(c.name()).isEmpty()) {
                            log.warn("Watcher '" + c.name() + "' will most likely not work correctly in native mode. " +
                                    "Consider specifying the generic type of 'io.fabric8.kubernetes.client.Watcher' that this class handles. "
                                    +
                                    "See https://quarkus.io/guides/kubernetes-client#note-on-implementing-the-watcher-interface for more details");
                        }
                    }
                });
        if (!watchedClasses.isEmpty()) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, watchedClasses.toArray(new String[0])));
        }

        final String[] modelClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(KUBERNETES_RESOURCE)
                .stream()
                .peek(c -> {
                    // we need to make sure that the Jackson extension does not try to fully register the model classes
                    // since we are going to register them weakly
                    ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(c.name()));
                })
                .map(c -> c.name().toString())
                .filter(c -> !watchedClasses.contains(c))
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(modelClasses).weak(true).methods(true).fields(false).build());

        // we also ignore some classes that are annotated with @JsonDeserialize that would force the registration of the entire model
        ignoredJsonDeserializationClasses.produce(
                new IgnoreJsonDeserializeClassBuildItem(DotName.createSimple("io.fabric8.kubernetes.api.model.KubeSchema")));
        ignoredJsonDeserializationClasses.produce(
                new IgnoreJsonDeserializeClassBuildItem(
                        DotName.createSimple("io.fabric8.kubernetes.api.model.KubernetesResourceList")));
        ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(KUBERNETES_RESOURCE));

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
    }
}
