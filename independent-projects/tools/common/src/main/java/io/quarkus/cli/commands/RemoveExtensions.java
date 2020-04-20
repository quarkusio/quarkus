package io.quarkus.cli.commands;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import java.io.IOException;
import java.util.Set;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class RemoveExtensions {

    public static final String NAME = "remove-extensions";
    public static final String EXTENSIONS = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "extensions");
    public static final String OUTCOME_UPDATED = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "outcome", "updated");

    private final QuarkusCommandInvocation invocation;

    public RemoveExtensions(final ProjectWriter writer, QuarkusPlatformDescriptor platformDescr) throws IOException {
        this(new MavenBuildFile(writer), platformDescr);
    }

    public RemoveExtensions(final ProjectWriter writer, final BuildTool buildTool, QuarkusPlatformDescriptor platformDescr)
            throws IOException {
        this(buildTool.createBuildFile(writer), platformDescr);
    }

    public RemoveExtensions(final BuildFile buildFile, QuarkusPlatformDescriptor platformDescr) {
        invocation = new QuarkusCommandInvocation(platformDescr);
        invocation.setBuildFile(buildFile);
    }

    public RemoveExtensions extensions(Set<String> extensions) {
        invocation.setValue(EXTENSIONS, extensions);
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return new RemoveExtensionsCommandHandler().execute(invocation);
    }
}
