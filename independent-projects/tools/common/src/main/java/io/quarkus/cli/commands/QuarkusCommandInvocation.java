package io.quarkus.cli.commands;

import static com.google.common.base.Preconditions.checkNotNull;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;
import java.util.HashMap;
import java.util.Map;

public class QuarkusCommandInvocation extends ValueMap<QuarkusCommandInvocation> {

    protected final QuarkusPlatformDescriptor platformDescr;
    protected final MessageWriter log;
    private final BuildFile buildFile;
    private final ProjectWriter writer;

    public QuarkusCommandInvocation(QuarkusPlatformDescriptor platformDescr, ProjectWriter writer,
            BuildFile buildFile) {
        this(new HashMap<>(), platformDescr, writer, buildFile, new DefaultMessageWriter());
    }

    public QuarkusCommandInvocation(Map<String, Object> values, QuarkusPlatformDescriptor platformDescr, ProjectWriter writer,
            BuildFile buildFile) {
        this(values, platformDescr, writer, buildFile, new DefaultMessageWriter());
    }

    public QuarkusCommandInvocation(Map<String, Object> values, QuarkusPlatformDescriptor platformDescr, ProjectWriter writer,
            BuildFile buildFile, MessageWriter log) {
        super(values);
        this.platformDescr = checkNotNull(platformDescr, "platformDescr is required");
        this.log = checkNotNull(log, "log is required");
        this.writer = checkNotNull(writer, "writer is required");
        this.buildFile = checkNotNull(buildFile, "buildFile is required");
    }

    public QuarkusCommandInvocation(QuarkusCommandInvocation original) {
        super(original.values);
        this.platformDescr = original.platformDescr;
        this.log = original.log;
        this.buildFile = original.buildFile;
        this.writer = original.writer;
    }

    public MessageWriter getMessageWriter() {
        return log;
    }

    public QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescr;
    }

    public BuildFile getBuildFile() {
        return buildFile;
    }

    public ProjectWriter getProjectWriter() {
        return writer;
    }

}
