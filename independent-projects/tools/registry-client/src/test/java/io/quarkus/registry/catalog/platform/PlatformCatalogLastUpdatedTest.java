package io.quarkus.registry.catalog.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata.Nature;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigMapperHelper;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import io.quarkus.registry.config.RegistryPlatformsConfig;

public class PlatformCatalogLastUpdatedTest {

    @Test
    void testLastUpdated() throws Exception {

        final Path registryWorkDir = Paths.get("target").resolve("test-registry").normalize().toAbsolutePath();
        Files.createDirectories(registryWorkDir);

        final Path registryRepoDir = registryWorkDir.resolve("repo");
        Files.createDirectories(registryRepoDir);

        final MavenArtifactResolver mvn = MavenArtifactResolver.builder().setWorkspaceDiscovery(false)
                .setLocalRepository(registryRepoDir.toString()).build();

        RegistriesConfig.Mutable registriesConfig = RegistriesConfig.builder();
        registriesConfig.addRegistry(configureRegistry("foo", registryWorkDir, mvn));

        final String fooTimestamp = "20210101010101";
        setLastUpdated("foo", fooTimestamp, registryRepoDir, mvn);

        PlatformCatalog platformCatalog = newCatalogResolver(registriesConfig.build(), mvn).resolvePlatformCatalog();
        assertThat(platformCatalog).isNotNull();
        assertThat(platformCatalog.getPlatforms()).hasSize(1);
        assertThat(platformCatalog.getPlatforms().iterator().next().getPlatformKey()).isEqualTo(toPlatformKey("foo"));
        assertThat(platformCatalog.getMetadata().get(Constants.LAST_UPDATED)).isEqualTo(fooTimestamp);

        registriesConfig.addRegistry(configureRegistry("bar", registryWorkDir, mvn));

        final String barTimestamp = "20210101010102";
        setLastUpdated("bar", barTimestamp, registryRepoDir, mvn);

        platformCatalog = newCatalogResolver(registriesConfig, mvn).resolvePlatformCatalog();
        assertThat(platformCatalog).isNotNull();
        assertThat(platformCatalog.getPlatforms()).hasSize(2);
        final Iterator<Platform> platforms = platformCatalog.getPlatforms().iterator();
        assertThat(platforms.next().getPlatformKey()).isEqualTo(toPlatformKey("foo"));
        assertThat(platforms.next().getPlatformKey()).isEqualTo(toPlatformKey("bar"));
        assertThat(platformCatalog.getMetadata().get(Constants.LAST_UPDATED)).isEqualTo(barTimestamp);
    }

    private ExtensionCatalogResolver newCatalogResolver(RegistriesConfig config, MavenArtifactResolver mvn)
            throws Exception {
        return ExtensionCatalogResolver.builder()
                .config(config)
                .artifactResolver(mvn)
                .build();
    }

    private static RegistryConfig configureRegistry(String shortName,
            final Path registryWorkDir, final MavenArtifactResolver mvn) throws Exception {
        final String groupId = toRegistryGroupId(shortName);
        final ArtifactCoords descriptorCoords = ArtifactCoords
                .fromString(groupId + ":quarkus-registry-descriptor::json:1.0-SNAPSHOT");
        final ArtifactCoords platformsCoords = ArtifactCoords
                .fromString(groupId + ":quarkus-registry-platforms::json:1.0-SNAPSHOT");

        RegistryConfig registry = RegistryConfig.builder()
                .setId("registry." + shortName + ".org")
                .setDescriptor(RegistryDescriptorConfig.builder()
                        .setArtifact(descriptorCoords)
                        .build())
                .setPlatforms(RegistryPlatformsConfig.builder()
                        .setArtifact(platformsCoords)
                        .build())
                .build();

        Path json = registryWorkDir.resolve(shortName + "-quarkus-registry-descriptor.json");
        RegistriesConfigMapperHelper.serialize(registry, json);

        Artifact a = new DefaultArtifact(descriptorCoords.getGroupId(), descriptorCoords.getArtifactId(),
                descriptorCoords.getClassifier(), descriptorCoords.getType(),
                descriptorCoords.getVersion());
        a = a.setFile(json.toFile());
        mvn.install(a);

        final String platformKey = toPlatformKey(shortName);
        PlatformCatalog platforms = PlatformCatalog.builder()
                .addPlatform(Platform.builder()
                        .setPlatformKey(platformKey)
                        .addStream(PlatformStream.builder()
                                .setId("1.0")
                                .addRelease(PlatformRelease.builder()
                                        .setVersion(PlatformReleaseVersion.fromString("1.0.0"))
                                        .setQuarkusCoreVersion("1.2.3")
                                        .setMemberBoms(Collections
                                                .singletonList(ArtifactCoords
                                                        .fromString(platformKey + ":" + shortName + "-quarkus-bom::pom:1.0.0")))
                                        .build())
                                .build())
                        .build())
                .build();

        json = registryWorkDir.resolve(shortName + "-quarkus-platforms.json");
        CatalogMapperHelper.serialize(platforms, json);
        a = new DefaultArtifact(platformsCoords.getGroupId(), platformsCoords.getArtifactId(), platformsCoords.getClassifier(),
                platformsCoords.getType(),
                platformsCoords.getVersion());
        a = a.setFile(json.toFile());
        mvn.install(a);

        return registry;
    }

    private static void setLastUpdated(final String shortName, final String timestamp, final Path registryRepoDir,
            final MavenArtifactResolver mvn) throws Exception {
        final Path mdXml = registryRepoDir.resolve(mvn.getSession().getLocalRepositoryManager()
                .getPathForLocalMetadata(new DefaultMetadata(toRegistryGroupId(shortName), "quarkus-registry-platforms",
                        "1.0-SNAPSHOT", "maven-metadata.xml", Nature.SNAPSHOT)));
        if (!Files.exists(mdXml)) {
            assertThat(mdXml).exists();
        }
        final MetadataXpp3Reader mdReader = new MetadataXpp3Reader();
        final Metadata md;
        try (Reader reader = Files.newBufferedReader(mdXml)) {
            md = mdReader.read(reader);
            final Versioning versioning = md.getVersioning();
            assertThat(versioning).isNotNull();
            versioning.setLastUpdated(timestamp);
            for (SnapshotVersion sv : versioning.getSnapshotVersions()) {
                sv.setUpdated(timestamp);
            }
        }
        final MetadataXpp3Writer mdWriter = new MetadataXpp3Writer();
        try (Writer writer = Files.newBufferedWriter(mdXml)) {
            mdWriter.write(writer, md);
        }
    }

    private static String toPlatformKey(String shortName) {
        return "org." + shortName + ".platform";
    }

    private static String toRegistryGroupId(String shortName) {
        return "org." + shortName + ".registry";
    }
}
