package io.quarkus.cli.commands;

import static io.quarkus.generators.ProjectGenerator.*;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.ProjectGeneratorRegistry;
import io.quarkus.generators.SourceType;
import io.quarkus.generators.rest.BasicRestProjectGenerator;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsUtils;
import java.io.IOException;
import java.util.Properties;

/**
 * Instances of this class are thread-safe. They create a new project extracting all the necessary properties from an instance
 * of {@link QuarkusCommandInvocation}.
 */
public class CreateProjectCommandHandler implements QuarkusCommand {

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final ProjectWriter projectWriter = new FileProjectWriter(
                invocation.getQuarkusProject().getProjectFolderPath().toFile());
        if (!projectWriter.init()) {
            return QuarkusCommandOutcome.failure();
        }

        final QuarkusPlatformDescriptor platformDescr = invocation.getPlatformDescriptor();
        invocation.setValue(BOM_GROUP_ID, platformDescr.getBomGroupId());
        invocation.setValue(BOM_ARTIFACT_ID, platformDescr.getBomArtifactId());
        invocation.setValue(QUARKUS_VERSION, platformDescr.getQuarkusVersion());
        invocation.setValue(BOM_VERSION, platformDescr.getBomVersion());

        final Properties quarkusProps = ToolsUtils.readQuarkusProperties(platformDescr);
        quarkusProps.forEach((k, v) -> invocation.setValue(k.toString().replace("-", "_"), v.toString()));

        try (BuildFile buildFile = invocation.getBuildFile()) {
            String className = invocation.getStringValue(CLASS_NAME);
            if (className != null) {
                className = invocation.getValue(SOURCE_TYPE, SourceType.JAVA).stripExtensionFrom(className);
                int idx = className.lastIndexOf('.');
                if (idx >= 0) {
                    String pkgName = invocation.getStringValue(PACKAGE_NAME);
                    if (pkgName == null) {
                        invocation.setValue(PACKAGE_NAME, className.substring(0, idx));
                    }
                    className = className.substring(idx + 1);
                }
                invocation.setValue(CLASS_NAME, className);
            }
            ProjectGeneratorRegistry.get(BasicRestProjectGenerator.NAME).generate(projectWriter, invocation);

            // call close at the end to save file
            buildFile.completeFile(invocation.getStringValue(PROJECT_GROUP_ID),
                    invocation.getStringValue(PROJECT_ARTIFACT_ID),
                    invocation.getStringValue(PROJECT_VERSION),
                    platformDescr, quarkusProps);
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create project", e);
        }
        return QuarkusCommandOutcome.success();
    }
}
