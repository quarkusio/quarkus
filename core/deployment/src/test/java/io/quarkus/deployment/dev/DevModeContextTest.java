package io.quarkus.deployment.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

class DevModeContextTest {

    @TempDir
    Path directory;

    @Test
    void buildUpdateSourceDefaultsToQuarkus() {
        assertThat(new DevModeContext().getBuildUpdateSource()).isEqualTo(DevModeContext.BuildUpdateSource.QUARKUS);
    }

    @Test
    void buildUpdateSourceCanBeSetToExternalBuildTool() {
        var context = new DevModeContext();

        context.setBuildUpdateSource(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);

        assertThat(context.getBuildUpdateSource()).isEqualTo(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);
    }

    @Test
    void buildUpdateSourceSurvivesSerialization() throws Exception {
        var context = new DevModeContext();
        context.setBuildUpdateSource(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);

        var copy = serializeAndDeserialize(context);

        assertThat(copy.getBuildUpdateSource()).isEqualTo(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);
    }

    @Test
    void buildUpdateSourceTreatsNullAsQuarkus() {
        var context = new DevModeContext();

        context.setBuildUpdateSource(null);

        assertThat(context.getBuildUpdateSource()).isEqualTo(DevModeContext.BuildUpdateSource.QUARKUS);
    }

    @Test
    void externalBuildOutputTransportDefaultsToDisabled() {
        var transport = new DevModeContext().getExternalBuildOutputTransport();

        assertThat(transport.isEnabled()).isFalse();
        assertThat(transport.getUri()).isEmpty();
        assertThat(transport.getToken()).isEmpty();
    }

    @Test
    void externalBuildOutputTransportTreatsNullAsDisabled() {
        var context = new DevModeContext();

        context.setExternalBuildOutputTransport(null);

        assertThat(context.getExternalBuildOutputTransport().isEnabled()).isFalse();
        assertThat(context.getExternalBuildOutputTransport().getUri()).isEmpty();
    }

    @Test
    void externalBuildOutputTransportUriTreatsNullAsDisabled() {
        var transport = DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://127.0.0.1:12345"), "secret");

        transport.setUri(null);

        assertThat(transport.isEnabled()).isFalse();
        assertThat(transport.getUri()).isEmpty();
    }

    @Test
    void externalBuildOutputTransportCanBeSetToUri() {
        var context = new DevModeContext();

        context.setExternalBuildOutputTransport(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://127.0.0.1:12345"), "secret"));

        var transport = context.getExternalBuildOutputTransport();
        assertThat(transport.isEnabled()).isTrue();
        assertThat(transport.getUri()).contains(URI.create("tcp://127.0.0.1:12345"));
        assertThat(transport.getToken()).contains("secret");
    }

    @Test
    void externalBuildOutputTransportSurvivesSerialization() throws Exception {
        var context = new DevModeContext();
        context.setExternalBuildOutputTransport(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://127.0.0.1:12345"), "secret"));

        var copy = serializeAndDeserialize(context);

        var transport = copy.getExternalBuildOutputTransport();
        assertThat(transport.isEnabled()).isTrue();
        assertThat(transport.getUri()).contains(URI.create("tcp://127.0.0.1:12345"));
        assertThat(transport.getToken()).contains("secret");
    }

    @Test
    void singleAndMultipleClassOutputPathsRoundTrip() {
        Path singleMain = directory.resolve("single-main");
        Path firstMain = directory.resolve("main-java");
        Path secondMain = directory.resolve("main-kotlin");
        Path firstTest = directory.resolve("test-java");
        Path secondTest = directory.resolve("test-kotlin");

        var single = moduleBuilder().setClassesPath(singleMain.toString()).build();
        var multiple = moduleBuilder()
                .setClassesPaths(List.of(firstMain, secondMain))
                .setTestClassesPaths(List.of(firstTest, secondTest))
                .build();

        assertThat(single.getMain().getClassesPath()).isEqualTo(singleMain.toString());
        assertThat(single.getMain().getClassesPaths()).containsExactly(singleMain);
        assertThat(multiple.getMain().getClassesPaths()).containsExactly(firstMain, secondMain);
        assertThat(multiple.getTest().orElseThrow().getClassesPaths()).containsExactly(firstTest, secondTest);
    }

    @Test
    void externalTestModelAndMultipleClassPathsSurviveSerialization() throws Exception {
        Path firstMain = directory.resolve("main-java");
        Path secondMain = directory.resolve("main-kotlin");
        Path firstTest = directory.resolve("test-java");
        Path secondTest = directory.resolve("test-kotlin");
        var testModel = new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("app")
                        .setVersion("1")
                        .setResolvedPath(firstTest))
                .build();
        var context = new DevModeContext();
        context.setApplicationRoot(moduleBuilder()
                .setClassesPaths(List.of(firstMain, secondMain))
                .setTestClassesPaths(List.of(firstTest, secondTest))
                .build());
        context.setExternalTestApplicationModel(testModel);

        var copy = serializeAndDeserialize(context);

        assertThat(copy.getApplicationRoot().getMain().getClassesPaths()).containsExactly(firstMain, secondMain);
        assertThat(copy.getApplicationRoot().getTest().orElseThrow().getClassesPaths())
                .containsExactly(firstTest, secondTest);
        assertThat(copy.getExternalTestApplicationModel().getAppArtifact().getKey())
                .isEqualTo(testModel.getAppArtifact().getKey());
    }

    @Test
    void devModeApplicationBuildDirectoriesIncludeEveryClassOutput() throws Exception {
        Path firstMain = Files.createDirectories(directory.resolve("main-java"));
        Path secondMain = Files.createDirectories(directory.resolve("main-kotlin"));
        Path resources = Files.createDirectories(directory.resolve("resources"));
        var context = new DevModeContext();
        context.setApplicationRoot(moduleBuilder()
                .setClassesPaths(List.of(firstMain, secondMain))
                .setResourcesOutputPath(resources.toString())
                .build());

        assertThat(new DevModeMain(context).getApplicationBuildDirs())
                .containsExactly(firstMain, secondMain, resources);
    }

    @Test
    void devModeApplicationBuildDirectoriesDoNotDuplicateResourcesOutput() throws Exception {
        Path firstMain = Files.createDirectories(directory.resolve("main-java"));
        Path secondMainAndResources = Files.createDirectories(directory.resolve("main-kotlin"));
        var context = new DevModeContext();
        context.setApplicationRoot(moduleBuilder()
                .setClassesPaths(List.of(firstMain, secondMainAndResources))
                .setResourcesOutputPath(secondMainAndResources.toString())
                .build());

        assertThat(new DevModeMain(context).getApplicationBuildDirs())
                .containsExactly(firstMain, secondMainAndResources);
    }

    private DevModeContext.ModuleInfo.Builder moduleBuilder() {
        return new DevModeContext.ModuleInfo.Builder()
                .setArtifactKey(ArtifactKey.of("org.acme", "app"))
                .setProjectDirectory(directory.toString())
                .setSourcePaths(PathList.of())
                .setResourcePaths(PathList.of());
    }

    private static DevModeContext serializeAndDeserialize(DevModeContext context) throws Exception {
        var output = new ByteArrayOutputStream();
        try (var objectOutput = new ObjectOutputStream(output)) {
            objectOutput.writeObject(context);
        }
        try (var objectInput = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            return (DevModeContext) objectInput.readObject();
        }
    }
}
