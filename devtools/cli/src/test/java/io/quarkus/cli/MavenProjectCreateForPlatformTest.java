package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import picocli.CommandLine;

public class MavenProjectCreateForPlatformTest extends RegistryClientBuilderTestBase {

    @BeforeAll
    static void configureRegistryAndMavenRepo() {
        TestRegistryClientBuilder.newInstance()
                .baseDir(registryConfigDir())
                .persistPlatformDescriptorsAsMavenArtifacts()
                .newRegistry("registry.acme.com")
                .newPlatform("com.acme.quarkus.platform")
                .newStream("9.0")
                .newRelease("9.0.0")
                .quarkusVersion("9.0.0.acme-1")
                .upstreamQuarkusVersion("9.0.0")
                .addCoreMember()
                .alignPluginsOnQuarkusVersion()
                .release()
                .newMember("acme-bom")
                .addExtension("io.quarkus.platform", "acme-quarkus-supersonic", "9.0.0.acme-1")
                .addExtension("io.quarkus.platform", "acme-quarkus-subatomic", "9.0.0.acme-1")
                .release().stream().platform()
                .newStream("8.0")
                .newRelease("8.0.0")
                .quarkusVersion("8.0.0.acme-1")
                .upstreamQuarkusVersion("8.0.0")
                .addCoreMember()
                .alignPluginsOnQuarkusVersion()
                .release()
                .newMember("acme-supersonic-bom")
                .addExtension("io.quarkus.platform", "acme-quarkus-supersonic", "8.0.0.acme-1")
                .registry()
                .clientBuilder()
                .newRegistry("registry.acme.org")
                .newPlatform("io.quarkus.platform")
                .newStream("9.0")
                .newRelease("9.0.0")
                .quarkusVersion("9.0.0")
                .addCoreMember()
                .alignPluginsOnQuarkusVersion()
                .release()
                .newMember("acme-bom").addExtension("acme-quarkus-supersonic").addExtension("acme-quarkus-subatomic")
                .release().stream().platform()
                .newStream("8.0")
                .newRelease("8.0.0")
                .quarkusVersion("8.0.0")
                .addCoreMember()
                .alignPluginsOnQuarkusVersion()
                .release()
                .newMember("acme-supersonic-bom").addExtension("acme-quarkus-supersonic").release()
                .newMember("acme-subatomic-bom").addExtension("acme-quarkus-subatomic")
                .registry()
                .newNonPlatformCatalog(getCurrentQuarkusVersion())
                .addExtension("org.acme", "acme-quarkiverse-extension", "1.0")
                .registry()
                .clientBuilder()
                .build();
    }

    @Test
    void testCreateForPlatformWithUpstream() throws Exception {

        final CliDriver.Result createResult = run(workDir(), "create", "app", "create-for-platform",
                "-P com.acme.quarkus.platform:acme-supersonic-bom:8.0.0", "-x supersonic,subatomic");
        createResult.echoSystemOut();
        createResult.echoSystemErr();
        assertThat(createResult.exitCode).isEqualTo(CommandLine.ExitCode.OK)
                .as(() -> "Expected OK return code." + createResult);
        assertThat(createResult.stdout).contains("SUCCESS")
                .as(() -> "Expected confirmation that the project has been created." + createResult);

        final Path projectDir = workDir().resolve("create-for-platform");
        assertThat(projectDir).exists();
        final Path pomXml = projectDir.resolve("pom.xml");
        assertThat(pomXml).exists();

        final Model model = ModelUtils.readModel(pomXml);
        final Properties pomProps = model.getProperties();
        assertThat(pomProps).containsEntry("quarkus-plugin.version", "8.0.0.acme-1");
        assertThat(pomProps).containsEntry("quarkus.platform.artifact-id", "quarkus-bom");
        assertThat(pomProps).containsEntry("quarkus.platform.group-id", "com.acme.quarkus.platform");
        assertThat(pomProps).containsEntry("quarkus.platform.version", "8.0.0");

        final DependencyManagement dm = model.getDependencyManagement();
        assertThat(dm).isNotNull();
        final List<Dependency> managed = dm.getDependencies();
        assertThat(managed).hasSize(3);

        Dependency bomImport = managed.get(0);
        assertThat(bomImport.getGroupId()).isEqualTo("${quarkus.platform.group-id}");
        assertThat(bomImport.getArtifactId()).isEqualTo("${quarkus.platform.artifact-id}");
        assertThat(bomImport.getVersion()).isEqualTo("${quarkus.platform.version}");
        assertThat(bomImport.getType()).isEqualTo("pom");
        assertThat(bomImport.getScope()).isEqualTo("import");

        bomImport = managed.get(1);
        assertThat(bomImport.getGroupId()).isEqualTo("${quarkus.platform.group-id}");
        assertThat(bomImport.getArtifactId()).isEqualTo("acme-supersonic-bom");
        assertThat(bomImport.getVersion()).isEqualTo("${quarkus.platform.version}");
        assertThat(bomImport.getType()).isEqualTo("pom");
        assertThat(bomImport.getScope()).isEqualTo("import");

        bomImport = managed.get(2);
        assertThat(bomImport.getGroupId()).isEqualTo("io.quarkus.platform");
        assertThat(bomImport.getArtifactId()).isEqualTo("acme-subatomic-bom");
        assertThat(bomImport.getVersion()).isEqualTo("8.0.0");
        assertThat(bomImport.getType()).isEqualTo("pom");
        assertThat(bomImport.getScope()).isEqualTo("import");

        final Set<ArtifactCoords> pomDeps = model.getDependencies().stream()
                .map(d -> ArtifactCoords.of(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion()))
                .collect(Collectors.toSet());
        assertThat(pomDeps).contains(ArtifactCoords.of("io.quarkus.platform", "acme-quarkus-subatomic",
                ArtifactCoords.DEFAULT_CLASSIFIER, ArtifactCoords.TYPE_JAR, null));
        assertThat(pomDeps).contains(ArtifactCoords.of("io.quarkus.platform", "acme-quarkus-supersonic",
                ArtifactCoords.DEFAULT_CLASSIFIER, ArtifactCoords.TYPE_JAR, null));
    }
}
