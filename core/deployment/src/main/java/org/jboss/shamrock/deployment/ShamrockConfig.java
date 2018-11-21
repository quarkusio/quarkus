package org.jboss.shamrock.deployment;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.builder.item.SimpleBuildItem;

public final class ShamrockConfig extends SimpleBuildItem {

    public static final ShamrockConfig INSTANCE = new ShamrockConfig();

    private static final Map<Object, String> reverseMap = new IdentityHashMap<>();

    private ShamrockConfig() {

    }

    public static String getString(String key, String defaultValue, boolean allowNull) {
        String opt = ConfigProvider.getConfig().getOptionalValue(key, String.class).orElse(defaultValue);
        if (opt == null) {
            if (!allowNull) {
                throw new IllegalStateException("Excepted config property " + key + " was not found");
            }
            return null;
        }
        String retVal = new String(opt);
        reverseMap.put(retVal, key);
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
        return Boolean.parseBoolean(val);
    }

    public static Boolean getBoxedBoolean(String configKey, String defaultValue, boolean allowNull) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        Boolean result;
        if (val == null || val.isEmpty()) {
            if (!allowNull) {
                throw new IllegalStateException("Excepted config property " + configKey + " was not found");
            }
            return null;
        } else {
            result = Boolean.parseBoolean(val);
        }
        result = new Boolean(result);
        reverseMap.put(result, configKey);
        return result;
    }

    public static int getInt(String configKey, String defaultValue) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        if (val == null || val.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(val);
    }

    public static Integer getBoxedInt(String configKey, String defaultValue, boolean allowNull) {
        Optional<String> res = ConfigProvider.getConfig().getOptionalValue(configKey, String.class);
        String val = res.orElse(defaultValue);
        Integer result;
        if (val == null || val.isEmpty()) {
            if (!allowNull) {
                throw new IllegalStateException("Excepted config property " + configKey + " was not found");
            }
            return null;
        } else {
            result = Integer.parseInt(val);
        }
        result = new Integer(result);
        reverseMap.put(result, configKey);
        return result;
    }
}
