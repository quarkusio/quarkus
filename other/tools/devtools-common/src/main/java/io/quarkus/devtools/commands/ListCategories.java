package io.quarkus.devtools.commands;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.ListCategoriesCommandHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import java.util.HashMap;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class ListCategories {

    public static final String BATCH_MODE = "quarkus.list-categories.batch-mode";
    public static final String FROM_CLI = "quarkus.list-categories.from-cli";
    public static final String FORMAT = "quarkus.list-categories.format";

    public static final String LIST_EXTENSIONS_HINT = "To list extensions in given category, use:\n%s";
    public static final String MORE_INFO_HINT = "To get more information, append `%s` to your command line.";

    private final QuarkusCommandInvocation invocation;
    private final ListCategoriesCommandHandler handler = new ListCategoriesCommandHandler();

    public ListCategories(final QuarkusProject quarkusProject) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public ListCategories(final QuarkusProject quarkusProject, final MessageWriter messageWriter) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject, new HashMap<>(), messageWriter);
    }

    public ListCategories fromCli(boolean cli) {
        invocation.setValue(FROM_CLI, cli);
        return this;
    }

    public ListCategories format(String format) {
        invocation.setValue(FORMAT, format);
        return this;
    }

    public ListCategories batchMode(boolean batchMode) {
        invocation.setValue(BATCH_MODE, batchMode);
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return handler.execute(invocation);
    }
}
