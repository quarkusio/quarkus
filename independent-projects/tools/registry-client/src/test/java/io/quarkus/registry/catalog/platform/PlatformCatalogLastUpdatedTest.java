package io.quarkus.registry.catalog.platform;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.catalog.json.JsonPlatformRelease;
import io.quarkus.registry.catalog.json.JsonPlatformReleaseVersion;
import io.quarkus.registry.catalog.json.JsonPlatformStream;
import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryDescriptorConfig;
import io.quarkus.registry.config.json.JsonRegistryPlatformsConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
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

public class PlatformCatalogLastUpdatedTest {

    @Test
    void testLastUpdated() throws Exception {

        final Path registryWorkDir = Paths.get("target").resolve("test-registry").normalize().toAbsolutePath();
        Files.createDirectories(registryWorkDir);

        final Path registryRepoDir = registryWorkDir.resolve("repo");
        Files.createDirectories(registryRepoDir);

        final MavenArtifactResolver mvn = MavenArtifactResolver.builder().setWorkspaceDiscovery(false)
                .setLocalRepository(registryRepoDir.toString()).build();

        final JsonRegistriesConfig config = new JsonRegistriesConfig();

        configureRegistry("foo", config, registryWorkDir, mvn);
        final String fooTimestamp = "20210101010101";
        setLastUpdated("foo", fooTimestamp, registryRepoDir, mvn);

        PlatformCatalog platformCatalog = newCatalogResolver(config, mvn).resolvePlatformCatalog();
        assertThat(platformCatalog).isNotNull();
        assertThat(platformCatalog.getPlatforms()).hasSize(1);
        assertThat(platformCatalog.getPlatforms().iterator().next().getPlatformKey()).isEqualTo(toPlatformKey("foo"));
        assertThat(platformCatalog.getMetadata().get(Constants.LAST_UPDATED)).isEqualTo(fooTimestamp);

        configureRegistry("bar", config, registryWorkDir, mvn);
        final String barTimestamp = "20210101010102";
        setLastUpdated("bar", barTimestamp, registryRepoDir, mvn);

        platformCatalog = newCatalogResolver(config, mvn).resolvePlatformCatalog();
        assertThat(platformCatalog).isNotNull();
        assertThat(platformCatalog.getPlatforms()).hasSize(2);
        final Iterator<Platform> platforms = platformCatalog.getPlatforms().iterator();
        assertThat(platforms.next().getPlatformKey()).isEqualTo(toPlatformKey("foo"));
        assertThat(platforms.next().getPlatformKey()).isEqualTo(toPlatformKey("bar"));
        assertThat(platformCatalog.getMetadata().get(Constants.LAST_UPDATED)).isEqualTo(barTimestamp);
    }

    private ExtensionCatalogResolver newCatalogResolver(JsonRegistriesConfig config, MavenArtifactResolver mvn)
            throws Exception {
        return ExtensionCatalogResolver.builder()
                .config(config)
                .artifactResolver(mvn)
                .build();
    }

    private static void configureRegistry(String shortName, final JsonRegistriesConfig config,
            final Path registryWorkDir, final MavenArtifactResolver mvn) throws Exception {
        final JsonRegistryConfig registry = new JsonRegistryConfig();
        config.addRegistry(registry);
        registry.setId("registry." + shortName + ".org");

        final String groupId = toRegistryGroupId(shortName);
        final JsonRegistryDescriptorConfig descriptorConfig = new JsonRegistryDescriptorConfig();
        registry.setDescriptor(descriptorConfig);
        final ArtifactCoords descriptorCoords = ArtifactCoords
                .fromString(groupId + ":quarkus-registry-descriptor::json:1.0-SNAPSHOT");
        descriptorConfig.setArtifact(descriptorCoords);

        final JsonRegistryPlatformsConfig platformsConfig = new JsonRegistryPlatformsConfig();
        registry.setPlatforms(platformsConfig);
        ArtifactCoords platformsCoords = ArtifactCoords
                .fromString(groupId + ":quarkus-registry-platforms::json:1.0-SNAPSHOT");
        platformsConfig.setArtifact(platformsCoords);

        Path json = registryWorkDir.resolve(shortName + "-quarkus-registry-descriptor.json");
        RegistriesConfigMapperHelper.serialize(registry, json);
        Artifact a = new DefaultArtifact(descriptorCoords.getGroupId(), descriptorCoords.getArtifactId(),
                descriptorCoords.getClassifier(), descriptorCoords.getType(),
                descriptorCoords.getVersion());
        a = a.setFile(json.toFile());
        mvn.install(a);

        JsonPlatformCatalog platforms = new JsonPlatformCatalog();
        JsonPlatform platform = new JsonPlatform();
        platforms.addPlatform(platform);
        final String platformKey = toPlatformKey(shortName);
        platform.setPlatformKey(platformKey);
        JsonPlatformStream stream = new JsonPlatformStream();
        platform.addStream(stream);
        stream.setId("1.0");
        JsonPlatformRelease release = new JsonPlatformRelease();
        stream.addRelease(release);
        release.setVersion(JsonPlatformReleaseVersion.fromString("1.0.0"));
        release.setQuarkusCoreVersion("1.2.3");
        release.setMemberBoms(Collections
                .singletonList(ArtifactCoords.fromString(platformKey + ":" + shortName + "-quarkus-bom::pom:1.0.0")));

        json = registryWorkDir.resolve(shortName + "-quarkus-platforms.json");
        JsonCatalogMapperHelper.serialize(platforms, json);
        a = new DefaultArtifact(platformsCoords.getGroupId(), platformsCoords.getArtifactId(), platformsCoords.getClassifier(),
                platformsCoords.getType(),
                platformsCoords.getVersion());
        a = a.setFile(json.toFile());
        mvn.install(a);
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
