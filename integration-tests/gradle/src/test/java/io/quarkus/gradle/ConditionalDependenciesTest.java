package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.bootstrap.utils.BuildToolHelper;

/**
 * This class uses test order because all tests depend on extension publication which can be done once.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConditionalDependenciesTest extends QuarkusGradleWrapperTestBase {

    @Test
    @Order(1)
    public void publishTestExtensions() throws IOException, InterruptedException, URISyntaxException {
        File dependencyProject = getProjectDir("conditional-dependencies");
        runGradleWrapper(dependencyProject, ":transitive-dependency:publishToMavenLocal",
                ":simple-dependency:publishToMavenLocal");
        runGradleWrapper(dependencyProject,
                ":ext-a:runtime:publishToMavenLocal",
                ":ext-a:deployment:publishToMavenLocal",
                ":ext-b:runtime:publishToMavenLocal",
                ":ext-b:deployment:publishToMavenLocal",
                ":ext-c:runtime:publishToMavenLocal",
                ":ext-c:deployment:publishToMavenLocal",
                ":ext-d:runtime:publishToMavenLocal",
                ":ext-d:deployment:publishToMavenLocal",
                ":ext-e:runtime:publishToMavenLocal",
                ":ext-e:deployment:publishToMavenLocal",
                ":ext-f:runtime:publishToMavenLocal",
                ":ext-f:deployment:publishToMavenLocal",
                ":ext-g:runtime:publishToMavenLocal",
                ":ext-g:deployment:publishToMavenLocal",
                ":ext-h:runtime:publishToMavenLocal",
                ":ext-h:deployment:publishToMavenLocal",
                ":ext-i:runtime:publishToMavenLocal",
                ":ext-i:deployment:publishToMavenLocal",
                ":ext-j:runtime:publishToMavenLocal",
                ":ext-j:deployment:publishToMavenLocal",
                ":ext-k:runtime:publishToMavenLocal",
                ":ext-k:deployment:publishToMavenLocal",
                ":ext-l:runtime:publishToMavenLocal",
                ":ext-l:deployment:publishToMavenLocal",
                ":ext-m:runtime:publishToMavenLocal",
                ":ext-m:deployment:publishToMavenLocal",
                ":ext-n:runtime:publishToMavenLocal",
                ":ext-n:deployment:publishToMavenLocal",
                ":ext-o:runtime:publishToMavenLocal",
                ":ext-o:deployment:publishToMavenLocal",
                ":ext-p:runtime:publishToMavenLocal",
                ":ext-p:deployment:publishToMavenLocal",
                ":ext-r:runtime:publishToMavenLocal",
                ":ext-r:deployment:publishToMavenLocal",
                ":ext-s:runtime:publishToMavenLocal",
                ":ext-s:deployment:publishToMavenLocal",
                ":ext-t:runtime:publishToMavenLocal",
                ":ext-t:deployment:publishToMavenLocal",
                ":ext-u:runtime:publishToMavenLocal",
                ":ext-u:deployment:publishToMavenLocal",
                ":ext-cap-requirer:runtime:publishToMavenLocal",
                ":ext-cap-requirer:deployment:publishToMavenLocal",
                ":ext-cap-provider:runtime:publishToMavenLocal",
                ":ext-cap-provider:deployment:publishToMavenLocal",
                ":ext-cap-cond:runtime:publishToMavenLocal",
                ":ext-cap-cond:deployment:publishToMavenLocal",
                ":ext-gp-b:runtime:publishToMavenLocal",
                ":ext-gp-b:deployment:publishToMavenLocal",
                ":ext-gp-c:runtime:publishToMavenLocal",
                ":ext-gp-c:deployment:publishToMavenLocal",
                ":ext-gp-e:runtime:publishToMavenLocal",
                ":ext-gp-e:deployment:publishToMavenLocal",
                ":ext-gp-g:runtime:publishToMavenLocal",
                ":ext-gp-g:deployment:publishToMavenLocal",
                ":ext-gp-i:runtime:publishToMavenLocal",
                ":ext-gp-i:deployment:publishToMavenLocal",
                ":acme-platform-descriptor:publishToMavenLocal",
                ":acme-platform-properties:publishToMavenLocal",
                ":acme-bom:publishToMavenLocal",
                ":dev-mode-only-lib:publishToMavenLocal");
        runGradleWrapper(dependencyProject,
                ":ext-gp-a:runtime:publishToMavenLocal",
                ":ext-gp-a:deployment:publishToMavenLocal",
                ":ext-gp-d:runtime:publishToMavenLocal",
                ":ext-gp-d:deployment:publishToMavenLocal",
                ":ext-gp-f:runtime:publishToMavenLocal",
                ":ext-gp-f:deployment:publishToMavenLocal",
                ":ext-gp-h:runtime:publishToMavenLocal",
                ":ext-gp-h:deployment:publishToMavenLocal");
    }

    @Test
    @Order(2)
    public void shouldImportConditionalDependency() throws IOException, URISyntaxException, InterruptedException {

        // A -> B?(C) -> E?(C)
        // C
        // T

        final File projectDir = getProjectDir("conditional-test-project");

        runGradleWrapper(projectDir, "clean", ":runner:quarkusBuild", "-Dquarkus.package.jar.type=mutable-jar");

        final File buildDir = new File(projectDir, "runner" + File.separator + "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        assertThat(mainLib.resolve("org.acme.ext-a-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-b-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-c-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-e-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-t-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-d-1.0-SNAPSHOT.jar")).doesNotExist();
        assertThat(mainLib.resolve("org.acme.transitive-dependency-1.0-SNAPSHOT.jar")).doesNotExist();

        final Path deploymentLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("deployment");
        assertThat(deploymentLib.resolve("org.acme.transitive-dependency-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("io.quarkus.quarkus-agroal-" + getQuarkusVersion() + ".jar")).doesNotExist();
    }

    @Test
    @Order(3)
    public void shouldNotImportConditionalDependency() throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("conditional-test-project");

        runGradleWrapper(projectDir, "clean", ":runner-with-exclude:quarkusBuild");

        final File buildDir = new File(projectDir, "runner-with-exclude" + File.separator + "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        assertThat(mainLib.resolve("org.acme.ext-a-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-b-1.0-SNAPSHOT.jar")).doesNotExist();
        assertThat(mainLib.resolve("org.acme.ext-c-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-e-1.0-SNAPSHOT.jar")).doesNotExist();
        assertThat(mainLib.resolve("org.acme.ext-d-1.0-SNAPSHOT.jar")).doesNotExist();
    }

    @Test
    @Order(4)
    public void shouldNotFailIfConditionalDependencyIsExplicitlyDeclared()
            throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("conditional-test-project");

        runGradleWrapper(projectDir, "clean", ":runner-with-explicit-import:quarkusBuild");

        final File buildDir = new File(projectDir, "runner-with-explicit-import" + File.separator + "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        assertThat(mainLib.resolve("org.acme.ext-a-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-b-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-c-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-e-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-d-1.0-SNAPSHOT.jar")).doesNotExist();
    }

    @Test
    @Order(5)
    public void scenarioTwo() throws Exception {

        // F -> G -> H?(I,J) -> K -> T
        // L -> J -> P?(O)
        // M -> N?(G) -> I -> O?(H)
        // M -> R?(I) -> S?(T) -> U

        final File projectDir = getProjectDir("conditional-test-project");

        runGradleWrapper(projectDir, "clean", ":scenario-two:quarkusBuild", "-Dquarkus.package.jar.type=mutable-jar");

        final File buildDir = new File(projectDir, "scenario-two" + File.separator + "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        assertThat(mainLib.resolve("org.acme.ext-f-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-g-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-h-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-i-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-j-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-k-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-l-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-m-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-n-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-o-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-p-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-r-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-s-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-t-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-u-1.0-SNAPSHOT.jar")).exists();

        final Path deploymentLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("deployment");
        assertThat(deploymentLib.resolve("org.acme.ext-f-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-g-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-h-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-i-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-j-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-k-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-l-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-m-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-n-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-o-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-p-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-r-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-s-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-t-deployment-1.0-SNAPSHOT.jar")).exists();
        assertThat(deploymentLib.resolve("org.acme.ext-u-deployment-1.0-SNAPSHOT.jar")).exists();
    }

    @Test
    @Order(6)
    public void conditionalDevDependencies() throws Exception {

        // A -> B?(C) -> E?(C)
        // A -> D?(dev)
        // A -> N?(dev, G)
        // A -> S? (dev, T) -> U
        // C -> dev-mode-only-lib? (dev) -> P
        // T

        var appModel = BuildToolHelper
                .enableGradleAppModelForDevMode(getProjectDir("conditional-test-project/runner").toPath());
        final Set<String> acmeArtifacts = new HashSet<>();
        for (var d : appModel.getDependencies()) {
            if (d.getGroupId().equals("org.acme")) {
                acmeArtifacts.add(d.getArtifactId());
            }
        }
        assertThat(acmeArtifacts).containsExactlyInAnyOrder(
                "ext-a", "ext-a-deployment",
                "ext-b", "ext-b-deployment",
                "ext-c", "ext-c-deployment",
                "ext-d", "ext-d-deployment",
                "ext-e", "ext-e-deployment",
                "ext-p", "ext-p-deployment",
                "ext-s", "ext-s-deployment",
                "ext-u", "ext-u-deployment",
                "ext-t", "ext-t-deployment",
                "simple-dependency", "transitive-dependency",
                "dev-mode-only-lib");
    }

    @Test
    @Order(7)
    public void conditionalDevDependenciesInProdMode() throws Exception {

        // A -> B?(C) -> E?(C)
        // A -> D?(dev)
        // A -> N?(dev, G)
        // A -> S? (dev, T) -> U
        // C -> dev-mode-only-lib? (dev) -> P
        // T

        var appModel = BuildToolHelper
                .enableGradleAppModelForProdMode(getProjectDir("conditional-test-project/runner").toPath());
        final Set<String> acmeArtifacts = new HashSet<>();
        for (var d : appModel.getDependencies()) {
            if (d.getGroupId().equals("org.acme")) {
                acmeArtifacts.add(d.getArtifactId());
            }
        }
        assertThat(acmeArtifacts).containsExactlyInAnyOrder(
                "ext-a", "ext-a-deployment",
                "ext-b", "ext-b-deployment",
                "ext-c", "ext-c-deployment",
                "ext-e", "ext-e-deployment",
                "ext-t", "ext-t-deployment",
                "simple-dependency", "transitive-dependency");
    }

    @Test
    @Order(8)
    public void defaultCapabilityProviderWithConditionalDeps()
            throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("conditional-test-project");

        runGradleWrapper(projectDir, "clean", ":runner-with-default-cap:quarkusBuild",
                "-Dquarkus.package.jar.type=mutable-jar");

        final File buildDir = new File(projectDir, "runner-with-default-cap" + File.separator + "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        // ext-cap-requirer is an explicit dependency
        assertThat(mainLib.resolve("org.acme.ext-cap-requirer-1.0-SNAPSHOT.jar")).exists();
        // ext-cap-provider should be automatically injected as a default capability provider
        assertThat(mainLib.resolve("org.acme.ext-cap-provider-1.0-SNAPSHOT.jar")).exists();
        // ext-cap-cond is a conditional dep of ext-cap-provider, conditioned on ext-cap-requirer;
        // it should be activated because ext-cap-requirer is present
        assertThat(mainLib.resolve("org.acme.ext-cap-cond-1.0-SNAPSHOT.jar")).exists();
    }

    @Test
    @Order(9)
    public void defaultCapabilityProviderGraphPosition()
            throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("conditional-test-project");

        runGradleWrapper(projectDir, "clean", ":runner-with-default-cap-graph-position:quarkusBuild",
                "-Dquarkus.package.jar.type=mutable-jar");

        final File buildDir = new File(projectDir, "runner-with-default-cap-graph-position" + File.separator + "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        // ext-gp-a and ext-gp-d are explicit dependencies
        assertThat(mainLib.resolve("org.acme.ext-gp-a-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-gp-d-1.0-SNAPSHOT.jar")).exists();

        // ext-gp-f should be injected as default provider for cap.gp.x (ext-gp-c has higher BFS priority than ext-gp-e)
        assertThat(mainLib.resolve("org.acme.ext-gp-f-1.0-SNAPSHOT.jar")).exists();
        // ext-gp-g is a dependency of ext-gp-f and provides cap.gp.y, satisfying ext-gp-e's requirement
        assertThat(mainLib.resolve("org.acme.ext-gp-g-1.0-SNAPSHOT.jar")).exists();

        // ext-gp-h should NOT be injected because cap.gp.y is already satisfied by ext-gp-g
        assertThat(mainLib.resolve("org.acme.ext-gp-h-1.0-SNAPSHOT.jar")).doesNotExist();
        // ext-gp-i should NOT be present since ext-gp-h was not injected
        assertThat(mainLib.resolve("org.acme.ext-gp-i-1.0-SNAPSHOT.jar")).doesNotExist();
    }

}
