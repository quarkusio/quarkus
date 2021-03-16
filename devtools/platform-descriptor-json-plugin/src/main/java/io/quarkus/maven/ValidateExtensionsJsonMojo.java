package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;

/**
 * This goal validates a given JSON descriptor.
 * Specifically, it will make sure that all the extensions that are included in the BOM
 * the descriptor is referencing that are expected to be in the JSON descriptor are
 * actually present in the JSON descriptor. And that all the extensions that are found
 * in the JSON descriptor are also present in the BOM the descriptor is referencing.
 *
 */
@Mojo(name = "validate-extensions-json")
public class ValidateExtensionsJsonMojo extends AbstractMojo {

    @Parameter(property = "jsonGroupId", required = true)
    private String jsonGroupId;

    @Parameter(property = "jsonArtifactId", required = true)
    private String jsonArtifactId;

    @Parameter(property = "jsonVersion", required = true)
    private String jsonVersion;

    /**
     * Skip the execution of this mojo.
     *
     * @since 1.4.0
     */
    @Parameter(defaultValue = "false", property = "quarkus.validate-extensions-json.skip")
    private boolean skip;

    @Component
    private RepositorySystem repoSystem;

    @Component
    private RemoteRepositoryManager remoteRepoManager;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * Group ID's that we know don't contain extensions. This can speed up the process
     * by preventing the download of artifacts that are not required.
     */
    @Parameter
    private Set<String> ignoredGroupIds = new HashSet<>(0);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping as required by the mojo configuration");
            return;
        }

        MavenArtifactResolver mvn;
        try {
            mvn = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .setRemoteRepositoryManager(remoteRepoManager)
                    .build();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize maven artifact resolver", e);
        }

        final Artifact artifact = new DefaultArtifact(jsonGroupId, jsonArtifactId, jsonVersion, "json", jsonVersion);
        final Path jsonPath;
        try {
            jsonPath = mvn.resolve(artifact).getArtifact().getFile().toPath();
        } catch (BootstrapMavenException e) {
            throw new MojoExecutionException("Failed to resolve platform descriptor " + artifact, e);
        }

        JsonExtensionCatalog catalog;
        try {
            catalog = JsonCatalogMapperHelper.deserialize(jsonPath, JsonExtensionCatalog.class);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to deserialize extension catalog " + jsonPath, e);
        }
        final ArtifactCoords bomCoords = catalog.getBom();

        final DefaultArtifact bomArtifact = new DefaultArtifact(bomCoords.getGroupId(),
                bomCoords.getArtifactId(), bomCoords.getClassifier(), bomCoords.getType(), bomCoords.getVersion());
        final Map<String, Artifact> bomExtensions = collectBomExtensions(mvn, bomArtifact);

        List<Extension> missingFromBom = Collections.emptyList();
        for (Extension ext : catalog.getExtensions()) {
            if (bomExtensions.remove(ext.getArtifact().getGroupId() + ":" + ext.getArtifact().getArtifactId()) == null) {
                if (missingFromBom.isEmpty()) {
                    missingFromBom = new ArrayList<>();
                }
                missingFromBom.add(ext);
            }
        }

        if (bomExtensions.isEmpty() && missingFromBom.isEmpty()) {
            return;
        }

        if (!bomExtensions.isEmpty()) {
            getLog().error("Extensions from " + bomArtifact + " not present in " + jsonGroupId + ":" + jsonArtifactId + ":"
                    + jsonVersion);
            for (Map.Entry<String, Artifact> entry : bomExtensions.entrySet()) {
                getLog().error("- " + entry.getValue());
            }
        }
        if (!missingFromBom.isEmpty()) {
            getLog().error("Extensions from " + jsonGroupId + ":" + jsonArtifactId + ":" + jsonVersion + " missing from "
                    + bomArtifact);
            for (Extension e : missingFromBom) {
                getLog().error("- " + e.getArtifact());
            }
        }
        throw new MojoExecutionException("Extensions referenced in " + bomArtifact + " and included in " + jsonGroupId + ":"
                + jsonArtifactId + ":" + jsonVersion + " do not match expectations. See the errors logged above.");
    }

    private Map<String, Artifact> collectBomExtensions(MavenArtifactResolver mvn, final DefaultArtifact platformBom)
            throws MojoExecutionException {

        final List<Dependency> bomDeps;
        try {
            bomDeps = mvn.resolveDescriptor(platformBom).getManagedDependencies();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve platform BOM " + platformBom, e);
        }

        final Map<String, Artifact> bomExtensions = new HashMap<>(bomDeps.size());

        for (Dependency dep : bomDeps) {
            final Artifact artifact = dep.getArtifact();
            if (ignoredGroupIds.contains(artifact.getGroupId())
                    || !artifact.getExtension().equals("jar")
                    || "javadoc".equals(artifact.getClassifier())
                    || "tests".equals(artifact.getClassifier())
                    || "sources".equals(artifact.getClassifier())) {
                continue;
            }
            try {
                analyzeArtifact(mvn.resolve(artifact).getArtifact(), bomExtensions);
            } catch (Exception e) {
                getLog().warn("Failed to resolve " + artifact + " from managed dependencies of BOM " + platformBom);
            }
        }
        return bomExtensions;
    }

    private void analyzeArtifact(Artifact artifact, Map<String, Artifact> extensions) throws MojoExecutionException {
        final File file = artifact.getFile();
        if (!file.exists()) {
            throw new MojoExecutionException("Failed to locate " + artifact + " at " + file);
        }

        if (!doesDescriptorExistAndCanBeRead(artifact, extensions, file, BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME) &&
                !doesDescriptorExistAndCanBeRead(artifact, extensions, file,
                        BootstrapConstants.EXTENSION_PROPS_JSON_FILE_NAME)) {

            throw new MojoExecutionException("Failed to locate and read neither "
                    + BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME
                    + " or " + BootstrapConstants.EXTENSION_PROPS_JSON_FILE_NAME
                    + " for '" + artifact + "' in " + file);
        }
    }

    private boolean doesDescriptorExistAndCanBeRead(Artifact artifact, Map<String, Artifact> extensions, final File file,
            String descriptorName)
            throws MojoExecutionException {
        if (file.isDirectory()) {
            processExtensionDescriptor(artifact, file.toPath().resolve(BootstrapConstants.META_INF)
                    .resolve(descriptorName), extensions);
        } else {
            try (FileSystem fs = FileSystems.newFileSystem(file.toPath(), null)) {
                processExtensionDescriptor(artifact,
                        fs.getPath("/", BootstrapConstants.META_INF, descriptorName),
                        extensions);
            } catch (IOException e) {
                getLog().debug("Failed to read " + file, e);
                return false;
            }
        }
        return true;
    }

    private void processExtensionDescriptor(Artifact artifact, Path p, Map<String, Artifact> extensions) {
        if (Files.exists(p)) {
            extensions.put(artifact.getGroupId() + ":" + artifact.getArtifactId(), artifact);
        }
    }
}
