package io.quarkus.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.Constants;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;

/**
 * List updates available for the project
 */
@Mojo(name = "list-updates", requiresProject = false)
public class ListUpdatesMojo extends QuarkusProjectMojoBase {

    /**
     * List the already installed extensions
     */
    @Parameter(property = "installed", defaultValue = "false")
    protected boolean installed;

    @Override
    public void doExecute(final QuarkusProject quarkusProject, final MessageWriter log) throws MojoExecutionException {

        final PlatformCatalog catalog;
        try {
            catalog = getExtensionCatalogResolver().resolvePlatformCatalog();
        } catch (RegistryResolutionException e) {
            throw new MojoExecutionException("Failed to resolve the Quarkus platform catalog", e);
        }
        final List<Platform> platforms = catalog == null ? Collections.emptyList() : catalog.getPlatforms();
        if (platforms.isEmpty()) {
            return;
        }
        List<ArtifactCoords> latestList = new ArrayList<>(platforms.size());
        for (Platform p : platforms) {
            final ArtifactCoords bom = p.getBom();
            latestList.add(bom);
        }

        final List<ArtifactCoords> platformJsons = getImportedPlatforms();
        final List<ArtifactCoords> importedPlatformBoms = new ArrayList<>(platformJsons.size());
        for (ArtifactCoords descriptor : platformJsons) {
            final ArtifactCoords importedBom = new ArtifactCoords(descriptor.getGroupId(),
                    descriptor.getArtifactId().substring(0,
                            descriptor.getArtifactId().length() - Constants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX.length()),
                    null, "pom", descriptor.getVersion());
            if (!latestList.remove(importedBom)) {
                importedPlatformBoms.add(importedBom);
            }
        }

        final Map<ArtifactKey, String> latest = new HashMap<>(platforms.size());
        for (ArtifactCoords latestBom : latestList) {
            latest.put(latestBom.getKey(), latestBom.getVersion());
        }

        log.info("Available Quarkus platform updates:");
        final StringBuilder buf = new StringBuilder(0);
        for (ArtifactCoords importedBom : importedPlatformBoms) {
            final ArtifactKey key = importedBom.getKey();
            final String update = latest.get(key);
            if (update == null || importedBom.getVersion().equals(update)) {
                continue;
            }
            buf.setLength(0);
            buf.append(key.getGroupId()).append(':').append(key.getArtifactId());
            for (int i = buf.length(); i < 45; ++i) {
                buf.append(' ');
            }
            buf.append(importedBom.getVersion());
            for (int i = buf.length(); i < 60; ++i) {
                buf.append(' ');
            }
            buf.append(" -> ").append(update);
            log.info(buf.toString());
        }

        if (buf.length() == 0) {
            log.info("No updates yet, ping @gsmet");
        }
    }
}
