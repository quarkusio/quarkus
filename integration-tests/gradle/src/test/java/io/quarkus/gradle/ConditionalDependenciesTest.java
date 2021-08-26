package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.bytebuddy.ByteBuddy;

/**
 * This class uses test order because all tests depend on extension publication which can be done once.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConditionalDependenciesTest extends QuarkusGradleWrapperTestBase {

    @Test
    @Order(1)
    public void publishTestExtensions() throws IOException, InterruptedException, URISyntaxException {
        File dependencyProject = getProjectDir("conditional-dependencies");
        runGradleWrapper(dependencyProject, ":ext-a:runtime:publishToMavenLocal",
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
                ":ext-u:deployment:publishToMavenLocal");
    }

    @Test
    @Order(2)
    public void shouldImportConditionalDependency() throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir("conditional-test-project");

        runGradleWrapper(projectDir, "clean", ":runner:quarkusBuild", "-Dquarkus.package.type=mutable-jar");

        final File buildDir = new File(projectDir, "runner" + File.separator + "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        assertThat(mainLib.resolve("org.acme.ext-a-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-b-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-c-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-e-1.0-SNAPSHOT.jar")).exists();
        assertThat(mainLib.resolve("org.acme.ext-d-1.0-SNAPSHOT.jar")).doesNotExist();

        String byteBuddyJar = Optional.ofNullable(ByteBuddy.class.getProtectionDomain().getCodeSource().getLocation())
                .map(url -> Pattern.compile("byte-buddy-(\\d.+)\\.jar").matcher(url.getPath()))
                .filter(Matcher::find)
                .map(matcher -> "net.bytebuddy.byte-buddy-" + matcher.group(1) + ".jar")
                .orElseThrow(() -> new IllegalStateException("Could not determine byte-buddy version"));

        assertThat(mainLib.resolve(byteBuddyJar)).doesNotExist();

        final Path deploymentLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("deployment");
        assertThat(deploymentLib.resolve(byteBuddyJar)).exists();
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
        // M -> N?(K) -> I -> O?(H)
        // M -> R?(I) -> S?(T) -> U

        final File projectDir = getProjectDir("conditional-test-project");

        runGradleWrapper(projectDir, "clean", ":scenario-two:quarkusBuild", "-Dquarkus.package.type=mutable-jar");

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
}
