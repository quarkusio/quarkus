package io.quarkus.keycloak.admin.client.reactive;

import org.jboss.jandex.DotName;
import org.keycloak.admin.client.spi.ResteasyClientProvider;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.json.StringListMapDeserializer;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

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

}
