package io.quarkus.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import io.quarkus.platform.tools.ExtensionStructure;
import io.quarkus.platform.tools.ExtensionStructureChecker;
import io.quarkus.platform.tools.ExtensionStructureChecker.Violation;

/**
 * Warns when an extension does not follow the
 * <a href="https://github.com/quarkusio/quarkus/blob/main/adr/0009-extension-structure.adoc">extension structure
 * guidelines</a>.
 * <p>
 * Runs in the deployment module and checks its classes along with those of the runtime artifact it depends on.
 * Modules that are not the deployment module of an extension are skipped.
 */
@Mojo(name = "extension-structure", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ExtensionStructureMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    /**
     * Whether to skip the extension structure check.
     */
    @Parameter(defaultValue = "${skipExtensionStructureCheck}")
    boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping the extension structure check");
            return;
        }
        try {
            Optional<Artifact> runtime = findRuntimeArtifact();
            if (runtime.isEmpty()) {
                getLog().debug(project.getArtifactId() + " is not the deployment module of an extension");
                return;
            }
            check(runtime.get());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to check the structure of " + project.getArtifactId(), e);
        }
    }

    private Optional<Artifact> findRuntimeArtifact() throws IOException {
        Set<String> directDependencies = project.getDependencies().stream()
                .map(d -> d.getGroupId() + ":" + d.getArtifactId())
                .collect(Collectors.toSet());
        String self = project.getGroupId() + ":" + project.getArtifactId();
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getFile() != null
                    && directDependencies.contains(artifact.getGroupId() + ":" + artifact.getArtifactId())
                    && ExtensionStructureScanner.deploymentArtifactOf(artifact.getFile().toPath())
                            .filter(self::equals).isPresent()) {
                return Optional.of(artifact);
            }
        }
        return Optional.empty();
    }

    private void check(Artifact runtime) throws IOException {
        Path deploymentClasses = Path.of(project.getBuild().getOutputDirectory());
        ExtensionStructure structure = new ExtensionStructure(runtime.getGroupId(), runtime.getArtifactId(),
                project.getArtifactId(), ExtensionStructureScanner.scan(runtime.getFile().toPath()),
                Files.isDirectory(deploymentClasses) ? ExtensionStructureScanner.scan(deploymentClasses) : List.of());
        List<Violation> violations = ExtensionStructureChecker.check(structure);
        if (violations.isEmpty()) {
            return;
        }
        getLog().warn("The " + runtime.getArtifactId() + " extension does not follow the extension structure guidelines"
                + " (https://github.com/quarkusio/quarkus/blob/main/adr/0009-extension-structure.adoc):");
        for (Violation violation : violations) {
            getLog().warn("- " + violation.describe());
        }
    }
}
