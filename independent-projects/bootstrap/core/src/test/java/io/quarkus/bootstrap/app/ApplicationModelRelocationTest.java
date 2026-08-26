package io.quarkus.bootstrap.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

class ApplicationModelRelocationTest {

    @TempDir
    Path tempDir;

    /**
     * The point of the exercise: the same model, built in two different places, has to serialize to the
     * same bytes.
     */
    @Test
    void modelsFromDifferentCheckoutsSerializeIdentically() throws IOException {
        Path checkoutA = tempDir.resolve("slot-a");
        Path checkoutB = tempDir.resolve("slot-b");

        String serializedA = serializeAt(checkoutA);
        String serializedB = serializeAt(checkoutB);

        assertThat(serializedA).isEqualTo(serializedB);
        assertThat(serializedA).doesNotContain(asJson(checkoutA), asJson(checkoutB));
    }

    /**
     * A relocated model has to come back with paths that are absolute again, and correct for the
     * environment reading it.
     */
    @Test
    void relocatedModelIsResolvedBackToAbsolutePaths() throws IOException {
        Path checkout = tempDir.resolve("slot-a");
        Path file = tempDir.resolve("model-a.dat");
        ApplicationModelSerializer.serialize(modelAt(checkout), file, roots(checkout));

        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(file, roots(checkout));

        assertThat(deserialized.getAppArtifact().getResolvedPaths().getSinglePath())
                .isEqualTo(checkout.resolve("target/classes"));
    }

    /**
     * The same serialized model read against a different project location resolves to that location -
     * this is what makes a cache entry produced elsewhere usable here.
     */
    @Test
    void relocatedModelIsResolvedAgainstTheReadingEnvironment() throws IOException {
        Path writtenAt = tempDir.resolve("slot-a");
        Path readAt = tempDir.resolve("slot-b");
        Path file = tempDir.resolve("model.dat");
        ApplicationModelSerializer.serialize(modelAt(writtenAt), file, roots(writtenAt));

        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(file, roots(readAt));

        assertThat(deserialized.getAppArtifact().getResolvedPaths().getSinglePath())
                .isEqualTo(readAt.resolve("target/classes"));
    }

    /**
     * A model written before relocation existed, or with it disabled, carries no roots and has to be read
     * back exactly as it is.
     */
    @Test
    void modelWithoutRelocationIsReadUnchanged() throws IOException {
        Path checkout = tempDir.resolve("slot-a");
        Path file = tempDir.resolve("model-absolute.dat");
        ApplicationModelSerializer.serialize(modelAt(checkout), file, List.of());

        assertThat(Files.readString(file))
                .contains(asJson(checkout))
                .doesNotContain(ApplicationModelRelocation.RELOCATION_ROOTS);

        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(file, roots(checkout));
        assertThat(deserialized.getAppArtifact().getResolvedPaths().getSinglePath())
                .isEqualTo(checkout.resolve("target/classes"));
    }

    /**
     * Only the names of the roots may be recorded: writing their locations would put the checkout
     * directory straight back into the file.
     */
    @Test
    void recordedRootsCarryNamesOnly() throws IOException {
        Path checkout = tempDir.resolve("slot-a");
        Path file = tempDir.resolve("model.dat");
        ApplicationModelSerializer.serialize(modelAt(checkout), file, roots(checkout));

        assertThat(Files.readString(file))
                .contains(ApplicationModelRelocation.RELOCATION_ROOTS)
                .doesNotContain(asJson(checkout));
    }

    /**
     * A reader that has nothing but the model's path - a forked test or dev JVM - must resolve a
     * sibling module's jar correctly in a multi-module build.
     * <p>
     * Regression test: the root of the build used to be guessed as the project directory, which
     * silently resolved {@code <root>/common/build/libs/common.jar} to
     * {@code <root>/application/common/build/libs/common.jar} - a path that does not exist.
     */
    @Test
    void siblingModuleJarResolvesForAReaderThatOnlyHasThePath() throws IOException {
        Path rootDir = tempDir.resolve("build-root");
        Path projectDir = rootDir.resolve("application");
        Path buildDir = projectDir.resolve("build");
        Path siblingJar = rootDir.resolve("common/build/libs/common.jar");
        Path modelFile = buildDir.resolve("quarkus/application-model/quarkus-app-model.dat");

        ApplicationModel model = new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("com.example")
                        .setArtifactId("application")
                        .setVersion("1.0.0")
                        .setResolvedPath(buildDir.resolve("classes/java/main")))
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("com.example")
                        .setArtifactId("common")
                        .setVersion("1.0.0")
                        .setResolvedPath(siblingJar)
                        .setFlags(DependencyFlags.DEPLOYMENT_CP))
                .setPlatformImports(PlatformImports.fromMap(Collections.emptyMap()))
                .build();

        ApplicationModelSerializer.serialize(model, modelFile, List.of(
                new ApplicationModelRelocation.Root(ApplicationModelRelocation.BUILD_DIR_ROOT, buildDir),
                new ApplicationModelRelocation.Root(ApplicationModelRelocation.PROJECT_DIR_ROOT, projectDir),
                new ApplicationModelRelocation.Root(ApplicationModelRelocation.ROOT_DIR_ROOT, rootDir)));

        // deserialize(Path) is what a forked JVM uses: roots derived from the file's location alone
        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(modelFile);

        assertThat(deserialized.getDependencies())
                .singleElement()
                .satisfies(d -> assertThat(d.getResolvedPaths().getSinglePath()).isEqualTo(siblingJar));
    }

    /**
     * Two definitions of one root name would tokenize by one and resolve by the other, silently
     * producing a path pointing somewhere else. There is no way to choose between them, so relocation
     * refuses rather than guesses.
     */
    @Test
    void conflictingDefinitionsOfARootAreRejected() {
        Path a = tempDir.resolve("a");
        Path b = tempDir.resolve("b");
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("p", a.resolve("foo.jar").toString());

        assertThatThrownBy(() -> ApplicationModelRelocation.relocate(model, List.of(
                new ApplicationModelRelocation.Root("quarkus.project.dir", a),
                new ApplicationModelRelocation.Root("quarkus.project.dir", b))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quarkus.project.dir");
    }

    /**
     * The same root named twice with the same location is redundant, not contradictory, and must be
     * accepted - {@code withEnvironmentRoots} relies on it being harmless.
     */
    @Test
    void repeatingARootWithTheSameLocationIsAccepted() {
        Path a = tempDir.resolve("a");
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("p", a.resolve("foo.jar").toString());

        Map<String, Object> relocated = ApplicationModelRelocation.relocate(model, List.of(
                new ApplicationModelRelocation.Root("quarkus.project.dir", a),
                new ApplicationModelRelocation.Root("quarkus.project.dir", a)));

        assertThat(relocated.get("p")).isEqualTo("${quarkus.project.dir}/foo.jar");
    }

    /**
     * A caller that knows a root better than the environment does replaces it, rather than adding a
     * second definition of the same name.
     */
    @Test
    void callerRootsReplaceTheEnvironmentOnesOfTheSameName() {
        Path gradleHome = tempDir.resolve("custom-gradle-home");

        List<ApplicationModelRelocation.Root> roots = ApplicationModelRelocation.withEnvironmentRoots(List.of(
                new ApplicationModelRelocation.Root(ApplicationModelRelocation.GRADLE_USER_HOME_ROOT, gradleHome)));

        assertThat(roots)
                .filteredOn(r -> r.name().equals(ApplicationModelRelocation.GRADLE_USER_HOME_ROOT))
                .singleElement()
                .satisfies(r -> assertThat(r.path()).isEqualTo(gradleHome));
        // the environment roots it does not override are still there
        assertThat(roots).anySatisfy(r -> assertThat(r.name()).isEqualTo(ApplicationModelRelocation.LOCAL_REPO_ROOT));
    }

    /**
     * An included build lies outside the root directory of the build including it, so its artifacts
     * need a root of their own or they stay absolute and keep the model checkout-dependent.
     */
    @Test
    void includedBuildArtifactsAreRelocated() throws IOException {
        Path rootDir = tempDir.resolve("main-build");
        Path includedBuild = tempDir.resolve("shared-lib");
        Path jar = includedBuild.resolve("lib/build/libs/lib.jar");
        Path file = tempDir.resolve("included.dat");

        ApplicationModel model = new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("com.example")
                        .setArtifactId("app")
                        .setVersion("1.0.0")
                        .setResolvedPath(rootDir.resolve("build/classes")))
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("com.example")
                        .setArtifactId("lib")
                        .setVersion("1.0.0")
                        .setResolvedPath(jar)
                        .setFlags(DependencyFlags.DEPLOYMENT_CP))
                .setPlatformImports(PlatformImports.fromMap(Collections.emptyMap()))
                .build();

        List<ApplicationModelRelocation.Root> roots = List.of(
                new ApplicationModelRelocation.Root(ApplicationModelRelocation.ROOT_DIR_ROOT, rootDir),
                new ApplicationModelRelocation.Root(ApplicationModelRelocation.includedBuildRoot(0), includedBuild));
        ApplicationModelSerializer.serialize(model, file, roots);

        assertThat(Files.readString(file)).doesNotContain(asJson(includedBuild));

        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(file, roots);
        assertThat(deserialized.getDependencies())
                .singleElement()
                .satisfies(d -> assertThat(d.getResolvedPaths().getSinglePath()).isEqualTo(jar));
    }

    private String serializeAt(Path checkout) throws IOException {
        Path file = tempDir.resolve(checkout.getFileName() + ".dat");
        ApplicationModelSerializer.serialize(modelAt(checkout), file, roots(checkout));
        return Files.readString(file);
    }

    /**
     * A path as it appears inside the serialized JSON. On Windows the separators are escaped, so a raw
     * {@link Path#toString()} never occurs verbatim in the file and asserting on one would pass or fail
     * for the wrong reason.
     */
    private static String asJson(Path path) {
        return path.toString().replace("\\", "\\\\");
    }

    private static List<ApplicationModelRelocation.Root> roots(Path projectDir) {
        return List.of(new ApplicationModelRelocation.Root("quarkus.project.dir", projectDir));
    }

    private static ApplicationModel modelAt(Path projectDir) {
        return new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("com.example")
                        .setArtifactId("my-app")
                        .setVersion("1.0.0")
                        .setResolvedPath(projectDir.resolve("target/classes")))
                .setPlatformImports(PlatformImports.fromMap(Collections.emptyMap()))
                .build();
    }
}
