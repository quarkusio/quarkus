package io.quarkus.maven;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import io.quarkus.bootstrap.model.JvmOptions;
import io.quarkus.bootstrap.model.JvmOptionsBuilder;

/**
 * Extension Dev mode configuration options
 */
public class ExtensionDevModeMavenConfig {

    private JvmOptionsMap jvmOptions;
    private XxJvmOptionsMap xxJvmOptions;
    private List<String> lockJvmOptions = List.of();
    private List<String> lockXxJvmOptions = List.of();

    /**
     * Standard JVM options that should be added to the command line launching an application in Dev mode.
     *
     * @return standard JVM options that should be added to the command line launching an application in Dev mode
     */
    public JvmOptions getJvmOptions() {
        return jvmOptions == null ? null : jvmOptions.builder.build();
    }

    public void setJvmOptions(JvmOptionsMap jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    /**
     * Non-standard JVM options that should be added to the command line launching an application in Dev mode.
     *
     * @return non-standard JVM options that should be added to the command line launching an application in Dev mode
     */
    public JvmOptions getXxJvmOptions() {
        return xxJvmOptions == null ? null : xxJvmOptions.builder.build();
    }

    public void setXxJvmOptions(XxJvmOptionsMap xxJvmOptions) {
        this.xxJvmOptions = xxJvmOptions;
    }

    /**
     * JVM options whose default values should not be overridden by non-default values that would be set
     * by default by Quarkus Maven and Gradle plugins for dev mode.
     *
     * @return JVM option names
     */
    public List<String> getLockJvmOptions() {
        return lockJvmOptions;
    }

    public void setLockJvmOptions(List<String> lockJvmOptions) {
        this.lockJvmOptions = lockJvmOptions;
    }

    public boolean hasLockedJvmOptions() {
        return !lockJvmOptions.isEmpty();
    }

    /**
     * XX JVM options whose default values should not be overridden by non-default values that would be set
     * by default by Quarkus Maven and Gradle plugins for dev mode.
     *
     * @return XX JVM option names
     */
    public List<String> getLockXxJvmOptions() {
        return lockXxJvmOptions;
    }

    public void setLockXxJvmOptions(List<String> lockXxJvmOptions) {
        this.lockXxJvmOptions = lockXxJvmOptions;
    }

    public boolean hasLockedXxJvmOptions() {
        return !lockXxJvmOptions.isEmpty();
    }

    /**
     * This {@link java.util.Map} implementation overrides the {@link java.util.Map#put(Object, Object)} method
     * to allow the users configuring a parameter with the same name more than once, merging all the configured values.
     */
    public static class JvmOptionsMap extends AbstractMap<String, String> {

        protected final JvmOptionsBuilder builder = JvmOptions.builder();

        public JvmOptionsMap() {
            super();
        }

        @Override
        public String put(String name, String value) {
            builder.add(name, nullOrTrim(value));
            return null;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            // this is just to make Maven debug mode work with the type
            var options = builder.getOptions();
            var map = new HashMap<String, String>(options.size());
            for (var arg : options) {
                map.put(arg.getName(), arg.getValues().toString());
            }
            return map.entrySet();
        }

        static String nullOrTrim(Object value) {
            return value == null ? null : String.valueOf(value).trim();
        }
    }

    public static class XxJvmOptionsMap extends JvmOptionsMap {

        public XxJvmOptionsMap() {
            super();
        }

        @Override
        public String put(String name, String value) {
            builder.addXxOption(name, nullOrTrim(value));
            return null;
        }
    }
}
