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
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.AnyType;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubeSchema;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.extension.ExtensionAdapter;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.impl.KubernetesClientImpl;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
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
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.jackson.deployment.IgnoreJsonDeserializeClassBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.KubernetesClientObjectMapperProducer;
import io.quarkus.kubernetes.client.runtime.KubernetesClientProducer;
import io.quarkus.kubernetes.client.runtime.KubernetesConfigProducer;
import io.quarkus.kubernetes.client.runtime.KubernetesSerializationProducer;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

public class KubernetesClientProcessor {

    private static final Logger log = Logger.getLogger(KubernetesClientProcessor.class.getName());
    private static final DotName WATCHER = DotName.createSimple(Watcher.class.getName());
    private static final DotName RESOURCE_EVENT_HANDLER = DotName
            .createSimple(io.fabric8.kubernetes.client.informers.ResourceEventHandler.class.getName());
    private static final DotName KUBERNETES_RESOURCE = DotName.createSimple(KubernetesResource.class.getName());
    private static final DotName KUBERNETES_RESOURCE_LIST = DotName
            .createSimple(KubernetesResourceList.class.getName());
    private static final DotName KUBE_SCHEMA = DotName.createSimple(KubeSchema.class.getName());
    private static final DotName VISITABLE_BUILDER = DotName.createSimple(VisitableBuilder.class.getName());
    private static final DotName CUSTOM_RESOURCE = DotName.createSimple(CustomResource.class.getName());
    private static final String SERVICE_ACCOUNT = "ServiceAccount";

    private static final DotName JSON_FORMAT = DotName.createSimple(JsonFormat.class.getName());
    private static final String[] EMPTY_STRINGS_ARRAY = new String[0];

    @BuildStep
    public void registerBeanProducers(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildItem,
            Capabilities capabilities) {
        additionalBeanBuildItemBuildItem
                .produce(AdditionalBeanBuildItem.unremovableOf(KubernetesClientObjectMapperProducer.class));
        additionalBeanBuildItemBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(KubernetesSerializationProducer.class));
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
                .produce(new RuntimeReinitializedClassBuildItem(io.fabric8.kubernetes.client.utils.Utils.class.getName()));
    }

    @BuildStep
    RemovedResourceBuildItem unregisterVertHttpClient() {
        // Exclude VertxHttpClientFactory in favour of QuarkusHttpClientFactory
        return new RemovedResourceBuildItem(ArtifactKey.fromString("io.fabric8:kubernetes-httpclient-vertx"),
                Collections.singleton("META-INF/services/io.fabric8.kubernetes.client.http.HttpClient$Factory"));
    }

    @BuildStep
    public void process(ApplicationIndexBuildItem applicationIndex, CombinedIndexBuildItem combinedIndexBuildItem,
            KubernetesClientBuildConfig kubernetesClientConfig,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<FeatureBuildItem> featureProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies,
            BuildProducer<IgnoreJsonDeserializeClassBuildItem> ignoredJsonDeserializationClasses,
            BuildProducer<ServiceProviderBuildItem> serviceProviderProducer,
            BuildProducer<KubernetesClientCapabilityBuildItem> kubernetesClientCapabilityProducer) {

        featureProducer.produce(new FeatureBuildItem(Feature.KUBERNETES_CLIENT));

        kubernetesClientCapabilityProducer
                .produce(new KubernetesClientCapabilityBuildItem(kubernetesClientConfig.generateRbac()));

        // register fully (and not weakly) for reflection watchers, informers and custom resources
        final Set<DotName> watchedClasses = new HashSet<>();
        final var appIndex = applicationIndex.getIndex();
        final var fullIndex = combinedIndexBuildItem.getIndex();
        findWatchedClasses(WATCHER, watchedClasses, 1, true, appIndex, fullIndex);
        findWatchedClasses(RESOURCE_EVENT_HANDLER, watchedClasses, 1, true, appIndex, fullIndex);
        // in the case of CustomResources, we want to search the full index to also take into account additional
        // dependencies that might not be in the application index, the most common use case being the model classes
        // being defined in a separate module for reuse and indexed separately, which would cause the CustomResource
        // implementations to not appear in the application index
        findWatchedClasses(CUSTOM_RESOURCE, watchedClasses, 2, false, fullIndex, fullIndex);

        Predicate<DotName> reflectionIgnorePredicate = ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE;
        for (DotName className : watchedClasses) {
            if (reflectionIgnorePredicate.test(className)) {
                continue;
            }
            final ClassInfo watchedClass = fullIndex.getClassByName(className);
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

        Collection<ClassInfo> kubernetesResourceImpls = fullIndex.getAllKnownImplementors(KUBERNETES_RESOURCE);
        Collection<ClassInfo> kubernetesResourceListImpls = fullIndex.getAllKnownImplementors(KUBERNETES_RESOURCE_LIST);
        Collection<ClassInfo> visitableBuilderImpls = fullIndex.getAllKnownImplementors(VISITABLE_BUILDER);

        // default sizes determined experimentally - these are only set in order to prevent continuous expansion of the array list
        List<String> withoutFieldsRegistration = new ArrayList<>(
                kubernetesResourceImpls.size() + kubernetesResourceListImpls.size());
        List<String> withFieldsRegistration = new ArrayList<>(2);
        List<DotName> ignoreJsonDeserialization = new ArrayList<>(
                kubernetesResourceImpls.size() + kubernetesResourceListImpls.size());

        populateReflectionRegistrationLists(kubernetesResourceImpls, watchedClasses, ignoreJsonDeserialization,
                withoutFieldsRegistration,
                withFieldsRegistration);
        populateReflectionRegistrationLists(kubernetesResourceListImpls, watchedClasses, ignoreJsonDeserialization,
                withoutFieldsRegistration,
                withFieldsRegistration);
        populateReflectionRegistrationLists(visitableBuilderImpls, watchedClasses, ignoreJsonDeserialization,
                withoutFieldsRegistration,
                withFieldsRegistration);

        if (!withFieldsRegistration.isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem
                    .builder(withFieldsRegistration.toArray(EMPTY_STRINGS_ARRAY)).weak(true).methods().fields()
                    .build());
        }
        if (!withoutFieldsRegistration.isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem
                    .builder(withoutFieldsRegistration.toArray(EMPTY_STRINGS_ARRAY)).weak(true).methods()
                    .build());
        }

        ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(ignoreJsonDeserialization));

        // we also ignore some classes that are annotated with @JsonDeserialize that would force the registration of the entire model
        ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(KUBE_SCHEMA));
        ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(KUBERNETES_RESOURCE_LIST));
        ignoredJsonDeserializationClasses.produce(new IgnoreJsonDeserializeClassBuildItem(KUBERNETES_RESOURCE));

        final String[] deserializerClasses = fullIndex
                .getAllKnownSubclasses(DotName.createSimple("com.fasterxml.jackson.databind.JsonDeserializer"))
                .stream()
                .map(c -> c.name().toString())
                .filter(s -> s.startsWith("io.fabric8.kubernetes"))
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(deserializerClasses).methods().build());

        final String[] serializerClasses = fullIndex
                .getAllKnownSubclasses(DotName.createSimple("com.fasterxml.jackson.databind.JsonSerializer"))
                .stream()
                .map(c -> c.name().toString())
                .filter(s -> s.startsWith("io.fabric8.kubernetes"))
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(serializerClasses).methods().build());

        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(KubernetesClientImpl.class, DefaultKubernetesClient.class, VersionInfo.class)
                        .methods().fields().build());
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(AnyType.class, IntOrString.class, KubernetesDeserializer.class).methods().build());

        // exec credentials support
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(Config.ExecCredential.class,
                        Config.ExecCredentialSpec.class,
                        Config.ExecCredentialStatus.class).methods().fields().build());

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

        // Register all HttpClient implementations
        serviceProviderProducer.produce(ServiceProviderBuildItem.allProvidersFromClassPath(HttpClient.Factory.class.getName()));
        // Register all KubernetesResource providers (needed for the KubernetesDeserializer)
        serviceProviderProducer.produce(ServiceProviderBuildItem.allProvidersFromClassPath(KubernetesResource.class.getName()));
        // Register all KubernetesClient extensions
        serviceProviderProducer.produce(ServiceProviderBuildItem.allProvidersFromClassPath(ExtensionAdapter.class.getName()));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.KUBERNETES_CLIENT));
    }

    private static void populateReflectionRegistrationLists(Collection<ClassInfo> kubernetesResourceImpls,
            Set<DotName> watchedClasses,
            List<DotName> ignoredJsonDeserializationClasses,
            List<String> withoutFieldsRegistration,
            List<String> withFieldsRegistration) {
        kubernetesResourceImpls
                .stream()
                .peek(c -> {
                    // we need to make sure that the Jackson extension does not try to fully register the model classes
                    // since we are going to register them weakly
                    ignoredJsonDeserializationClasses.add(c.name());
                })
                .filter(c -> !watchedClasses.contains(c.name()))
                .map(c -> {
                    boolean registerFields = false;
                    List<AnnotationInstance> jsonFormatInstances = c.annotationsMap().get(JSON_FORMAT);
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
    }

    private void findWatchedClasses(final DotName implementedOrExtendedClass,
            final Set<DotName> watchedClasses, final int expectedGenericTypeCardinality, boolean isTargetClassAnInterface,
            IndexView targetIndex,
            IndexView fullIndex) {

        final var implementors = isTargetClassAnInterface ? targetIndex.getAllKnownImplementors(implementedOrExtendedClass)
                : targetIndex.getAllKnownSubclasses(implementedOrExtendedClass);
        implementors.forEach(c -> {
            try {
                final List<Type> watcherGenericTypes = JandexUtil.resolveTypeParameters(c.name(), implementedOrExtendedClass,
                        fullIndex);
                if (!isTargetClassAnInterface) {
                    // add the class itself: for example, in the case of CustomResource, we want to
                    // register the class that extends CustomResource in addition to its type parameters
                    watchedClasses.add(c.name());
                }
                if (watcherGenericTypes.size() == expectedGenericTypeCardinality) {
                    watcherGenericTypes.forEach(t -> watchedClasses.add(t.name()));
                }
            } catch (IllegalArgumentException ignored) {
                // when the class has no subclasses and we were not able to determine the generic types,
                // it's likely that the class might be able to get deserialized
                if (targetIndex.getAllKnownSubclasses(c.name()).isEmpty()) {
                    log.warnv("{0} '{1}' will most likely not work correctly in native mode. " +
                            "Consider specifying the generic type of '{2}' that this class handles. "
                            +
                            "See https://quarkus.io/guides/kubernetes-client#note-on-generic-types for more details",
                            implementedOrExtendedClass.local(), c.name(), implementedOrExtendedClass);
                }
            }
        });
    }

}
