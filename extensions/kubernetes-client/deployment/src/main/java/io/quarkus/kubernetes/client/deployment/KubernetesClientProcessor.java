package io.quarkus.kubernetes.client.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.jackson.deployment.IgnoreJsonDeserializeClassBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientProducer;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;

public class KubernetesClientProcessor {

    private static final DotName WATCHER = DotName.createSimple("io.fabric8.kubernetes.client.Watcher");
    private static final DotName RESOURCE_EVENT_HANDLER = DotName
            .createSimple("io.fabric8.kubernetes.client.informers.ResourceEventHandler");
    private static final DotName KUBERNETES_RESOURCE = DotName
            .createSimple("io.fabric8.kubernetes.api.model.KubernetesResource");
    private static final DotName KUBERNETES_RESOURCE_LIST = DotName
            .createSimple("io.fabric8.kubernetes.api.model.KubernetesResourceList");

    private static final Logger log = Logger.getLogger(KubernetesClientProcessor.class.getName());

    @Inject
    BuildProducer<FeatureBuildItem> featureProducer;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;

    @Inject
    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies;

    @Inject
    BuildProducer<IgnoreJsonDeserializeClassBuildItem> ignoredJsonDeserializationClasses;

    @Inject
    BuildProducer<KubernetesRoleBindingBuildItem> roleBindingProducer;

    @BuildStep
    public void process(ApplicationIndexBuildItem applicationIndex, CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildItem) {

        featureProducer.produce(new FeatureBuildItem(Feature.KUBERNETES_CLIENT));
        roleBindingProducer.produce(new KubernetesRoleBindingBuildItem("view", true));

        // make sure the watchers fully (and not weakly) register Kubernetes classes for reflection
        final Set<DotName> watchedClasses = new HashSet<>();
        findWatchedClasses(WATCHER, applicationIndex, combinedIndexBuildItem, watchedClasses);
        findWatchedClasses(RESOURCE_EVENT_HANDLER, applicationIndex, combinedIndexBuildItem, watchedClasses);

        for (DotName className : watchedClasses) {
            final ClassInfo watchedClass = combinedIndexBuildItem.getIndex().getClassByName(className);
            if (watchedClass == null) {
                log.warnv("Unable to lookup class: {0}", className);
            } else {
                reflectiveHierarchies
                        .produce(new ReflectiveHierarchyBuildItem.Builder()
                                .type(Type.create(watchedClass.name(), Type.Kind.CLASS))
                                .source(getClass().getSimpleName() + " > " + watchedClass.name())
                                .build());
            }
        }

        final String[] modelClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(KUBERNETES_RESOURCE)
                .stream()
                .peek(c -> {
                    // we need to make sure that the Jackson extension does not try to fully register the model classes
                    // since we are going to register them weakly
                    ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(c.name()));
                })
                .map(ClassInfo::name)
                .filter(c -> !watchedClasses.contains(c))
                .map(Object::toString)
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

        if (log.isDebugEnabled()) {
            final String watchedClassNames = watchedClasses
                    .stream().map(Object::toString)
                    .sorted()
                    .collect(Collectors.joining("\n"));
            log.debugv("Watched Classes:\n{0}", watchedClassNames);
            Arrays.sort(modelClasses);
            log.debugv("Model Classes:\n{0}", String.join("\n", modelClasses));
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.KUBERNETES_CLIENT));

        // wire up the KubernetesClient bean support
        additionalBeanBuildItemBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(KubernetesClientProducer.class));
    }

    private void findWatchedClasses(final DotName implementor, final ApplicationIndexBuildItem applicationIndex,
            final CombinedIndexBuildItem combinedIndexBuildItem, final Set<DotName> watchedClasses) {
        applicationIndex.getIndex().getAllKnownImplementors(implementor)
                .forEach(c -> {
                    try {
                        final List<Type> watcherGenericTypes = JandexUtil.resolveTypeParameters(c.name(),
                                implementor, combinedIndexBuildItem.getIndex());
                        if (watcherGenericTypes.size() == 1) {
                            watchedClasses.add(watcherGenericTypes.get(0).name());
                        }
                    } catch (IllegalStateException ignored) {
                        // when the class has no subclasses and we were not able to determine the generic types, it's likely that
                        // the watcher will fail due to not being able to deserialize the class
                        if (applicationIndex.getIndex().getAllKnownSubclasses(c.name()).isEmpty()) {
                            log.warnv("{0} '{1}' will most likely not work correctly in native mode. " +
                                    "Consider specifying the generic type of '{2}' that this class handles. "
                                    +
                                    "See https://quarkus.io/guides/kubernetes-client#note-on-implementing-the-watcher-interface for more details",
                                    implementor.local(), c.name(), implementor);
                        }
                    }
                });
    }

}
