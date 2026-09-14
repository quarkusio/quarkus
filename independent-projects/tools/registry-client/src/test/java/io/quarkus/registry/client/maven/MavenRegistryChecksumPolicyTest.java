package io.quarkus.registry.client.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/54974
 * <p>
 * Extension registries publish their catalogs as constantly re-deployed Maven snapshots. A client fetching
 * {@code maven-metadata.xml} and its checksum while the registry is re-publishing will read the two from different
 * generations of the metadata and report a checksum mismatch, even though the download itself is fine. Registry
 * artifacts are therefore resolved with checksum validation disabled.
 */
public class MavenRegistryChecksumPolicyTest {

    private static final String REGISTRY_ID = "acme-registry";
    private static final String SNAPSHOT_VERSION = "1.0-SNAPSHOT";
    private static final String TIMESTAMPED_VERSION = "1.0-20260101.120000-1";
    private static final String CORRUPTED_SHA1 = "0000000000000000000000000000000000000000";

    @Test
    public void registryArtifactsAreResolvedWithoutChecksumValidation(@TempDir Path testDir) throws Exception {

        final Path remoteRepoDir = testDir.resolve("remote-repo");
        final Path localRepoDir = testDir.resolve("local-repo");
        publishSnapshotWithCorruptedChecksums(remoteRepoDir);

        final RemoteRepository registryRepo = new RemoteRepository.Builder(REGISTRY_ID, "default",
                remoteRepoDir.toUri().toURL().toExternalForm()).build();

        final MavenArtifactResolver registryResolver = MavenRegistryClientFactory.newResolver(
                newOriginalResolver(testDir, localRepoDir, registryRepo), List.of(registryRepo), newRegistryConfig(),
                MessageWriter.info());

        final ArtifactResult result = registryResolver
                .resolve(new DefaultArtifact("org.acme", "acme-app", "jar", SNAPSHOT_VERSION));
        assertThat(result.getArtifact().getFile()).exists();

        // with checksum validation enabled, the resolver downloads the checksums next to the files it validates
        final Path localSnapshotDir = localRepoDir.resolve("org/acme/acme-app").resolve(SNAPSHOT_VERSION);
        assertThat(localSnapshotDir.resolve("maven-metadata-" + REGISTRY_ID + ".xml.sha1")).doesNotExist();
        assertThat(localSnapshotDir.resolve("acme-app-" + TIMESTAMPED_VERSION + ".jar.sha1")).doesNotExist();
    }

    private static MavenArtifactResolver newOriginalResolver(Path testDir, Path localRepoDir, RemoteRepository registryRepo)
            throws Exception {
        // an empty settings.xml, so that the mirrors and proxies of the user running the test don't affect the outcome
        final Path settingsXml = testDir.resolve("settings.xml");
        Files.writeString(settingsXml, "<settings/>");
        return MavenArtifactResolver.builder()
                .setUserSettings(settingsXml.toFile())
                .setLocalRepository(localRepoDir.toString())
                .setRemoteRepositories(List.of(registryRepo))
                .setWorkspaceDiscovery(false)
                .build();
    }

    private static RegistryConfig newRegistryConfig() {
        return RegistryConfig.builder()
                .setId(REGISTRY_ID)
                .setDescriptor(RegistryDescriptorConfig.builder()
                        .setArtifact(ArtifactCoords.fromString("org.acme:acme-registry-descriptor::json:" + SNAPSHOT_VERSION))
                        .build())
                .build();
    }

    private static void publishSnapshotWithCorruptedChecksums(Path repoDir) throws IOException {
        final Path snapshotDir = repoDir.resolve("org/acme/acme-app").resolve(SNAPSHOT_VERSION);
        Files.createDirectories(snapshotDir);

        writeWithCorruptedChecksum(snapshotDir.resolve("maven-metadata.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                  <groupId>org.acme</groupId>
                  <artifactId>acme-app</artifactId>
                  <version>%s</version>
                  <versioning>
                    <snapshot>
                      <timestamp>20260101.120000</timestamp>
                      <buildNumber>1</buildNumber>
                    </snapshot>
                    <lastUpdated>20260101120000</lastUpdated>
                    <snapshotVersions>
                      <snapshotVersion>
                        <extension>jar</extension>
                        <value>%s</value>
                        <updated>20260101120000</updated>
                      </snapshotVersion>
                    </snapshotVersions>
                  </versioning>
                </metadata>
                """.formatted(SNAPSHOT_VERSION, TIMESTAMPED_VERSION));

        writeWithCorruptedChecksum(snapshotDir.resolve("acme-app-" + TIMESTAMPED_VERSION + ".jar"), "acme-app");
    }

    private static void writeWithCorruptedChecksum(Path file, String content) throws IOException {
        Files.writeString(file, content);
        Files.writeString(file.resolveSibling(file.getFileName() + ".sha1"), CORRUPTED_SHA1);
    }
}
