package io.quarkus.keycloak.admin.rest.client.deployment;

import jakarta.enterprise.context.RequestScoped;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.spi.ResteasyClientProvider;
import org.keycloak.common.util.MultivaluedHashMap;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.keycloak.admin.client.common.deployment.KeycloakAdminClientInjectionEnabled;
import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfig;
import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfigUtil;
import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientJsonMapper;
import io.quarkus.keycloak.admin.rest.client.runtime.KeycloakAdminClientCreator;
import io.quarkus.keycloak.admin.rest.client.runtime.KeycloakAdminRestClientProviderPlaceholder;
import io.quarkus.keycloak.admin.rest.client.runtime.KeycloakAdminRestClientRecorder;
import io.quarkus.tls.TlsConfigurationRegistry;

public class KeycloakAdminRestClientProcessor {

    // Must be executed even if the extension is disabled, see https://github.com/quarkusio/quarkus/pull/26966/
    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.KEYCLOAK_ADMIN_REST_CLIENT);
    }

    @BuildStep
    void marker(BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> producer) {
        producer.produce(new AdditionalApplicationArchiveMarkerBuildItem("org/keycloak/admin/client/"));
        producer.produce(new AdditionalApplicationArchiveMarkerBuildItem("org/keycloak/representations"));
    }

    @BuildStep
    public void nativeImage(BuildProducer<ServiceProviderBuildItem> serviceProviderProducer,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> reflectiveHierarchyProducer) {
        // the placeholder is loaded through the ServiceLoader when the Keycloak class is initialized
        serviceProviderProducer.produce(new ServiceProviderBuildItem(ResteasyClientProvider.class.getName(),
                KeycloakAdminRestClientProviderPlaceholder.class.getName()));
        reflectiveHierarchyProducer.produce(
                new ReflectiveHierarchyIgnoreWarningBuildItem(new ReflectiveHierarchyIgnoreWarningBuildItem.DotNameExclusion(
                        DotName.createSimple(MultivaluedHashMap.class.getName()))));
    }

    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    public void integrate(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService("io.quarkus.keycloak-admin-rest-client.client-provider")
                .atPhase(Phase.INIT)
                .require(TlsConfigurationRegistry.class)
                .require(KeycloakAdminClientConfig.class)
                .require(KeycloakAdminClientJsonMapper.class)
                .onStart((ctx, registry, config, jsonMapper) -> KeycloakAdminRestClientRecorder.setClientProvider(registry,
                        config, jsonMapper.mapper()));
    }

    @BuildStep(onlyIf = KeycloakAdminClientInjectionEnabled.class)
    public void registerKeycloakAdminClientBeans(ServiceRegistrar serviceRegistrar,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        serviceRegistrar
                .forService("io.quarkus.keycloak-admin-rest-client.validate-config")
                .atPhase(Phase.INIT)
                .require(KeycloakAdminClientConfig.class)
                .onStart((ctx, config) -> KeycloakAdminClientConfigUtil.validate(config));
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem
                .configure(Keycloak.class)
                // use @RequestScoped as we don't want to keep client connection open too long
                .scope(RequestScoped.class)
                .setRuntimeInit()
                .reserve(true)
                .priority(0)
                .unremovable()
                .creator(KeycloakAdminClientCreator.class)
                .addInjectionPoint(Type.create(KeycloakAdminClientConfig.class))
                .autoClose(true)
                .done());
    }
}
