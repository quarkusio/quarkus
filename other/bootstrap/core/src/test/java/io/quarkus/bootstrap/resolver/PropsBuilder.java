package io.quarkus.bootstrap.resolver;

import java.util.Properties;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropsBuilder {

    public static Properties build(String name, Object value) {
        return build(name, value.toString());
    }

    public static Properties build(String name, String value) {
        final Properties props = new Properties();
        props.setProperty(name, value);
        return props;
    }

    public static PropsBuilder newInstance() {
        return new PropsBuilder();
    }

    private final Properties props = new Properties();

    private PropsBuilder() {
    }

    public PropsBuilder set(String name, String value) {
        props.setProperty(name, value);
        return this;
    }

    public Properties build() {
        return props;
    }
}
