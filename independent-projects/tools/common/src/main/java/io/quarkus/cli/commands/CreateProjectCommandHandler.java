package io.quarkus.cli.commands;

import static io.quarkus.generators.ProjectGenerator.BOM_ARTIFACT_ID;
import static io.quarkus.generators.ProjectGenerator.BOM_GROUP_ID;
import static io.quarkus.generators.ProjectGenerator.BOM_VERSION;
import static io.quarkus.generators.ProjectGenerator.BUILD_FILE;
import static io.quarkus.generators.ProjectGenerator.CLASS_NAME;
import static io.quarkus.generators.ProjectGenerator.PACKAGE_NAME;
import static io.quarkus.generators.ProjectGenerator.PROJECT_ARTIFACT_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_VERSION;
import static io.quarkus.generators.ProjectGenerator.QUARKUS_VERSION;
import static io.quarkus.generators.ProjectGenerator.SOURCE_TYPE;

import io.quarkus.cli.commands.file.BuildFile;
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
        final ProjectWriter projectWriter = invocation.getProjectWriter();
        if (projectWriter == null) {
            throw new IllegalStateException("Project writer has not been provided");
        }
        if (!projectWriter.init()) {
            return QuarkusCommandOutcome.failure();
        }

        final QuarkusPlatformDescriptor platformDescr = invocation.getPlatformDescriptor();
        invocation.setProperty(BOM_GROUP_ID, platformDescr.getBomGroupId());
        invocation.setProperty(BOM_ARTIFACT_ID, platformDescr.getBomArtifactId());
        invocation.setProperty(QUARKUS_VERSION, platformDescr.getQuarkusVersion());
        invocation.setProperty(BOM_VERSION, platformDescr.getBomVersion());

        final Properties quarkusProps = ToolsUtils.readQuarkusProperties(platformDescr);
        quarkusProps.forEach((k, v) -> invocation.setProperty(k.toString().replace("-", "_"), v.toString()));

        try (BuildFile buildFile = invocation.getBuildFile()) {
            invocation.setValue(BUILD_FILE, buildFile);

            String className = invocation.getProperty(CLASS_NAME);
            if (className != null) {
                className = invocation.getValue(SOURCE_TYPE, SourceType.JAVA).stripExtensionFrom(className);
                int idx = className.lastIndexOf('.');
                if (idx >= 0) {
                    String pkgName = invocation.getProperty(PACKAGE_NAME);
                    if (pkgName == null) {
                        invocation.setProperty(PACKAGE_NAME, className.substring(0, idx));
                    }
                    className = className.substring(idx + 1);
                }
                invocation.setProperty(CLASS_NAME, className);
            }
            ProjectGeneratorRegistry.get(BasicRestProjectGenerator.NAME).generate(projectWriter, invocation);

            // call close at the end to save file
            buildFile.completeFile(invocation.getProperty(PROJECT_GROUP_ID),
                    invocation.getProperty(PROJECT_ARTIFACT_ID),
                    invocation.getProperty(PROJECT_VERSION),
                    platformDescr, quarkusProps);
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create project", e);
        }
        return QuarkusCommandOutcome.success();
    }
}
