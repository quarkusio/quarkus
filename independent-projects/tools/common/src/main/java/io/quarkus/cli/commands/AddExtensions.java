package io.quarkus.cli.commands;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import java.io.IOException;
import java.util.Set;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class AddExtensions {

    public static final String NAME = "add-extensions";
    public static final String EXTENSIONS = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "extensions");
    public static final String OUTCOME_UPDATED = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "outcome", "updated");

    private final QuarkusCommandInvocation invocation;

    /**
     * @deprecated in 1.3.0.CR1
     *             Please use the variant that accepts {@link QuarkusPlatformDescriptor} as an argument.
     */
    @Deprecated
    public AddExtensions(ProjectWriter writer) throws IOException {
        this(writer, QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor());
    }

    /**
     * @deprecated in 1.3.0.CR1
     *             Please use the variant that accepts {@link QuarkusPlatformDescriptor} as an argument.
     */
    @Deprecated
    public AddExtensions(BuildFile buildFile) throws IOException {
        this(buildFile, QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor());
    }

    /**
     * @deprecated in 1.3.0.CR1
     *             Please use the variant that accepts {@link QuarkusPlatformDescriptor} as an argument.
     */
    @Deprecated
    public AddExtensions(final ProjectWriter writer, final BuildTool buildTool)
            throws IOException {
        this(writer, buildTool, QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor());
    }

    public AddExtensions(final ProjectWriter writer, QuarkusPlatformDescriptor platformDescr) throws IOException {
        this(new MavenBuildFile(writer), platformDescr);
    }

    public AddExtensions(final ProjectWriter writer, final BuildTool buildTool, QuarkusPlatformDescriptor platformDescr)
            throws IOException {
        this(buildTool.createBuildFile(writer), platformDescr);
    }

    public AddExtensions(final BuildFile buildFile, QuarkusPlatformDescriptor platformDescr) {
        invocation = new QuarkusCommandInvocation(platformDescr);
        invocation.setBuildFile(buildFile);
    }

    public AddExtensions extensions(Set<String> extensions) {
        invocation.setValue(EXTENSIONS, extensions);
        return this;
    }

    /**
     * @deprecated in 1.3.0.CR1
     *             Please call {@link #extensions(Set)} and then {@link #execute()}
     */
    @Deprecated
    public AddExtensionResult addExtensions(final Set<String> extensions) throws IOException {
        final QuarkusCommandOutcome outcome;
        try {
            outcome = extensions(extensions).execute();
        } catch (QuarkusCommandException e) {
            throw new IOException("Failed to list extensions", e);
        }
        return new AddExtensionResult(outcome.getValue(OUTCOME_UPDATED, false), outcome.isSuccess());

    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return new AddExtensionsCommandHandler().execute(invocation);
    }
}
