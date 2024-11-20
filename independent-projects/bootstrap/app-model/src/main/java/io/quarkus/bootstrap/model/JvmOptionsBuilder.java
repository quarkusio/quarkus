package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * JVM arguments builder
 */
public class JvmOptionsBuilder {

    public static boolean isExtensionDevModeJvmOptionProperty(String name) {
        return name.startsWith(BootstrapConstants.EXT_DEV_MODE_JVM_OPTION_PREFIX);
    }

    private static String getGroupPrefixForPropertyName(String propName) {
        final int i = propName.indexOf('.', BootstrapConstants.EXT_DEV_MODE_JVM_OPTION_PREFIX.length() + 1);
        if (i < 0) {
            throw new IllegalArgumentException("Failed to determine JVM option group given property name " + propName);
        }
        return propName.substring(BootstrapConstants.EXT_DEV_MODE_JVM_OPTION_PREFIX.length(), i + 1);
    }

    private Map<String, MutableBaseJvmOption<?>> options = Map.of();

    JvmOptionsBuilder() {
    }

    public void addFromQuarkusExtensionProperty(String propertyName, String value) {
        final String groupPrefix = getGroupPrefixForPropertyName(propertyName);
        final String optionName = propertyName
                .substring(BootstrapConstants.EXT_DEV_MODE_JVM_OPTION_PREFIX.length() + groupPrefix.length());
        addToGroup(groupPrefix, optionName, value);
    }

    private JvmOptionsBuilder addToGroup(String optionGroupPrefix, String optionName, String value) {
        if (options.isEmpty()) {
            options = new HashMap<>();
        }
        var option = options.computeIfAbsent(optionName, n -> {
            switch (optionGroupPrefix) {
                case MutableStandardJvmOption.PROPERTY_GROUP_PREFIX:
                    return MutableStandardJvmOption.newInstance(optionName);
                case MutableXxJvmOption.PROPERTY_GROUP_PREFIX:
                    return MutableXxJvmOption.newInstance(optionName);
            }
            throw new IllegalArgumentException("Unrecognized JVM option group prefix " + optionGroupPrefix);
        });
        if (!value.isBlank()) {
            option.addValue(value);
        }
        return this;
    }

    /**
     * Adds a standard option without a value.
     *
     * @param name option name without the starting dashes
     * @return this builder instance
     */
    public JvmOptionsBuilder add(String name) {
        if (options.isEmpty()) {
            options = new HashMap<>();
        }
        options.computeIfAbsent(name, n -> MutableStandardJvmOption.newInstance(name));
        return this;
    }

    /**
     * Adds a standard option with a value.
     *
     * @param name option name without the starting dashes
     * @param value option value, must not be null or black
     * @return this builder instance
     */
    public JvmOptionsBuilder add(String name, String value) {
        if (options.isEmpty()) {
            options = new HashMap<>();
        }
        var arg = options.get(name);
        if (arg == null) {
            options.put(name, MutableStandardJvmOption.newInstance(name, value));
        } else {
            arg.addValue(value);
        }
        return this;
    }

    /**
     * Adds a standard option with multiple values.
     *
     * @param name option name without the starting dashes
     * @param values option values
     * @return this builder instance
     */
    public JvmOptionsBuilder addAll(String name, Collection<String> values) {
        if (options.isEmpty()) {
            options = new HashMap<>();
        }
        var option = options.computeIfAbsent(name, n -> MutableStandardJvmOption.newInstance(name));
        for (var value : values) {
            option.addValue(value);
        }
        return this;
    }

    /**
     * Adds a non-standard option that should be prefixed with {@code -XX:} when added to the command line with a value.
     *
     * @param name option name without the starting dashes
     * @param value option value, must not be null or black
     * @return this builder instance
     */
    public JvmOptionsBuilder addXxOption(String name, String value) {
        if (options.isEmpty()) {
            options = new HashMap<>();
        }
        var arg = options.get(name);
        if (arg == null) {
            options.put(name, MutableXxJvmOption.newInstance(name, value));
        } else {
            arg.addValue(value);
        }
        return this;
    }

    /**
     * Adds JVM options with their values.
     *
     * @param options options to add
     * @return this builder instance
     */
    public JvmOptionsBuilder addAll(JvmOptions options) {
        if (this.options.isEmpty()) {
            this.options = new HashMap<>();
        }
        for (var option : options.asCollection()) {
            if (option.hasValue()) {
                var existing = this.options.putIfAbsent(option.getName(), (MutableBaseJvmOption<?>) option);
                if (existing != null) {
                    for (var value : option.getValues()) {
                        existing.addValue(value);
                    }
                }
                addAll(option.getName(), option.getValues());
            } else {
                this.options.putIfAbsent(option.getName(), (MutableBaseJvmOption<?>) option);
            }
        }
        return this;
    }

    /**
     * Collection of currently configured options.
     *
     * @return collection of currently configured options
     */
    public Collection<JvmOption> getOptions() {
        return List.copyOf(options.values());
    }

    /**
     * Checks whether an option with a given name is set.
     *
     * @param optionName option name
     * @return true in case the option is set, otherwise - false
     */
    public boolean contains(String optionName) {
        for (var option : options.values()) {
            if (option.getName().equals(optionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finalizes a collection of JVM options by creating an instance of {@link JvmOptions}.
     *
     * @return new instance of {@link JvmOptions} with all the configured options
     */
    public JvmOptions build() {
        return new JvmOptionsImpl(options.isEmpty() ? List.of() : List.copyOf(options.values()));
    }

    private static class JvmOptionsImpl implements JvmOptions, Serializable {

        private final List<JvmOption> args;

        private JvmOptionsImpl(List<JvmOption> args) {
            this.args = args;
        }

        @Override
        public Collection<JvmOption> asCollection() {
            return args;
        }
    }
}
