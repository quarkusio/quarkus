package io.quarkus.maven;

import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.codestartLoadersBuilder;
import static org.fusesource.jansi.Ansi.ansi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.commands.CreateJBangProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.tools.maven.MojoMessageWriter;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;

@Mojo(name = "create-jbang", requiresProject = false)
public class CreateJBangMojo extends AbstractMojo {

    @Parameter(property = "noJBangWrapper", defaultValue = "false")
    private boolean noJBangWrapper;

    /**
     * Group ID of the target platform BOM
     */
    @Parameter(property = "platformGroupId", required = false)
    private String bomGroupId;

    /**
     * Artifact ID of the target platform BOM
     */
    @Parameter(property = "platformArtifactId", required = false)
    private String bomArtifactId;

    /**
     * Version of the target platform BOM
     */
    @Parameter(property = "platformVersion", required = false)
    private String bomVersion;

    @Parameter(property = "extensions")
    private Set<String> extensions;

    @Parameter(property = "outputDirectory", defaultValue = "${basedir}/jbang-with-quarkus")
    private File outputDirectory;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Component
    private RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepoManager;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Files.createDirectories(outputDirectory.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create directory " + outputDirectory, e);
        }

        File projectRoot = outputDirectory;
        final Path projectDirPath = projectRoot.toPath();

        final MavenArtifactResolver mvn;
        try {
            mvn = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .setRemoteRepositoryManager(remoteRepoManager)
                    .build();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
        }

        final MessageWriter log = new MojoMessageWriter(getLog());
        ExtensionCatalog catalog;
        try {
            catalog = CreateProjectMojo.resolveExtensionsCatalog(this, bomGroupId, bomArtifactId, bomVersion,
                    QuarkusProjectHelper.getCatalogResolver(mvn, log), mvn, log);
        } catch (RegistryResolutionException e) {
            throw new MojoExecutionException("Failed to resolve Quarkus extension catalog", e);
        }

        final List<ResourceLoader> codestartsResourceLoader = codestartLoadersBuilder()
                .catalog(catalog)
                .artifactResolver(mvn)
                .build();
        final CreateJBangProject createJBangProject = new CreateJBangProject(QuarkusProject.of(projectDirPath, catalog,
                codestartsResourceLoader, log, BuildTool.MAVEN))
                        .extensions(extensions)
                        .setValue("noJBangWrapper", noJBangWrapper);

        boolean success;

        try {
            success = createJBangProject.execute().isSuccess();
        } catch (QuarkusCommandException e) {
            throw new MojoExecutionException("Failed to generate JBang Quarkus project", e);
        }

        if (success) {
            getLog().info("");
            getLog().info("========================================================================");
            getLog().warn(ansi().a("Quarkus JBang project is an experimental feature.").toString());
            getLog().info("========================================================================");
            getLog().info("");
        } else {
            throw new MojoExecutionException(
                    "Failed to generate JBang Quarkus project");
        }
    }
}
