package io.quarkus.devtools.commands;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.ListExtensionsCommandHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import java.util.HashMap;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class ListExtensions {

    public static final String ALL = "quarkus.list-extensions.all";
    public static final String INSTALLED = "quarkus.list-extensions.installed";
    public static final String FROM_CLI = "quarkus.list-extensions.from-cli";
    public static final String FORMAT = "quarkus.list-extensions.format";
    public static final String SEARCH = "quarkus.list-extensions.search";
    public static final String CATEGORY = "quarkus.list-extensions.category";
    public static final String BATCH_MODE = "quarkus.list-extensions.batch-mode";
    public static final String EXTENSION_MANAGER = "quarkus.list-extensions.extension-manager";

    public static final String MORE_INFO_HINT = "To get more information, append `%s` to your command line.";
    public static final String FILTER_HINT = "To list only extensions from specific category, append " +
            "`%s` to your command line.";
    public static final String ADD_EXTENSION_HINT = "Add an extension to your project by adding the dependency to your " +
            "%s or use `%s`";

    private final QuarkusCommandInvocation invocation;
    private final ListExtensionsCommandHandler handler = new ListExtensionsCommandHandler();

    public ListExtensions(final QuarkusProject quarkusProject) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public ListExtensions(final QuarkusProject quarkusProject, final MessageWriter messageWriter) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject, new HashMap<>(), messageWriter);
    }

    public ListExtensions all(boolean all) {
        invocation.setValue(ALL, all);
        return this;
    }

    public ListExtensions installed(boolean installed) {
        invocation.setValue(INSTALLED, installed);
        return this;
    }

    public ListExtensions fromCli(boolean cli) {
        invocation.setValue(FROM_CLI, cli);
        return this;
    }

    public ListExtensions format(String format) {
        invocation.setValue(FORMAT, format);
        return this;
    }

    public ListExtensions extensionManager(ExtensionManager extensionManager) {
        invocation.setValue(EXTENSION_MANAGER, requireNonNull(extensionManager, "extensionManager is required"));
        return this;
    }

    public ListExtensions search(String search) {
        invocation.setValue(SEARCH, search);
        return this;
    }

    public ListExtensions category(String category) {
        invocation.setValue(CATEGORY, category);
        return this;
    }

    public ListExtensions batchMode(boolean batchMode) {
        invocation.setValue(BATCH_MODE, batchMode);
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return handler.execute(invocation);
    }
}
