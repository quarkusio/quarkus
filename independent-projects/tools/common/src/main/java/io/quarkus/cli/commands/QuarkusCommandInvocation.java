package io.quarkus.cli.commands;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class QuarkusCommandInvocation extends ValueMap<QuarkusCommandInvocation> {

    protected final QuarkusPlatformDescriptor platformDescr;
    protected final MessageWriter log;
    protected final Properties props;
    private BuildTool buildTool;
    private BuildFile buildFile;
    private ProjectWriter writer;

    public QuarkusCommandInvocation(QuarkusPlatformDescriptor platformDescr) {
        this(platformDescr, new DefaultMessageWriter());
    }

    public QuarkusCommandInvocation(QuarkusPlatformDescriptor platformDescr, MessageWriter log) {
        this(platformDescr, log, new HashMap<>(), new Properties(System.getProperties()));
    }

    public QuarkusCommandInvocation(QuarkusPlatformDescriptor platformDescr, MessageWriter log, Map<String, Object> values,
            Properties props) {
        super(values);
        this.platformDescr = platformDescr;
        this.log = log;
        this.props = props;
    }

    public QuarkusCommandInvocation(QuarkusCommandInvocation original) {
        super(original.values);
        this.platformDescr = original.platformDescr;
        this.log = original.log;
        this.props = new Properties();
        this.props.putAll(original.props);
        this.buildTool = original.buildTool;
        this.buildFile = original.buildFile;
        this.writer = original.writer;
    }

    public MessageWriter getMessageWriter() {
        return log;
    }

    public QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescr;
    }

    public String getProperty(String name) {
        final String value = props.getProperty(name, NOT_SET);
        return value == NOT_SET ? System.getProperty(name) : value;
    }

    public boolean hasProperty(String name) {
        return props.getOrDefault(name, NOT_SET) != NOT_SET;
    }

    public QuarkusCommandInvocation setProperty(String name, String value) {
        props.setProperty(name, value);
        return this;
    }

    public Properties getProperties() {
        return props;
    }

    public BuildTool getBuildTool() {
        return buildTool;
    }

    public void setBuildTool(BuildTool buildTool) {
        this.buildTool = buildTool;
    }

    public BuildFile getBuildFile() {
        return getBuildFile(true);
    }

    BuildFile getBuildFile(boolean required) {
        if (buildFile == null) {
            if (writer == null) {
                if (required) {
                    throw new IllegalStateException(
                            "Neither project's build file handler nor the project writer has been provided");
                }
                return null;
            }
            if (buildTool == null) {
                try {
                    buildFile = new MavenBuildFile(writer);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to initialize project's build file handler", e);
                }
            } else {
                try {
                    buildFile = buildTool.createBuildFile(writer);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to initialize project's build file handler", e);
                }
            }
        }
        return buildFile;
    }

    public void setBuildFile(BuildFile buildFile) {
        this.buildFile = buildFile;
    }

    public ProjectWriter getProjectWriter() {
        return writer;
    }

    public void setProjectWriter(ProjectWriter writer) {
        this.writer = writer;
    }
}
