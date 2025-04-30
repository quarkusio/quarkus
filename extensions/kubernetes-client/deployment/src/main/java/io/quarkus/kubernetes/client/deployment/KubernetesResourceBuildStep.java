package io.quarkus.kubernetes.client.deployment;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.kubernetes.client.KubernetesResources;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesSerializationRecorder;
import io.quarkus.kubernetes.client.spi.KubernetesResourcesBuildItem;

public class KubernetesResourceBuildStep {
    @BuildStep
    void scanKubernetesResourceClasses(
            BuildProducer<ServiceProviderBuildItem> serviceProviderProducer,
            BuildProducer<KubernetesResourcesBuildItem> kubernetesResourcesBuildItemBuildProducer) {
        serviceProviderProducer.produce(ServiceProviderBuildItem.allProvidersFromClassPath(KubernetesResource.class.getName()));
        final Set<String> resourceClasses = new HashSet<>();
        final var serviceLoader = ServiceLoader.load(KubernetesResource.class);
        for (var kr : serviceLoader) {
            final var className = kr.getClass().getName();
            // Filter build-time only available classes from those that are really available at runtime
            // e.g. The Kubernetes extension provides KubernetesResource classes only for deployment purposes (not prod code)
            if (QuarkusClassLoader.isClassPresentAtRuntime(className)) {
                resourceClasses.add(className);
            }
        }
        kubernetesResourcesBuildItemBuildProducer.produce(
                new KubernetesResourcesBuildItem(resourceClasses.toArray(String[]::new)));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem kubernetesResourceClasses(
            KubernetesSerializationRecorder recorder,
            KubernetesResourcesBuildItem kubernetesResourcesBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(KubernetesResources.class));
        final var classArray = Type.create(DotName.createSimple(Class[].class.getName()), Type.Kind.ARRAY);
        return SyntheticBeanBuildItem
                .configure(Object.class).providerType(classArray).addType(classArray)
                .scope(Singleton.class)
                .qualifiers(AnnotationInstance.builder(KubernetesResources.class).build())
                .runtimeValue(recorder.initKubernetesResources(kubernetesResourcesBuildItem.getResourceClasses()))
                .done();
    }
}
