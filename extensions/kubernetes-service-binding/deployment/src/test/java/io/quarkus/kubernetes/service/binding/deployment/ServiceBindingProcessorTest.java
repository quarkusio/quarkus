
package io.quarkus.kubernetes.service.binding.deployment;

import static io.quarkus.kubernetes.service.binding.deployment.ServiceBindingProcessor.createRequirementFromConfig;
import static io.quarkus.kubernetes.service.binding.deployment.ServiceBindingProcessor.createRequirementFromQualifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingRequirementBuildItem;

class ServiceBindingProcessorTest {

    @Test
    public void testFullyAutomaticPostgresConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", config,
                new ServiceBindingQualifierBuildItem("postgresql", "default"));
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("postgres-operator.crunchydata.com/v1beta1", r.getApiVersion());
            assertEquals("PostgresCluster", r.getKind());
            assertEquals("postgresql-default", r.getName());
            assertEquals("app-postgresql-default", r.getBinding());
        });
    }

    @Test
    public void testSemiAutomaticPostgresConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        when(serviceConfig.name()).thenReturn(Optional.of("my-postgresql"));
        when(serviceConfig.binding()).thenReturn(Optional.of("custom-binding"));
        when(config.services()).thenReturn(Map.of("postgresql-default", serviceConfig));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", config,
                new ServiceBindingQualifierBuildItem("postgresql", "default"));
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("postgres-operator.crunchydata.com/v1beta1", r.getApiVersion());
            assertEquals("PostgresCluster", r.getKind());
            assertEquals("my-postgresql", r.getName());
            assertEquals("custom-binding", r.getBinding());
        });
    }

    @Test
    public void testManualPostgresConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        when(serviceConfig.apiVersion()).thenReturn(Optional.of("foo/v1"));
        when(serviceConfig.kind()).thenReturn(Optional.of("PostgresDB"));
        when(serviceConfig.binding()).thenReturn(Optional.of("custom-binding"));
        when(config.services()).thenReturn(Map.of("my-postgresql", serviceConfig));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromConfig("app", "my-postgresql", config);
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("foo/v1", r.getApiVersion());
            assertEquals("PostgresDB", r.getKind());
            assertEquals("my-postgresql", r.getName());
            assertEquals("custom-binding", r.getBinding());
        });
    }

    @Test
    public void testFullyAutomaticMysqlConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", config,
                new ServiceBindingQualifierBuildItem("mysql", "default"));
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("pxc.percona.com/v1-9-0", r.getApiVersion());
            assertEquals("PerconaXtraDBCluster", r.getKind());
            assertEquals("mysql-default", r.getName());
            assertEquals("app-mysql-default", r.getBinding());
        });
    }

    @Test
    public void testSemiAutomaticMysqlConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        when(serviceConfig.name()).thenReturn(Optional.of("my-mysql"));
        when(serviceConfig.apiVersion()).thenReturn(Optional.of("some.group/v1"));
        when(serviceConfig.kind()).thenReturn(Optional.of("Mysql"));
        when(config.services()).thenReturn(Map.of("mysql-default", serviceConfig));

        ServiceBindingQualifierBuildItem qualifier = new ServiceBindingQualifierBuildItem("mysql", "default");
        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", config, qualifier);
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("some.group/v1", r.getApiVersion());
            assertEquals("Mysql", r.getKind());
            assertEquals("my-mysql", r.getName());
            assertEquals("app-mysql-default", r.getBinding());
        });
    }

    @Test
    public void testManualMysqlConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        when(serviceConfig.apiVersion()).thenReturn(Optional.of("foo/v1"));
        when(serviceConfig.kind()).thenReturn(Optional.of("Bar"));
        when(serviceConfig.name()).thenReturn(Optional.of("custom-name"));
        when(serviceConfig.binding()).thenReturn(Optional.of("custom-binding"));
        when(config.services()).thenReturn(Map.of("my-mysql", serviceConfig));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromConfig("app", "my-mysql", config);
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("foo/v1", r.getApiVersion());
            assertEquals("Bar", r.getKind());
            assertEquals("custom-name", r.getName());
            assertEquals("custom-binding", r.getBinding());
        });
    }

    @Test
    public void testFullyAutomaticMongoConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", config,
                new ServiceBindingQualifierBuildItem("mongodb", "default"));
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("psmdb.percona.com/v1-9-0", r.getApiVersion());
            assertEquals("PerconaServerMongoDB", r.getKind());
            assertEquals("mongodb-default", r.getName());
            assertEquals("app-mongodb-default", r.getBinding());
        });
    }

    @Test
    public void testSemiAutomaticMongoConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        when(serviceConfig.name()).thenReturn(Optional.of("my-mongo"));
        when(config.services()).thenReturn(Map.of("mongodb-default", serviceConfig));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", config,
                new ServiceBindingQualifierBuildItem("mongodb", "default"));
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("psmdb.percona.com/v1-9-0", r.getApiVersion());
            assertEquals("PerconaServerMongoDB", r.getKind());
            assertEquals("my-mongo", r.getName());
            assertEquals("app-mongodb-default", r.getBinding());
        });
    }

    @Test
    public void testManualMongoConfiguration() {
        KubernetesServiceBindingConfig config = mock(KubernetesServiceBindingConfig.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        when(serviceConfig.apiVersion()).thenReturn(Optional.of("foo/v1"));
        when(serviceConfig.kind()).thenReturn(Optional.of("MongoDB"));
        when(serviceConfig.name()).thenReturn(Optional.of("custom-name"));
        when(serviceConfig.binding()).thenReturn(Optional.of("custom-binding"));
        when(config.services()).thenReturn(Map.of("my-mongodb", serviceConfig));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromConfig("app", "my-mongodb", config);
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("foo/v1", r.getApiVersion());
            assertEquals("MongoDB", r.getKind());
            assertEquals("custom-name", r.getName());
            assertEquals("custom-binding", r.getBinding());
        });
    }
}
