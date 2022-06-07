package io.quarkus.kubernetes.client.deployment;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.jackson.deployment.IgnoreJsonDeserializeClassBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.KubernetesClientProducer;
import io.quarkus.kubernetes.client.runtime.KubernetesConfigProducer;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;

public class KubernetesClientProcessor {

    private static final DotName WATCHER = DotName.createSimple("io.fabric8.kubernetes.client.Watcher");
    private static final DotName RESOURCE_EVENT_HANDLER = DotName
            .createSimple("io.fabric8.kubernetes.client.informers.ResourceEventHandler");
    private static final DotName KUBERNETES_RESOURCE = DotName
            .createSimple("io.fabric8.kubernetes.api.model.KubernetesResource");
    private static final DotName CUSTOM_RESOURCE = DotName.createSimple("io.fabric8.kubernetes.client.CustomResource");

    private static final Logger log = Logger.getLogger(KubernetesClientProcessor.class.getName());

    private static final Predicate<DotName> IS_OKHTTP_CLASS = d -> d.toString().startsWith("okhttp3");
    private static final DotName JSON_FORMAT = DotName.createSimple(JsonFormat.class.getName());

    @BuildStep
    public void registerBeanProducers(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildItem,
            Capabilities capabilities) {
        // wire up the Config bean support
        additionalBeanBuildItemBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(KubernetesConfigProducer.class));
        // do not register our client producer if the openshift client is present, because it provides it too
        if (capabilities.isMissing(Capability.OPENSHIFT_CLIENT)) {
            // wire up the KubernetesClient bean support
            additionalBeanBuildItemBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(KubernetesClientProducer.class));
        }
    }

    @BuildStep
    public void nativeImageSupport(BuildProducer<RuntimeReinitializedClassBuildItem> runtimeInitializedClassProducer) {
        runtimeInitializedClassProducer
                .produce(new RuntimeReinitializedClassBuildItem("io.fabric8.kubernetes.client.utils.Utils"));
    }

    @BuildStep
    public void process(ApplicationIndexBuildItem applicationIndex, CombinedIndexBuildItem combinedIndexBuildItem,
            KubernetesClientBuildConfig kubernetesClientConfig,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<FeatureBuildItem> featureProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies,
            BuildProducer<IgnoreJsonDeserializeClassBuildItem> ignoredJsonDeserializationClasses,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindingProducer) {

        featureProducer.produce(new FeatureBuildItem(Feature.KUBERNETES_CLIENT));
        if (kubernetesClientConfig.generateRbac) {
            roleBindingProducer.produce(new KubernetesRoleBindingBuildItem("view", true));
        }

        // register fully (and not weakly) for reflection watchers, informers and custom resources
        final Set<DotName> watchedClasses = new HashSet<>();
        findWatchedClasses(WATCHER, applicationIndex, combinedIndexBuildItem, watchedClasses, 1);
        findWatchedClasses(RESOURCE_EVENT_HANDLER, applicationIndex, combinedIndexBuildItem, watchedClasses, 1);
        findWatchedClasses(CUSTOM_RESOURCE, applicationIndex, combinedIndexBuildItem, watchedClasses, 2);

        Predicate<DotName> reflectionIgnorePredicate = ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE
                .or(IS_OKHTTP_CLASS);
        for (DotName className : watchedClasses) {
            if (reflectionIgnorePredicate.test(className)) {
                continue;
            }
            final ClassInfo watchedClass = combinedIndexBuildItem.getIndex().getClassByName(className);
            if (watchedClass == null) {
                log.warnv("Unable to lookup class: {0}", className);
            } else {
                reflectiveHierarchies
                        .produce(new ReflectiveHierarchyBuildItem.Builder()
                                .type(Type.create(watchedClass.name(), Type.Kind.CLASS))
                                .ignoreTypePredicate(reflectionIgnorePredicate)
                                .source(getClass().getSimpleName() + " > " + watchedClass.name())
                                .build());
            }
        }

        Collection<ClassInfo> kubernetesResourceImpls = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(KUBERNETES_RESOURCE);
        // default sizes determined experimentally - these are only set in order to prevent continuous expansion of the array list
        List<String> withoutFieldsRegistration = new ArrayList<>(kubernetesResourceImpls.size());
        List<String> withFieldsRegistration = new ArrayList<>(2);
        kubernetesResourceImpls
                .stream()
                .peek(c -> {
                    // we need to make sure that the Jackson extension does not try to fully register the model classes
                    // since we are going to register them weakly
                    ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(c.name()));
                })
                .filter(c -> !watchedClasses.contains(c.name()))
                .map(c -> {
                    boolean registerFields = false;
                    List<AnnotationInstance> jsonFormatInstances = c.annotations().get(JSON_FORMAT);
                    if (jsonFormatInstances != null) {
                        for (AnnotationInstance jsonFormatInstance : jsonFormatInstances) {
                            if (jsonFormatInstance.target().kind() == AnnotationTarget.Kind.FIELD) {
                                registerFields = true;
                                break;
                            }
                        }
                    }
                    return new AbstractMap.SimpleEntry<>(c.name(), registerFields);
                }).forEach(e -> {
                    if (e.getValue()) {
                        withFieldsRegistration.add(e.getKey().toString());
                    } else {
                        withoutFieldsRegistration.add(e.getKey().toString());
                    }
                });
        if (!withFieldsRegistration.isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem
                    .builder(withFieldsRegistration.toArray(new String[0])).weak(true).methods(true).fields(true).build());
        }
        if (!withoutFieldsRegistration.isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem
                    .builder(withoutFieldsRegistration.toArray(new String[0])).weak(true).methods(true).fields(false).build());
        }

        // we also ignore some classes that are annotated with @JsonDeserialize that would force the registration of the entire model
        ignoredJsonDeserializationClasses.produce(
                new IgnoreJsonDeserializeClassBuildItem(DotName.createSimple("io.fabric8.kubernetes.api.model.KubeSchema")));
        ignoredJsonDeserializationClasses.produce(
                new IgnoreJsonDeserializeClassBuildItem(
                        DotName.createSimple("io.fabric8.kubernetes.api.model.KubernetesResourceList")));
        ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(KUBERNETES_RESOURCE));

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
        reflectiveClasses
                .produce(new ReflectiveClassBuildItem(true, true, "io.fabric8.kubernetes.client.VersionInfo"));

        if (log.isDebugEnabled()) {
            final String watchedClassNames = watchedClasses
                    .stream().map(Object::toString)
                    .sorted()
                    .collect(Collectors.joining("\n"));
            log.debugv("Watched Classes:\n{0}", watchedClassNames);
            List<String> modelClasses = new ArrayList<>(withFieldsRegistration.size() + withoutFieldsRegistration.size());
            modelClasses.addAll(withFieldsRegistration);
            modelClasses.addAll(withoutFieldsRegistration);
            Collections.sort(modelClasses);
            log.debugv("Model Classes:\n{0}", String.join("\n", modelClasses));
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.KUBERNETES_CLIENT));
    }

    private void findWatchedClasses(final DotName implementor, final ApplicationIndexBuildItem applicationIndex,
            final CombinedIndexBuildItem combinedIndexBuildItem, final Set<DotName> watchedClasses,
            final int expectedGenericTypeCardinality) {
        applicationIndex.getIndex().getAllKnownImplementors(implementor)
                .forEach(c -> {
                    try {
                        final List<Type> watcherGenericTypes = JandexUtil.resolveTypeParameters(c.name(),
                                implementor, combinedIndexBuildItem.getIndex());
                        if (watcherGenericTypes.size() == expectedGenericTypeCardinality) {
                            watcherGenericTypes.forEach(t -> watchedClasses.add(t.name()));
                        }
                    } catch (IllegalArgumentException ignored) {
                        // when the class has no subclasses and we were not able to determine the generic types,
                        // it's likely that the class might be able to get deserialized
                        if (applicationIndex.getIndex().getAllKnownSubclasses(c.name()).isEmpty()) {
                            log.warnv("{0} '{1}' will most likely not work correctly in native mode. " +
                                    "Consider specifying the generic type of '{2}' that this class handles. "
                                    +
                                    "See https://quarkus.io/guides/kubernetes-client#note-on-generic-types for more details",
                                    implementor.local(), c.name(), implementor);
                        }
                    }
                });
    }

}
