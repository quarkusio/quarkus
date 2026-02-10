package io.quarkus.bootstrap.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

class ApplicationModelSerializerTest {

    @TempDir
    Path tempDir;

    @Test
    void testBasicSerializationDeserialization() throws IOException {
        // Create a basic ApplicationModel
        ApplicationModel originalModel = createBasicApplicationModel();

        // Serialize to JSON
        Path serializedFile = tempDir.resolve("app-model.json");
        ApplicationModelSerializer.serialize(originalModel, serializedFile);

        // Verify file was created
        assertThat(serializedFile).exists();
        assertThat(Files.size(serializedFile)).isGreaterThan(0);

        // Deserialize
        ApplicationModel deserializedModel = ApplicationModelSerializer.deserialize(serializedFile);

        // Verify basic properties
        assertThat(deserializedModel).isNotNull();
        assertThat(deserializedModel.getAppArtifact()).isNotNull();
        assertThat(deserializedModel.getAppArtifact().getGroupId()).isEqualTo("com.example");
        assertThat(deserializedModel.getAppArtifact().getArtifactId()).isEqualTo("my-app");
        assertThat(deserializedModel.getAppArtifact().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void testSerializationWithDependencies() throws IOException {
        // Create ApplicationModel with dependencies
        ApplicationModel originalModel = createApplicationModelWithDependencies();

        // Serialize
        Path serializedFile = tempDir.resolve("app-model-with-deps.json");
        ApplicationModelSerializer.serialize(originalModel, serializedFile);

        // Deserialize
        ApplicationModel deserializedModel = ApplicationModelSerializer.deserialize(serializedFile);

        // Verify dependencies
        Collection<ResolvedDependency> dependencies = deserializedModel.getDependencies();
        assertThat(dependencies)
                .isNotNull()
                .hasSize(2);

        // Verify dependency details
        assertThat(dependencies)
                .anySatisfy(dep -> {
                    assertThat(dep.getGroupId()).isEqualTo("com.google.guava");
                    assertThat(dep.getArtifactId()).isEqualTo("guava");
                    assertThat(dep.getVersion()).isEqualTo("31.0-jre");
                })
                .anySatisfy(dep -> {
                    assertThat(dep.getGroupId()).isEqualTo("org.apache.commons");
                    assertThat(dep.getArtifactId()).isEqualTo("commons-lang3");
                    assertThat(dep.getVersion()).isEqualTo("3.12.0");
                });
    }

    @Test
    void testSerializationWithDirectDependencies() throws IOException {
        // Create direct dependencies
        Dependency junitDep = DependencyBuilder.newInstance()
                .setGroupId("org.junit.jupiter")
                .setArtifactId("junit-jupiter-api")
                .setVersion("5.9.0")
                .setScope("test")
                .build();

        Dependency mockitoDep = DependencyBuilder.newInstance()
                .setGroupId("org.mockito")
                .setArtifactId("mockito-core")
                .setVersion("4.8.0")
                .setScope("test")
                .build();

        // Create a dependency with direct dependencies
        ResolvedDependencyBuilder depBuilder = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("lib-with-deps")
                .setVersion("2.0.0")
                .setResolvedPath(tempDir.resolve("lib-with-deps-2.0.0.jar"))
                .setFlags(DependencyFlags.DEPLOYMENT_CP)
                .setDirectDependencies(List.of(junitDep, mockitoDep));

        // Create ApplicationModel
        ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("my-app")
                .setVersion("1.0.0")
                .setResolvedPath(tempDir.resolve("my-app-1.0.0.jar"));

        ApplicationModel originalModel = new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .addDependency(depBuilder)
                .setPlatformImports(PlatformImports.fromMap(Collections.emptyMap()))
                .build();

        // Serialize
        Path serializedFile = tempDir.resolve("app-model-with-direct-deps.json");
        ApplicationModelSerializer.serialize(originalModel, serializedFile);

        // Verify file contents include direct-deps
        String jsonContent = Files.readString(serializedFile);
        assertThat(jsonContent).contains("direct-deps");

        // Deserialize
        ApplicationModel deserializedModel = ApplicationModelSerializer.deserialize(serializedFile);

        // Verify the deserialized model has dependencies with direct dependencies
        Collection<ResolvedDependency> dependencies = deserializedModel.getDependencies();
        assertThat(dependencies)
                .isNotNull()
                .hasSize(1);

        // Find and verify the dependency with direct dependencies
        assertThat(dependencies)
                .anySatisfy(dep -> {
                    assertThat(dep.getGroupId()).isEqualTo("com.example");
                    assertThat(dep.getArtifactId()).isEqualTo("lib-with-deps");

                    Collection<Dependency> directDeps = dep.getDirectDependencies();
                    assertThat(directDeps)
                            .isNotNull()
                            .hasSize(2);

                    // Verify the direct dependencies
                    assertThat(directDeps)
                            .anySatisfy(directDep -> {
                                assertThat(directDep.getGroupId()).isEqualTo("org.junit.jupiter");
                                assertThat(directDep.getArtifactId()).isEqualTo("junit-jupiter-api");
                                assertThat(directDep.getVersion()).isEqualTo("5.9.0");
                                assertThat(directDep.getScope()).isEqualTo("test");
                            })
                            .anySatisfy(directDep -> {
                                assertThat(directDep.getGroupId()).isEqualTo("org.mockito");
                                assertThat(directDep.getArtifactId()).isEqualTo("mockito-core");
                                assertThat(directDep.getVersion()).isEqualTo("4.8.0");
                                assertThat(directDep.getScope()).isEqualTo("test");
                            });
                });
    }

    private ApplicationModel createBasicApplicationModel() {
        ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("my-app")
                .setVersion("1.0.0")
                .setResolvedPath(tempDir.resolve("my-app-1.0.0.jar"));

        return new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .setPlatformImports(PlatformImports.fromMap(Collections.emptyMap()))
                .build();
    }

    private ApplicationModel createApplicationModelWithDependencies() {
        ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("my-app")
                .setVersion("1.0.0")
                .setResolvedPath(tempDir.resolve("my-app-1.0.0.jar"));

        ResolvedDependencyBuilder guava = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.google.guava")
                .setArtifactId("guava")
                .setVersion("31.0-jre")
                .setResolvedPath(tempDir.resolve("guava-31.0-jre.jar"))
                .setFlags(DependencyFlags.DEPLOYMENT_CP);

        ResolvedDependencyBuilder commonsLang = ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.apache.commons")
                .setArtifactId("commons-lang3")
                .setVersion("3.12.0")
                .setResolvedPath(tempDir.resolve("commons-lang3-3.12.0.jar"))
                .setFlags(DependencyFlags.DEPLOYMENT_CP);

        return new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .addDependency(guava)
                .addDependency(commonsLang)
                .setPlatformImports(PlatformImports.fromMap(Collections.emptyMap()))
                .build();
    }

}
