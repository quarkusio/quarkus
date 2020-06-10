package io.quarkus.devtools.commands.data;

import static com.google.common.base.Preconditions.checkNotNull;

import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;
import java.util.HashMap;
import java.util.Map;

public final class QuarkusCommandInvocation extends ValueMap<QuarkusCommandInvocation> {

    private final QuarkusProject quarkusProject;
    private final MessageWriter log;

    public QuarkusCommandInvocation(QuarkusProject quarkusProject) {
        this(quarkusProject, new HashMap<>());
    }

    public QuarkusCommandInvocation(final QuarkusProject quarkusProject, final Map<String, Object> values) {
        this(quarkusProject, values, new DefaultMessageWriter());
    }

    public QuarkusCommandInvocation(final QuarkusProject quarkusProject, final Map<String, Object> values,
            final MessageWriter log) {
        super(values);
        this.quarkusProject = checkNotNull(quarkusProject, "quarkusProject is required");
        this.log = checkNotNull(log, "log is required");
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

    public QuarkusPlatformDescriptor getPlatformDescriptor() {
        return quarkusProject.getPlatformDescriptor();
    }

}
