package io.quarkus.cli.commands;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import java.io.IOException;
import java.util.Map;
import org.apache.maven.model.Dependency;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class ListExtensions {
    public static final String NAME = "list-extensions";
    private static final String PARAM_PREFIX = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME);
    public static final String ALL = ToolsUtils.dotJoin(PARAM_PREFIX, "all");
    public static final String FORMAT = ToolsUtils.dotJoin(PARAM_PREFIX, "format");
    public static final String SEARCH = ToolsUtils.dotJoin(PARAM_PREFIX, "search");

    private final QuarkusCommandInvocation invocation;
    private final ListExtensionsCommandHandler handler = new ListExtensionsCommandHandler();

    /**
     * @deprecated since 1.3.0.CR1
     *             Please use {@link #ListExtensions(BuildFile, QuarkusPlatformDescriptor)} instead.
     */
    @Deprecated
    public ListExtensions(final BuildFile buildFile) throws IOException {
        this(buildFile, QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor());
    }

    public ListExtensions(final BuildFile buildFile, QuarkusPlatformDescriptor platformDescr) throws IOException {
        this.invocation = new QuarkusCommandInvocation(platformDescr);
        if (buildFile != null) {
            invocation.setBuildFile(buildFile);
        }
    }

    public ListExtensions all(boolean all) {
        invocation.setValue(ALL, all);
        return this;
    }

    public ListExtensions format(String format) {
        invocation.setValue(FORMAT, format);
        return this;
    }

    public ListExtensions search(String search) {
        invocation.setValue(SEARCH, search);
        return this;
    }

    /**
     * @deprecated in 1.3.0.CR1
     *             Please use {@link #all(boolean)}, {@link #format(String)} and {@link #search(String)} respectively.
     */
    @Deprecated
    public void listExtensions(boolean all, String format, String search) throws IOException {
        all(all);
        format(format);
        search(search);
        try {
            execute();
        } catch (QuarkusCommandException e) {
            throw new IOException("Failed to list extensions", e);
        }
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return handler.execute(invocation);
    }

    public Map<String, Dependency> findInstalled() throws IOException {
        return handler.findInstalled(invocation);
    }
}
