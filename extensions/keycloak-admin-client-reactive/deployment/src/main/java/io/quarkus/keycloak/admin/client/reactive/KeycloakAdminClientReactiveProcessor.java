package io.quarkus.keycloak.admin.client.reactive;

import jakarta.enterprise.context.RequestScoped;

import org.jboss.jandex.DotName;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.spi.ResteasyClientProvider;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.json.StringListMapDeserializer;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.keycloak.admin.client.common.AutoCloseableDestroyer;
import io.quarkus.keycloak.admin.client.common.KeycloakAdminClientInjectionEnabled;
import io.quarkus.keycloak.admin.client.reactive.runtime.ResteasyReactiveClientProvider;
import io.quarkus.keycloak.admin.client.reactive.runtime.ResteasyReactiveKeycloakAdminClientRecorder;

public class KeycloakAdminClientReactiveProcessor {

    @BuildStep
    void marker(BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> producer) {
        producer.produce(new AdditionalApplicationArchiveMarkerBuildItem("org/keycloak/admin/client/"));
        producer.produce(new AdditionalApplicationArchiveMarkerBuildItem("org/keycloak/representations"));
    }

    @BuildStep
    public void nativeImage(BuildProducer<ServiceProviderBuildItem> serviceProviderProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> reflectiveHierarchyProducer) {
        serviceProviderProducer.produce(new ServiceProviderBuildItem(ResteasyClientProvider.class.getName(),
                ResteasyReactiveClientProvider.class.getName()));
        reflectiveClassProducer.produce(ReflectiveClassBuildItem.builder(
                StringListMapDeserializer.class,
                StringOrArrayDeserializer.class,
                StringOrArraySerializer.class)
                .constructors(true)
                .methods(true)
                .build());
        reflectiveHierarchyProducer.produce(
                new ReflectiveHierarchyIgnoreWarningBuildItem(new ReflectiveHierarchyIgnoreWarningBuildItem.DotNameExclusion(
                        DotName.createSimple(MultivaluedHashMap.class.getName()))));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    public void integrate(ResteasyReactiveKeycloakAdminClientRecorder recorder) {
        recorder.setClientProvider();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = KeycloakAdminClientInjectionEnabled.class)
    public void registerKeycloakAdminClientBeans(ResteasyReactiveKeycloakAdminClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem
                .configure(Keycloak.class)
                // use @RequestScoped as we don't want to keep client connection open too long
                .scope(RequestScoped.class)
                .setRuntimeInit()
                .defaultBean()
                .unremovable()
                .supplier(recorder.createAdminClient())
                .destroyer(AutoCloseableDestroyer.class)
                .done());
    }
}
