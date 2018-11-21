package org.jboss.shamrock.deployment;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.deployment.configuration.ConfigurationError;

public final class ShamrockConfig extends SimpleBuildItem {

    public static final ShamrockConfig INSTANCE = new ShamrockConfig();

    private static final Map<Object, String> reverseMap = new IdentityHashMap<>();

    private ShamrockConfig() {
        //not to be constructed
    }

    public static String getString(String configKey, String defaultValue, boolean allowNull) {
        String opt = ConfigProvider.getConfig().getOptionalValue(configKey, String.class).orElse(defaultValue);
        if (opt == null) {
            if (!allowNull) {
                throw requiredConfigMissing(configKey);
            }
            return null;
        }
        String retVal = new String(opt);
        reverseMap.put(retVal, configKey);
        return retVal;
    }

    public static Set<String> getNames(String prefix) {
        Set<String> props = new HashSet<>();
        for (String i : ConfigProvider.getConfig().getPropertyNames()) {
            if (i.startsWith(prefix)) {
                int idex = i.indexOf('.', prefix.length());
                if (idex == -1) {
                    props.add(i.substring(prefix.length()));
                } else {
                    props.add(i.substring(prefix.length(), idex));
                }
            }
        }
        return props;
    }

    public static String getConfigKey(String val) {
        return reverseMap.get(val);
    }

    public static boolean getBoolean(String configKey, String defaultValue) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        if (val == null || val.isEmpty()) {
            return false;
        }
        return asBoolean(val, configKey);
    }

    public static Boolean getBoxedBoolean(String configKey, String defaultValue, boolean allowNull) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        Boolean result;
        if (val == null || val.isEmpty()) {
            if (!allowNull) {
                throw requiredConfigMissing(configKey);
            }
            return null;
        } else {
            result = asBoolean(val, configKey);
        }
        result = Boolean.valueOf(result);
        reverseMap.put(result, configKey);
        return result;
    }

    public static int getInt(String configKey, String defaultValue) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        if (val == null || val.isEmpty()) {
            return 0;
        }
        return asInteger(val, configKey);
    }

    public static Integer getBoxedInt(String configKey, String defaultValue, boolean allowNull) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        Integer result;
        if (val == null || val.isEmpty()) {
            if (!allowNull) {
                throw requiredConfigMissing(configKey);
            }
            return null;
        } else {
            result = asInteger(val, configKey);
        }
        result = Integer.valueOf(result);
        reverseMap.put(result, configKey);
        return result;
    }

    private static ConfigurationError requiredConfigMissing(final String configKey) {
        return new ConfigurationError("Required configuration property '" + configKey + "' was not defined.");
    }

    private static boolean asBoolean(final String val, final String configKey) {
        final String cleanValue = cleanupValue(val);
        if ("true".equals(cleanValue)) {
            return true;
        }
        else if ("false".equals(cleanValue)) {
            return false;
        }
        else {
            throw new ConfigurationError("Configuration value for property '" + configKey + "' was set to '" + val + "'. " +
                  "A boolean value is expected; set this property to either 'true' or 'false'.");
        }
    }

    private static int asInteger(final String val, final String configKey) {
        final String cleanValue = cleanupValue(val);
        try {
            return Integer.parseInt(cleanValue);
        }
        catch (NumberFormatException nfe) {
            throw new ConfigurationError("Configuration value for property '" + configKey + "' was set to '" + val + "'. " +
                  "An integer value is expected; set this property to a decimal integer.");
        }
    }

    private static String cleanupValue(final String val) {
        return val.trim().toLowerCase(Locale.ROOT);
    }

}
