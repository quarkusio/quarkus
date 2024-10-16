package io.quarkus.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

import io.quarkus.builder.Version;

public class DeployerTest {

    @Test
    void shouldNotFindDeployer() {
        Set<String> deployers = Deployer.getProjectDeployers(List.of());
        assertTrue(deployers.isEmpty());

        deployers = Deployer.getProjectDeployers(List.of(newDependency("quarkus-arc"), newDependency("quarkus-resteasy")));
        assertTrue(deployers.isEmpty());
    }

    @Test
    void shouldFindDeployer() {
        Set<String> deployers = Deployer.getProjectDeployers(
                List.of(newDependency("quarkus-arc"), newDependency("quarkus-resteasy"), newDependency("quarkus-kubernetes")));
        assertEquals(Set.of("kubernetes"), deployers);

        deployers = Deployer.getProjectDeployers(
                List.of(newDependency("quarkus-arc"), newDependency("quarkus-resteasy"), newDependency("quarkus-openshift")));
        assertEquals(Set.of("openshift"), deployers);

        deployers = Deployer.getProjectDeployers(
                List.of(newDependency("quarkus-arc"), newDependency("quarkus-resteasy"), newDependency("quarkus-kind")));
        assertEquals(Set.of("kind"), deployers);

        deployers = Deployer.getProjectDeployers(
                List.of(newDependency("quarkus-arc"), newDependency("quarkus-resteasy"), newDependency("quarkus-minikube")));
        assertEquals(Set.of("minikube"), deployers);
    }

    @Test
    void shouldFindMultipleDeployer() {
        Set<String> deployers = Deployer.getProjectDeployers(
                List.of(newDependency("quarkus-arc"), newDependency("quarkus-resteasy"), newDependency("quarkus-kubernetes"),
                        newDependency("quarkus-openshift")));
        assertEquals(Set.of("kubernetes", "openshift"), deployers);
    }

    private static Dependency newDependency(String artifactId) {
        Dependency dependency = new Dependency();
        dependency.setGroupId("io.quarkus");
        dependency.setArtifactId(artifactId);
        dependency.setVersion(Version.getVersion());
        dependency.setType("jar");
        return dependency;
    }
}
