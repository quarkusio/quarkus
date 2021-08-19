package io.quarkus.devtools.commands.handlers;

import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartCatalog;
import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartProjectInput;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.PomTransformer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;

/**
 * Instances of this class are thread-safe. They create a new project extracting all the necessary properties from an instance
 * of {@link QuarkusCommandInvocation}.
 */
public class CreateExtensionCommandHandler {

    final String artifactId;
    final String groupId;
    final QuarkusExtensionCodestartProjectInput input;
    final Path newExtensionDir;
    final Path extensionsParentDir;
    final Path itTestParentDir;
    final Path bomDir;

    public CreateExtensionCommandHandler(String groupId,
            String artifactId,
            QuarkusExtensionCodestartProjectInput input,
            Path newExtensionDir) {
        this(groupId, artifactId, input, newExtensionDir, null, null, null);
    }

    public CreateExtensionCommandHandler(String groupId,
            String artifactId,
            QuarkusExtensionCodestartProjectInput input,
            Path newExtensionDir,
            Path extensionsParentDir,
            Path itTestParentDir,
            Path bomDir) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.input = input;
        this.newExtensionDir = newExtensionDir;
        this.extensionsParentDir = extensionsParentDir;
        this.itTestParentDir = itTestParentDir;
        this.bomDir = bomDir;
    }

    public QuarkusCommandOutcome execute(MessageWriter log)
            throws QuarkusCommandException {
        try {
            final QuarkusExtensionCodestartCatalog catalog = QuarkusExtensionCodestartCatalog
                    .fromBaseCodestartsResources();
            catalog.createProject(input).generate(newExtensionDir);

            final String extensionDirName = newExtensionDir.getFileName().toString();
            if (extensionsParentDir != null) {
                updateExtensionsParentPom(extensionDirName, extensionsParentDir);
                log.info(MessageIcons.OK_ICON + " New extension module '%s' added to %s", extensionDirName,
                        extensionsParentDir);
            }

            if (itTestParentDir != null) {
                updateITParentPomAndMoveDir(extensionDirName, newExtensionDir, itTestParentDir);
                log.info(MessageIcons.OK_ICON + " New integration test module '%s' added to %s", extensionDirName,
                        itTestParentDir);
            }

            if (bomDir != null) {
                updateBom(groupId, artifactId, bomDir);
                log.info(MessageIcons.OK_ICON
                        + " The extension runtime and deployment artifacts have been added to the bom dependenciesManagement: %s",
                        bomDir);
            }

            log.info("\n-----------\n" + MessageIcons.NOOP_ICON +
                    " extension has been successfully generated in:\n--> "
                    + newExtensionDir.toString() + "\n-----------");

            return QuarkusCommandOutcome.success();
        } catch (IOException e) {
            throw new QuarkusCommandException("Error while creating Quarkus extension: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(input.getData());
    }

    private void updateBom(String groupId, String artifactId, Path bomDir) throws QuarkusCommandException {
        final Path bomPom = checkPomExists(bomDir);
        List<PomTransformer.Transformation> transformations = new ArrayList<>();
        transformations.add(PomTransformer.Transformation.addManagedDependency(groupId, artifactId,
                "${project.version}"));
        transformations.add(PomTransformer.Transformation.addManagedDependency(groupId, artifactId + "-deployment",
                "${project.version}"));
        new PomTransformer(bomPom, StandardCharsets.UTF_8).transform(transformations);
    }

    private void updateITParentPomAndMoveDir(String extensionDirName, Path newExtensionDir, Path itTestDir)
            throws QuarkusCommandException, IOException {
        final Path itTestPom = checkPomExists(itTestDir);
        new PomTransformer(itTestPom, StandardCharsets.UTF_8)
                .transform(PomTransformer.Transformation.addModule(extensionDirName));
        FileUtils.moveDirectory(newExtensionDir.resolve("integration-tests").toFile(),
                itTestDir.resolve(extensionDirName).toFile());
    }

    private void updateExtensionsParentPom(String extensionDirName, Path extensionsDir) throws QuarkusCommandException {
        final Path extensionsPom = checkPomExists(extensionsDir);
        new PomTransformer(extensionsPom, StandardCharsets.UTF_8)
                .transform(PomTransformer.Transformation.addModule(extensionDirName));
    }

    public static Path checkPomExists(Path dir) throws QuarkusCommandException {
        final Path pom = dir.resolve("pom.xml");
        if (!Files.exists(pom)) {
            throw new QuarkusCommandException("Invalid directory structure, file not found: " + pom.toString());
        }
        return pom;
    }

    public static Model readPom(Path dir) throws QuarkusCommandException {
        final Path pom = checkPomExists(dir);
        try {
            return MojoUtils.readPom(pom.toFile());
        } catch (IOException e) {
            throw new QuarkusCommandException("Error while reading pom: " + pom.toString(), e);
        }
    }
}
