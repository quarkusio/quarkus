package io.quarkus.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.util.ZipUtils;

/**
 * This goal generates a list of extensions for a given BOM
 * and stores it in a JSON format file that is later used by the tools
 * as the catalog of available extensions.
 */
@Mojo(name = "generate-extensions-json")
public class GenerateExtensionsJsonMojo extends AbstractMojo {

    @Parameter(property = "bomGroupId", defaultValue = "${project.groupId}")
    private String bomGroupId;

    @Parameter(property = "bomArtifactId", defaultValue = "${project.artifactId}")
    private String bomArtifactId;

    @Parameter(property = "bomVersion", defaultValue = "${project.version}")
    private String bomVersion;

    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/extensions.json")
    private File outputFile;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Component
    private MavenProject project;
    @Component
    private MavenProjectHelper projectHelper;

    public GenerateExtensionsJsonMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final DefaultArtifact bomArtifact = new DefaultArtifact(bomGroupId, bomArtifactId, "", "pom", bomVersion);
        debug("Generating catalog of extensions for %s", bomArtifact);

        final List<Dependency> deps;
        try {
            deps = repoSystem
                    .readArtifactDescriptor(repoSession,
                            new ArtifactDescriptorRequest().setRepositories(repos).setArtifact(bomArtifact))
                    .getManagedDependencies();
        } catch (ArtifactDescriptorException e) {
            throw new MojoExecutionException("Failed to read descriptor of " + bomArtifact, e);
        }
        if (deps.isEmpty()) {
            getLog().warn("BOM " + bomArtifact + " does not include any dependency");
            return;
        }

        final JsonArrayBuilder extListJson = Json.createArrayBuilder();
        for (Dependency dep : deps) {
            final Artifact artifact = dep.getArtifact();
            if (!artifact.getExtension().equals("jar")
                    || "javadoc".equals(artifact.getClassifier())
                    || "tests".equals(artifact.getClassifier())
                    || "sources".equals(artifact.getClassifier())) {
                continue;
            }
            ArtifactResult resolved = null;
            try {
                resolved = repoSystem.resolveArtifact(repoSession,
                        new ArtifactRequest().setRepositories(repos).setArtifact(artifact));
                processDependency(resolved.getArtifact(), extListJson);
            } catch (ArtifactResolutionException e) {
                // there are some parent poms that appear as jars for some reason
                debug("Failed to resolve dependency %s defined in %s", artifact, bomArtifact);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to process dependency " + artifact, e);
            }
        }

        final JsonObjectBuilder platformJson = Json.createObjectBuilder();
        final JsonObjectBuilder bomJson = Json.createObjectBuilder();
        bomJson.add("groupId", bomGroupId);
        bomJson.add("artifactId", bomArtifactId);
        bomJson.add("version", bomVersion);
        platformJson.add("bom", bomJson.build());
        platformJson.add("extensions", extListJson.build());

        final File outputDir = outputFile.getParentFile();
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory " + outputDir);
            }
        }
        final JsonWriterFactory jsonWriterFactory = Json
                .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(new FileOutputStream(outputFile))) {
            jsonWriter.writeObject(platformJson.build());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to persist " + outputFile, e);
        }

        projectHelper.attachArtifact(project, "json", null, outputFile);
    }

    private void processDependency(Artifact artifact, JsonArrayBuilder extJsonBuilder) throws IOException {
        final Path path = artifact.getFile().toPath();
        if (Files.isDirectory(path)) {
            processMetaInfDir(artifact, path.resolve(BootstrapConstants.META_INF), extJsonBuilder);
        } else {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                processMetaInfDir(artifact, artifactFs.getPath(BootstrapConstants.META_INF), extJsonBuilder);
            }
        }
    }

    private boolean processMetaInfDir(Artifact artifact, Path metaInfDir, JsonArrayBuilder extJsonBuilder)
            throws IOException {
        if (!Files.exists(metaInfDir)) {
            return false;
        }
        final Path p = metaInfDir.resolve(BootstrapConstants.EXTENSION_PROPS_JSON_FILE_NAME);
        if (!Files.exists(p)) {
            return false;
        }
        processPlatformArtifact(artifact, p, extJsonBuilder);
        return true;
    }

    private void processPlatformArtifact(Artifact artifact, Path descriptor, JsonArrayBuilder extJsonBuilder)
            throws IOException {
        try (InputStream is = Files.newInputStream(descriptor)) {
            try (JsonReader reader = Json.createReader(is)) {
                final JsonObject object = reader.readObject();
                debug("Adding Quarkus extension %s:%s", object.get("groupId"), object.get("artifactId"));
                extJsonBuilder.add(object);
            }
        } catch (IOException e) {
            throw new IOException("Failed to parse " + descriptor, e);
        }
    }

    private void debug(String msg, Object... args) {
        if (!getLog().isDebugEnabled()) {
            return;
        }
        if (args.length == 0) {
            getLog().debug(msg);
            return;
        }
        getLog().debug(String.format(msg, args));
    }
}
