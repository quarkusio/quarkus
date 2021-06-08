package io.quarkus.devtools.commands;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.AddExtensionsCommandHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import java.util.HashMap;
import java.util.Set;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class AddExtensions {

    public static final String EXTENSIONS = "quarkus.add-extensions.extensions";
    public static final String OUTCOME_UPDATED = "quarkus.add-extensions.outcome-updated";
    public static final String EXTENSION_MANAGER = "quarkus.add-extensions.extension-manager";

    private final QuarkusCommandInvocation invocation;
    private final AddExtensionsCommandHandler handler = new AddExtensionsCommandHandler();

    public AddExtensions(final QuarkusProject quarkusProject) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public AddExtensions(final QuarkusProject quarkusProject, final MessageWriter messageWriter) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject, new HashMap<>(), messageWriter);
    }

    public AddExtensions extensionManager(ExtensionManager extensionManager) {
        invocation.setValue(EXTENSION_MANAGER, requireNonNull(extensionManager, "extensionManager is required"));
        return this;
    }

    public AddExtensions extensions(Set<String> extensions) {
        invocation.setValue(EXTENSIONS, extensions);
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return handler.execute(invocation);
    }
}
