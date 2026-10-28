package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.settings.v4.SettingsStaxReader;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * Extracts top-level {@code <repositories>} and {@code <pluginRepositories>}
 * from settings.xml using Maven 4's {@link SettingsStaxReader}.
 *
 * <p>
 * Maven 4's SETTINGS/2.0.0 schema supports repositories at the top level
 * (outside of {@code <profiles>}). The Maven 3 settings parser ignores
 * these elements. This class bridges the gap by parsing the raw XML with
 * Maven 4's own reader.
 *
 * <p>
 * This class lives in the {@code maven4-resolver} module so it can compile
 * against Maven 4 API classes. At runtime under Maven 3 the class will fail
 * to load ({@link NoClassDefFoundError}), which callers must handle.
 */
public final class Maven4SettingsSupport {

    private Maven4SettingsSupport() {
    }

    /**
     * Parses the given settings file using Maven 4's StAX reader and adds
     * any top-level repositories to the provided list.
     *
     * @param repos the list to append repositories to
     * @param settingsFile the settings.xml file to parse (may be {@code null})
     * @param pluginRepos if {@code true}, extract {@code <pluginRepositories>};
     *        otherwise extract {@code <repositories>}
     */
    public static void addTopLevelRepos(List<RemoteRepository> repos, File settingsFile, boolean pluginRepos) {
        if (settingsFile == null || !settingsFile.exists()) {
            return;
        }
        final Settings settings;
        try (InputStream is = Files.newInputStream(settingsFile.toPath())) {
            settings = new SettingsStaxReader().read(is, false, null);
        } catch (IOException | XMLStreamException e) {
            // non-fatal: fall back to what the Maven 3 parser already provided
            return;
        }
        final List<Repository> repositories = pluginRepos
                ? settings.getPluginRepositories()
                : settings.getRepositories();
        if (repositories == null) {
            return;
        }
        for (Repository repo : repositories) {
            final RemoteRepository.Builder builder = new RemoteRepository.Builder(
                    repo.getId(),
                    repo.getLayout() != null ? repo.getLayout() : "default",
                    repo.getUrl());
            final org.apache.maven.api.settings.RepositoryPolicy releases = repo.getReleases();
            if (releases != null) {
                builder.setReleasePolicy(toAetherPolicy(releases));
            }
            final org.apache.maven.api.settings.RepositoryPolicy snapshots = repo.getSnapshots();
            if (snapshots != null) {
                builder.setSnapshotPolicy(toAetherPolicy(snapshots));
            }
            repos.add(builder.build());
        }
    }

    private static RepositoryPolicy toAetherPolicy(org.apache.maven.api.settings.RepositoryPolicy policy) {
        return new RepositoryPolicy(
                policy.isEnabled(),
                isBlank(policy.getUpdatePolicy()) ? RepositoryPolicy.UPDATE_POLICY_DAILY : policy.getUpdatePolicy(),
                isBlank(policy.getChecksumPolicy()) ? RepositoryPolicy.CHECKSUM_POLICY_WARN
                        : policy.getChecksumPolicy());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
