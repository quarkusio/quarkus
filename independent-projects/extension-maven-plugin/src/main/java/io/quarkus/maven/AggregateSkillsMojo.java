package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.devtools.utils.SkillComposer;

/**
 * Aggregates all extension skill files from the Quarkus source tree into a single JAR.
 * <p>
 * This goal walks the {@code extensionsDir} looking for
 * {@code {ext}/deployment/src/main/resources/META-INF/quarkus-skill.md} files.
 * For each one found, it reads the corresponding runtime extension metadata from
 * {@code {ext}/runtime/src/main/resources/META-INF/quarkus-extension.yaml},
 * composes them into a {@code SKILL.md} with YAML frontmatter per the
 * <a href="https://agentskills.io/specification">Agent Skills specification</a>,
 * and writes the result to {@code target/classes/META-INF/skills/{name}/SKILL.md}.
 * <p>
 * Extension developers only need to create a {@code quarkus-skill.md} file in their
 * deployment module's resources — no pom.xml configuration is required. This mojo
 * discovers all skill files automatically by scanning the source tree.
 */
@Mojo(name = "aggregate-skills", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class AggregateSkillsMojo extends AbstractMojo {

    private static final String DEPLOYMENT = "deployment";
    private static final String RUNTIME = "runtime";

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    /**
     * Root directory containing Quarkus extensions.
     * Defaults to {@code ../../extensions} relative to the project base directory
     * (i.e. from {@code devtools/extension-skills/}).
     */
    @Parameter(defaultValue = "${project.basedir}/../../extensions", property = "extensionsDir")
    private File extensionsDir;

    @Override
    public void execute() throws MojoExecutionException {
        final Path extRoot = extensionsDir.toPath().normalize();
        if (!Files.isDirectory(extRoot)) {
            getLog().warn("Extensions directory does not exist: " + extRoot);
            return;
        }

        final List<SkillEntry> entries = discoverSkillFiles(extRoot);
        if (entries.isEmpty()) {
            getLog().info("No extension skill files found");
            return;
        }

        int composed = 0;
        for (SkillEntry entry : entries) {
            try {
                composeAndWrite(entry);
                composed++;
            } catch (IOException e) {
                getLog().warn("Failed to compose skill for " + entry.skillFile + ": " + e.getMessage());
            }
        }
        getLog().info("Aggregated " + composed + " extension skills from " + extRoot);
    }

    private List<SkillEntry> discoverSkillFiles(Path extRoot) throws MojoExecutionException {
        final List<SkillEntry> entries = new ArrayList<>();

        try {
            Files.walkFileTree(extRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    // Match: **/deployment/src/main/resources/META-INF/quarkus-skill.md
                    if (file.getFileName().toString().equals("quarkus-skill.md")
                            && file.getParent() != null
                            && file.getParent().getFileName().toString().equals("META-INF")
                            && isDeploymentResourcePath(file)) {

                        Path runtimeYaml = resolveRuntimeMetadata(file);
                        if (runtimeYaml != null && Files.isRegularFile(runtimeYaml)) {
                            entries.add(new SkillEntry(file, runtimeYaml));
                        } else {
                            getLog().debug("No runtime quarkus-extension.yaml found for: " + file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip target directories for performance
                    if (dir.getFileName().toString().equals("target")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to scan extensions directory: " + extRoot, e);
        }

        return entries;
    }

    /**
     * Checks that the file is under a {@code deployment/src/main/resources/META-INF/} path.
     */
    private boolean isDeploymentResourcePath(Path file) {
        // Walk up from quarkus-skill.md: META-INF / resources / main / src / deployment
        Path current = file.getParent(); // META-INF
        if (current == null)
            return false;
        current = current.getParent(); // resources
        if (current == null || !current.getFileName().toString().equals("resources"))
            return false;
        current = current.getParent(); // main
        if (current == null || !current.getFileName().toString().equals("main"))
            return false;
        current = current.getParent(); // src
        if (current == null || !current.getFileName().toString().equals("src"))
            return false;
        current = current.getParent(); // deployment
        return current != null && current.getFileName().toString().equals(DEPLOYMENT);
    }

    /**
     * Resolves the runtime module's {@code quarkus-extension.yaml} relative to the
     * deployment module that contains the skill file.
     * <p>
     * First tries the conventional sibling {@code runtime} module. If not found,
     * scans other sibling directories for a {@code quarkus-extension.yaml} — this
     * handles extensions like Lambda (common-runtime) and other non-standard layouts.
     */
    private Path resolveRuntimeMetadata(Path skillFile) {
        // Navigate up to the deployment module directory
        // skillFile: .../extensions/{ext}/deployment/src/main/resources/META-INF/quarkus-skill.md
        Path deploymentDir = skillFile.getParent() // META-INF
                .getParent() // resources
                .getParent() // main
                .getParent() // src
                .getParent(); // deployment

        if (deploymentDir == null || !deploymentDir.getFileName().toString().equals(DEPLOYMENT)) {
            return null;
        }

        Path extensionDir = deploymentDir.getParent();
        if (extensionDir == null) {
            return null;
        }

        // Try conventional sibling "runtime" first
        Path runtimeYaml = extensionDir.resolve(RUNTIME)
                .resolve("src/main/resources")
                .resolve(SkillComposer.EXTENSION_METADATA_PATH);
        if (Files.isRegularFile(runtimeYaml)) {
            return runtimeYaml;
        }

        // Fall back: scan sibling directories for quarkus-extension.yaml
        // (handles extensions like Lambda with common-runtime, or other non-standard layouts)
        try (var siblings = Files.list(extensionDir)) {
            return siblings
                    .filter(Files::isDirectory)
                    .filter(d -> !d.getFileName().toString().equals(DEPLOYMENT))
                    .map(d -> d.resolve("src/main/resources").resolve(SkillComposer.EXTENSION_METADATA_PATH))
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            getLog().debug("Failed to scan siblings of " + extensionDir + ": " + e.getMessage());
            return null;
        }
    }

    private void composeAndWrite(SkillEntry entry) throws IOException {
        final String rawContent = Files.readString(entry.skillFile, StandardCharsets.UTF_8);

        final ObjectNode extMeta;
        try (InputStream is = Files.newInputStream(entry.runtimeYaml)) {
            extMeta = SkillComposer.parseExtensionMetadata(is);
        }

        // Derive skill name from the runtime module's pom.xml artifactId
        Path runtimeDir = entry.runtimeYaml.getParent() // META-INF
                .getParent() // resources
                .getParent() // main
                .getParent() // src
                .getParent(); // runtime
        String skillName = readArtifactIdFromPom(runtimeDir.resolve("pom.xml"));
        if (skillName == null) {
            // Fallback to directory name
            Path extensionDir = runtimeDir.getParent();
            skillName = extensionDir != null ? extensionDir.getFileName().toString() : "unknown";
        }

        final String composed = SkillComposer.compose(extMeta, rawContent, skillName);
        final Path outputPath = outputDirectory.toPath()
                .resolve(SkillComposer.outputSkillPath(skillName));
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, composed, StandardCharsets.UTF_8);

        getLog().debug("Composed skill: " + skillName + " from " + entry.skillFile);
    }

    /**
     * Reads the {@code <artifactId>} from a pom.xml file using the Maven model reader.
     */
    private String readArtifactIdFromPom(Path pomFile) {
        if (!Files.isRegularFile(pomFile)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(pomFile, StandardCharsets.UTF_8)) {
            // TODO: MavenXpp3Reader is Maven 3 specific; will need migration for Maven 4
            Model model = new MavenXpp3Reader().read(reader);
            return model.getArtifactId();
        } catch (IOException | XmlPullParserException e) {
            getLog().debug("Failed to read pom.xml: " + pomFile + ": " + e.getMessage());
            return null;
        }
    }

    private static class SkillEntry {
        final Path skillFile;
        final Path runtimeYaml;

        SkillEntry(Path skillFile, Path runtimeYaml) {
            this.skillFile = skillFile;
            this.runtimeYaml = runtimeYaml;
        }
    }
}
