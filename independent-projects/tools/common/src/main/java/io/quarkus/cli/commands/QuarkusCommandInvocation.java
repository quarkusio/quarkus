package io.quarkus.cli.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.MessageWriter;

public class QuarkusCommandInvocation extends ValueMap<QuarkusCommandInvocation> {

    protected final QuarkusPlatformDescriptor platformDescr;
    protected final MessageWriter log;
    protected final Properties props;

    public QuarkusCommandInvocation(QuarkusPlatformDescriptor platformDescr, MessageWriter log) {
        this(platformDescr, log, new HashMap<>(), new Properties(System.getProperties()));
    }

    public QuarkusCommandInvocation(QuarkusPlatformDescriptor platformDescr, MessageWriter log, Map<String, Object> values, Properties props) {
        super(values);
        this.platformDescr = platformDescr;
        this.log = log;
        this.props = props;
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
}
