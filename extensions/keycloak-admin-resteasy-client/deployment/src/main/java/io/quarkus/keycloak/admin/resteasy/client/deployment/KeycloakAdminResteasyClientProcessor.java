package io.quarkus.keycloak.admin.resteasy.client.deployment;

import jakarta.enterprise.context.RequestScoped;

import org.jboss.jandex.DotName;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.json.StringListMapDeserializer;
import org.keycloak.json.StringOrArrayDeserializer;
import org.keycloak.json.StringOrArraySerializer;

import io.quarkus.arc.BeanDestroyer;
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
import io.quarkus.keycloak.admin.client.common.deployment.KeycloakAdminClientInjectionEnabled;
import io.quarkus.keycloak.admin.resteasy.client.runtime.KeycloakAdminResteasyClientRecorder;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;

public class KeycloakAdminResteasyClientProcessor {

    @BuildStep
    ReflectiveHierarchyIgnoreWarningBuildItem marker(BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> prod) {
        prod.produce(new AdditionalApplicationArchiveMarkerBuildItem("org/keycloak/admin/client/"));
        prod.produce(new AdditionalApplicationArchiveMarkerBuildItem("org/keycloak/representations"));
        return new ReflectiveHierarchyIgnoreWarningBuildItem(new ReflectiveHierarchyIgnoreWarningBuildItem.DotNameExclusion(
                DotName.createSimple(MultivaluedHashMap.class.getName())));
    }

    @BuildStep
    ReflectiveClassBuildItem reflect() {
        return ReflectiveClassBuildItem.builder(ResteasyClientBuilderImpl.class, JacksonProvider.class, ProxyBuilderImpl.class,
                StringListMapDeserializer.class,
                StringOrArrayDeserializer.class,
                StringOrArraySerializer.class)
                .reason(getClass().getName())
                .methods().build();
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void avoidRuntimeInitIssueInClientBuilderWrapper(KeycloakAdminResteasyClientRecorder recorder) {
        recorder.avoidRuntimeInitIssueInClientBuilderWrapper();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    public void integrate(KeycloakAdminResteasyClientRecorder recorder, TlsRegistryBuildItem tlsRegistryBuildItem) {
        recorder.setClientProvider(tlsRegistryBuildItem.registry());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = KeycloakAdminClientInjectionEnabled.class)
    public void registerKeycloakAdminClientBeans(KeycloakAdminResteasyClientRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem
                .configure(Keycloak.class)
                // use @RequestScoped as we don't want to keep client connection open too long
                .scope(RequestScoped.class)
                .setRuntimeInit()
                .defaultBean()
                .unremovable()
                .supplier(recorder.createAdminClient())
                .destroyer(BeanDestroyer.AutoCloseableDestroyer.class)
                .done());
    }
}
