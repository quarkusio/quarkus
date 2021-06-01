package io.quarkus.devtools.commands.data;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.HashMap;
import java.util.Map;

public final class QuarkusCommandInvocation extends ValueMap<QuarkusCommandInvocation> {

    private final QuarkusProject quarkusProject;
    private final MessageWriter log;

    public QuarkusCommandInvocation(QuarkusProject quarkusProject) {
        this(quarkusProject, new HashMap<>());
    }

    public QuarkusCommandInvocation(final QuarkusProject quarkusProject, final Map<String, Object> values) {
        this(quarkusProject, values, quarkusProject.log());
    }

    public QuarkusCommandInvocation(final QuarkusProject quarkusProject, final Map<String, Object> values,
            final MessageWriter log) {
        super(values);
        this.quarkusProject = requireNonNull(quarkusProject, "quarkusProject is required");
        this.log = requireNonNull(log, "log is required");
    }

    public QuarkusCommandInvocation(QuarkusCommandInvocation original) {
        this(original.quarkusProject, original.values, original.log);
    }

    public QuarkusProject getQuarkusProject() {
        return quarkusProject;
    }

    public MessageWriter log() {
        return log;
    }

    public ExtensionCatalog getExtensionsCatalog() {
        return quarkusProject.getExtensionsCatalog();
    }
}
