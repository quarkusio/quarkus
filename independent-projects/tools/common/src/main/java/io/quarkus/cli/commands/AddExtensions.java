package io.quarkus.cli.commands;

import io.quarkus.cli.commands.project.QuarkusProject;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import java.util.Set;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class AddExtensions {

    public static final String NAME = "add-extensions";
    public static final String EXTENSIONS = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "extensions");
    public static final String OUTCOME_UPDATED = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "outcome", "updated");

    private final QuarkusCommandInvocation invocation;

    public AddExtensions(final QuarkusProject quarkusProject) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public AddExtensions extensions(Set<String> extensions) {
        invocation.setValue(EXTENSIONS, extensions);
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return new AddExtensionsCommandHandler().execute(invocation);
    }
}
