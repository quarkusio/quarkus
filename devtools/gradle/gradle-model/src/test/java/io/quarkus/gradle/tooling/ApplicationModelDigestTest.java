package io.quarkus.gradle.tooling;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

public class ApplicationModelDigestTest {

    @TempDir
    Path tempDir;

    @Test
    public void startsWithTheVersionHeader() {
        assertThat(ApplicationModelDigest.render(model(checkout("a"), "lib")))
                .startsWith(ApplicationModelDigest.HEADER + "\n");
    }

    @Test
    public void sameModelInAnotherCheckoutHasTheSameDigest() {
        Checkout first = checkout("first");
        Checkout second = checkout("second");

        String firstDigest = ApplicationModelDigest.render(model(first, "lib"));

        assertThat(ApplicationModelDigest.render(model(second, "lib"))).isEqualTo(firstDigest);
        assertThat(firstDigest)
                .doesNotContain(escaped(first.base.toString()))
                .contains("${artifact:org.acme:app::jar:1.0#0}")
                .contains("${artifact:org.acme:lib::jar:1.0#0}")
                .contains("${artifact:org.acme:ext::jar:1.0#0}")
                .contains("${module:org.acme:app:1.0}/build.gradle.kts");
    }

    @Test
    public void artifactOutsideTheCheckoutIsNamedAfterItsCoordinates() {
        // An artifact of an included build, or of a repository at any location
        Checkout checkout = checkout("a");
        Path elsewhere = tempDir.resolve("elsewhere/included.jar").toAbsolutePath();
        ApplicationModel model = builder(checkout, "lib")
                .addDependency(dependency("included", "1.0", elsewhere))
                .build();

        assertThat(ApplicationModelDigest.render(model))
                .contains("${artifact:org.acme:included::jar:1.0#0}")
                .doesNotContain(escaped(elsewhere.toString()));
    }

    @Test
    public void differentDependencyChangesTheDigest() {
        Checkout checkout = checkout("a");

        assertThat(ApplicationModelDigest.render(model(checkout, "other-lib")))
                .isNotEqualTo(ApplicationModelDigest.render(model(checkout, "lib")));
    }

    @Test
    public void differentVersionOfADependencyChangesTheDigest() {
        Checkout checkout = checkout("a");
        ApplicationModel model = builder(checkout, "lib")
                .addDependency(dependency("extra", "1.0", checkout.base.resolve("repo/extra.jar")))
                .build();
        ApplicationModel newerModel = builder(checkout, "lib")
                .addDependency(dependency("extra", "2.0", checkout.base.resolve("repo/extra.jar")))
                .build();

        assertThat(ApplicationModelDigest.render(newerModel)).isNotEqualTo(ApplicationModelDigest.render(model));
    }

    @Test
    public void classesDirectoryIsNamedAfterItsArtifactRatherThanItsModule() {
        assertThat(ApplicationModelDigest.render(model(checkout("a"), "lib")))
                .doesNotContain("${module-build:org.acme:app:1.0}/classes/java/main")
                .doesNotContain("${module:org.acme:app:1.0}/build/classes/java/main");
    }

    @Test
    public void pathTheModelDoesNotAttributeIsKeptAsIs() {
        List<Map.Entry<String, String>> locations = List.of(Map.entry("/ci/build", "${module:app}"));

        assertThat(ApplicationModelDigest.name("/somewhere/else", locations)).isEqualTo("/somewhere/else");
    }

    @Test
    public void locationOnlyMatchesWholePathSegments() {
        List<Map.Entry<String, String>> locations = List.of(Map.entry("/ci/build", "${module:app}"));

        assertThat(ApplicationModelDigest.name("/ci/build", locations)).isEqualTo("${module:app}");
        assertThat(ApplicationModelDigest.name("/ci/build/app", locations)).isEqualTo("${module:app}/app");
        assertThat(ApplicationModelDigest.name("/ci/build-2/app", locations)).isEqualTo("/ci/build-2/app");
        assertThat(ApplicationModelDigest.name("/ci/buildx", locations)).isEqualTo("/ci/buildx");
    }

    @Test
    public void windowsSeparatorsAreNormalizedUnderALocation() {
        List<Map.Entry<String, String>> locations = List.of(Map.entry("C:\\ci\\build", "${module:app}"));

        assertThat(ApplicationModelDigest.name("C:\\ci\\build\\app\\lib.jar", locations))
                .isEqualTo("${module:app}/app/lib.jar");
    }

    @Test
    public void writesTheRenderedDigest() throws Exception {
        ApplicationModel model = model(checkout("a"), "lib");
        Path file = tempDir.resolve("model.digest");

        ApplicationModelDigest.write(model, file);

        assertThat(Files.readString(file)).isEqualTo(ApplicationModelDigest.render(model));
    }

    private Checkout checkout(String name) {
        Path base = tempDir.resolve(name).toAbsolutePath();
        return new Checkout(base, base.resolve("project"), base.resolve("gradle-home"));
    }

    private static ApplicationModel model(Checkout checkout, String libName) {
        return builder(checkout, libName).build();
    }

    private static ApplicationModelBuilder builder(Checkout checkout, String libName) {
        Path appDir = checkout.rootProjectDir.resolve("app");
        WorkspaceModule.Mutable module = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "app", "1.0"))
                .setModuleDir(appDir)
                .setBuildDir(appDir.resolve("build"))
                .setBuildFile(appDir.resolve("build.gradle.kts"));
        ResolvedDependencyBuilder app = ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.acme").setArtifactId("app").setVersion("1.0")
                .setResolvedPaths(PathList.of(appDir.resolve("build/classes/java/main")))
                .setWorkspaceModule(module);
        return new ApplicationModelBuilder()
                .setAppArtifact(app)
                .setPlatformImports(new PlatformImportsImpl())
                .addDependency(dependency(libName, "1.0",
                        checkout.rootProjectDir.resolve(libName + "/build/libs/" + libName + "-1.0.jar")).setDirect(true))
                .addDependency(dependency("ext", "1.0", checkout.gradleUserHome
                        .resolve("caches/modules-2/files-2.1/org.acme/ext/1.0/ext-1.0.jar")));
    }

    private static ResolvedDependencyBuilder dependency(String artifactId, String version, Path path) {
        return ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.acme").setArtifactId(artifactId).setVersion(version)
                .setResolvedPath(path)
                .setRuntimeCp()
                .setDeploymentCp();
    }

    private static String escaped(String value) {
        return value.replace("\\", "\\\\");
    }

    private record Checkout(Path base, Path rootProjectDir, Path gradleUserHome) {
    }
}
