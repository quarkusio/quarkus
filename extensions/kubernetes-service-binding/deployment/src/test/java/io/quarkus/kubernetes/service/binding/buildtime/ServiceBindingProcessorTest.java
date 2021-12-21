
package io.quarkus.kubernetes.service.binding.buildtime;

import static io.quarkus.kubernetes.service.binding.buildtime.ServiceBindingProcessor.createRequirementFromConfig;
import static io.quarkus.kubernetes.service.binding.buildtime.ServiceBindingProcessor.createRequirementFromQualifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.kubernetes.service.binding.spi.ServiceBindingQualifierBuildItem;
import io.quarkus.kubernetes.service.binding.spi.ServiceBindingRequirementBuildItem;

class ServiceBindingProcessorTest {

    @Test
    public void testFullyAutomaticPostgresConfiguration() throws Exception {
        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app",
                new KubernetesServiceBindingConfig(),
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
    public void testSemiAutomaticPostgresConfiguration() throws Exception {
        KubernetesServiceBindingConfig userConfig = new KubernetesServiceBindingConfig();
        userConfig.services = new HashMap<>();
        userConfig.services.put("postgresql-default",
                ServiceConfig.createNew().withName("my-postgresql").withBinding("custom-binding"));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", userConfig,
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
    public void testManualPostgresConfiguration() throws Exception {
        KubernetesServiceBindingConfig userConfig = new KubernetesServiceBindingConfig();
        userConfig.services = new HashMap<>();
        userConfig.services.put("my-postgresql",
                ServiceConfig.createNew()
                        .withApiVersion("foo/v1")
                        .withKind("PostgresDB")
                        .withBinding("custom-binding"));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromConfig("app", "my-postgresql",
                userConfig);
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("foo/v1", r.getApiVersion());
            assertEquals("PostgresDB", r.getKind());
            assertEquals("my-postgresql", r.getName());
            assertEquals("custom-binding", r.getBinding());

        });
    }

    @Test
    public void testFullyAutomaticMysqlConfiguration() throws Exception {
        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app",
                new KubernetesServiceBindingConfig(),
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
    public void testSemiAutomaticMysqlConfiguration() throws Exception {
        KubernetesServiceBindingConfig userConfig = new KubernetesServiceBindingConfig();
        userConfig.services = new HashMap<>();
        userConfig.services.put("mysql-default",
                ServiceConfig.createNew().withName("my-mysql").withApiVersion("some.group/v1").withKind("Mysql"));

        ServiceBindingQualifierBuildItem qualifier = new ServiceBindingQualifierBuildItem("mysql", "default");
        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", userConfig, qualifier);
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("some.group/v1", r.getApiVersion());
            assertEquals("Mysql", r.getKind());
            assertEquals("my-mysql", r.getName());
            assertEquals("app-mysql-default", r.getBinding());

        });
    }

    @Test
    public void testManualMysqlConfiguration() throws Exception {
        KubernetesServiceBindingConfig userConfig = new KubernetesServiceBindingConfig();
        userConfig.services = new HashMap<>();
        userConfig.services.put("my-mysql",
                ServiceConfig.createNew()
                        .withApiVersion("foo/v1")
                        .withKind("Bar")
                        .withName("custom-name")
                        .withBinding("custom-binding"));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromConfig("app", "my-mysql", userConfig);
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("foo/v1", r.getApiVersion());
            assertEquals("Bar", r.getKind());
            assertEquals("custom-name", r.getName());
            assertEquals("custom-binding", r.getBinding());

        });
    }

    @Test
    public void testFullyAutomaticMongoConfiguration() throws Exception {
        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app",
                new KubernetesServiceBindingConfig(),
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
    public void testSemiAutomaticMongoConfiguration() throws Exception {
        KubernetesServiceBindingConfig userConfig = new KubernetesServiceBindingConfig();
        userConfig.services = new HashMap<>();
        userConfig.services.put("mongodb-default", ServiceConfig.createNew().withName("my-mongo"));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromQualifier("app", userConfig,
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
    public void testManualMongoConfiguration() throws Exception {
        KubernetesServiceBindingConfig userConfig = new KubernetesServiceBindingConfig();
        userConfig.services = new HashMap<>();
        userConfig.services.put("my-mongodb",
                ServiceConfig.createNew()
                        .withApiVersion("foo/v1")
                        .withKind("MongoDB")
                        .withName("custom-name")
                        .withBinding("custom-binding"));

        Optional<ServiceBindingRequirementBuildItem> requirement = createRequirementFromConfig("app", "my-mongodb", userConfig);
        assertTrue(requirement.isPresent());
        requirement.ifPresent(r -> {
            assertEquals("foo/v1", r.getApiVersion());
            assertEquals("MongoDB", r.getKind());
            assertEquals("custom-name", r.getName());
            assertEquals("custom-binding", r.getBinding());

        });
    }

}
