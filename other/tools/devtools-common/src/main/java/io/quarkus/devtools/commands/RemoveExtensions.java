package io.quarkus.devtools.commands;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.RemoveExtensionsCommandHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import java.util.HashMap;
import java.util.Set;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class RemoveExtensions {

    public static final String EXTENSIONS = "quarkus.remove-extensions.extensions";
    public static final String OUTCOME_UPDATED = "quarkus.remove-extensions.outcome-updated";
    public static final String EXTENSION_MANAGER = "quarkus.remove-extensions.extension-manager";

    private final QuarkusCommandInvocation invocation;

    public RemoveExtensions(final QuarkusProject quarkusProject) {
        invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public RemoveExtensions(final QuarkusProject quarkusProject, final MessageWriter messageWriter) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject, new HashMap<>(), messageWriter);
    }

    public RemoveExtensions extensions(Set<String> extensions) {
        invocation.setValue(EXTENSIONS, extensions);
        return this;
    }

    public RemoveExtensions extensionManager(ExtensionManager extensionManager) {
        invocation.setValue(EXTENSION_MANAGER, requireNonNull(extensionManager, "extensionManager is required"));
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return new RemoveExtensionsCommandHandler().execute(invocation);
    }
}
