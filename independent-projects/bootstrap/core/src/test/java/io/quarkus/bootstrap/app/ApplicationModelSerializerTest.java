package io.quarkus.bootstrap.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.ExtensionDevModeConfig;
import io.quarkus.bootstrap.model.JvmOption;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.ArtifactKey;
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

    @Test
    void testSerializationWithExtensionDevModeConfig() throws IOException {
        ApplicationModelBuilder builder = createBasicApplicationModelBuilder();
        Properties props = new Properties();
        props.setProperty("dev-mode.jvm-option.std.enable-preview", "");
        props.setProperty("dev-mode.jvm-option.std.add-modules", "java.compiler");
        props.setProperty("dev-mode.jvm-option.xx.MaxDirectMemorySize", "256m");
        props.setProperty(BootstrapConstants.EXT_DEV_MODE_LOCK_JVM_OPTIONS, "MaxDirectMemorySize");
        builder.handleExtensionProperties(props, ArtifactKey.ga("io.quarkus", "quarkus-vertx"));

        ApplicationModel originalModel = builder.build();

        Path serializedFile = tempDir.resolve("app-model-ext-dev.json");
        ApplicationModelSerializer.serialize(originalModel, serializedFile);
        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(serializedFile);

        Collection<ExtensionDevModeConfig> configs = deserialized.getExtensionDevModeConfig();
        assertThat(configs).hasSize(1);

        ExtensionDevModeConfig config = configs.iterator().next();
        assertThat(config.getExtensionKey().getGroupId()).isEqualTo("io.quarkus");
        assertThat(config.getExtensionKey().getArtifactId()).isEqualTo("quarkus-vertx");

        assertThat(config.getJvmOptions().asCollection()).hasSize(3);
        assertJvmOption(config, "enable-preview", false);
        assertJvmOption(config, "add-modules", true, "java.compiler");
        assertJvmOption(config, "MaxDirectMemorySize", true, "256m");

        assertThat(config.getLockJvmOptions()).containsExactly("MaxDirectMemorySize");
    }

    @Test
    void testSerializationWithMultipleExtensionDevModeConfigs() throws IOException {
        ApplicationModelBuilder builder = createBasicApplicationModelBuilder();

        Properties vertxProps = new Properties();
        vertxProps.setProperty("dev-mode.jvm-option.std.enable-preview", "");
        builder.handleExtensionProperties(vertxProps, ArtifactKey.ga("io.quarkus", "quarkus-vertx"));

        Properties nettyProps = new Properties();
        nettyProps.setProperty("dev-mode.jvm-option.xx.MaxDirectMemorySize", "512m");
        nettyProps.setProperty(BootstrapConstants.EXT_DEV_MODE_LOCK_JVM_OPTIONS, "MaxDirectMemorySize,enable-preview");
        builder.handleExtensionProperties(nettyProps, ArtifactKey.ga("io.quarkus", "quarkus-netty"));

        ApplicationModel originalModel = builder.build();

        Path serializedFile = tempDir.resolve("app-model-multi-ext-dev.json");
        ApplicationModelSerializer.serialize(originalModel, serializedFile);
        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(serializedFile);

        Collection<ExtensionDevModeConfig> configs = deserialized.getExtensionDevModeConfig();
        assertThat(configs).hasSize(2);

        assertThat(configs).anySatisfy(c -> {
            assertThat(c.getExtensionKey().getArtifactId()).isEqualTo("quarkus-vertx");
            assertJvmOption(c, "enable-preview", false);
            assertThat(c.getLockJvmOptions()).isEmpty();
        });
        assertThat(configs).anySatisfy(c -> {
            assertThat(c.getExtensionKey().getArtifactId()).isEqualTo("quarkus-netty");
            assertJvmOption(c, "MaxDirectMemorySize", true, "512m");
            assertThat(c.getLockJvmOptions()).containsExactlyInAnyOrder("MaxDirectMemorySize", "enable-preview");
        });
    }

    @Test
    void testSerializationWithExtensionDevModeConfigLockOnly() throws IOException {
        ApplicationModelBuilder builder = createBasicApplicationModelBuilder();
        Properties props = new Properties();
        props.setProperty(BootstrapConstants.EXT_DEV_MODE_LOCK_JVM_OPTIONS, "enable-preview");
        builder.handleExtensionProperties(props, ArtifactKey.ga("io.quarkus", "quarkus-vertx"));

        ApplicationModel originalModel = builder.build();

        Path serializedFile = tempDir.resolve("app-model-ext-dev-lock-only.json");
        ApplicationModelSerializer.serialize(originalModel, serializedFile);
        ApplicationModel deserialized = ApplicationModelSerializer.deserialize(serializedFile);

        Collection<ExtensionDevModeConfig> configs = deserialized.getExtensionDevModeConfig();
        assertThat(configs).hasSize(1);

        ExtensionDevModeConfig config = configs.iterator().next();
        assertThat(config.getExtensionKey().getArtifactId()).isEqualTo("quarkus-vertx");
        assertThat(config.getJvmOptions()).isNull();
        assertThat(config.getLockJvmOptions()).containsExactly("enable-preview");
    }

    private static void assertJvmOption(ExtensionDevModeConfig config, String name, boolean hasValue, String... values) {
        JvmOption option = config.getJvmOptions().asCollection().stream()
                .filter(o -> o.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("JVM option '" + name + "' not found"));
        assertThat(option.hasValue()).isEqualTo(hasValue);
        if (hasValue) {
            assertThat(option.getValues()).containsExactlyInAnyOrder(values);
        }
    }

    private ApplicationModelBuilder createBasicApplicationModelBuilder() {
        ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId("com.example")
                .setArtifactId("my-app")
                .setVersion("1.0.0")
                .setResolvedPath(tempDir.resolve("my-app-1.0.0.jar"));
        return new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .setPlatformImports(PlatformImports.fromMap(Collections.emptyMap()));
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
