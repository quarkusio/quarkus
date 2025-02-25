package io.quarkus.bootstrap.model;

import java.util.List;

import io.quarkus.bootstrap.BootstrapConstants;

public class MutableXxJvmOption extends MutableBaseJvmOption<MutableXxJvmOption> {

    public static final String PROPERTY_GROUP_PREFIX = "xx.";

    private static final String COMPLETE_PROPERTY_PREFIX = BootstrapConstants.EXT_DEV_MODE_JVM_OPTION_PREFIX
            + PROPERTY_GROUP_PREFIX;

    private static final String DASH_XX_COLLON = "-XX:";

    public static MutableXxJvmOption fromQuarkusExtensionProperty(String propertyName, String value) {
        final String optionName = propertyName.substring(COMPLETE_PROPERTY_PREFIX.length());
        return value.isBlank() ? newInstance(optionName) : newInstance(optionName, value);
    }

    public static MutableXxJvmOption newInstance(String name) {
        return newInstance(name, null);
    }

    public static MutableXxJvmOption newInstance(String name, String value) {
        var result = new MutableXxJvmOption();
        result.setName(name);
        if (value != null) {
            result.addValue(value);
        }
        return result;
    }

    @Override
    protected String getQuarkusExtensionPropertyPrefix() {
        return COMPLETE_PROPERTY_PREFIX;
    }

    @Override
    public List<String> toCliOptions() {
        if (!hasValue()) {
            return toBooleanOption(true);
        }
        if (getValues().size() == 1) {
            var value = getValues().iterator().next();
            if ("true".equalsIgnoreCase(value) || "+".equals(value)) {
                return toBooleanOption(true);
            }
            if ("false".equalsIgnoreCase(value) || "-".equals(value)) {
                return toBooleanOption(false);
            }
            return List.of(DASH_XX_COLLON + getName() + "=" + value);
        }
        throw new IllegalArgumentException(
                "Failed to format option " + DASH_XX_COLLON + getName() + " with values " + getValues());
    }

    private List<String> toBooleanOption(boolean enabled) {
        return List.of(DASH_XX_COLLON + (enabled ? "+" : "-") + getName());
    }
}
