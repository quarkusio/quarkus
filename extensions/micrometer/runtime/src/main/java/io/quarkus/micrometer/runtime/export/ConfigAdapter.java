package io.quarkus.micrometer.runtime.export;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.config.MeterRegistryConfig;
import io.micrometer.core.instrument.config.validate.InvalidReason;
import io.micrometer.core.instrument.config.validate.Validated;

public class ConfigAdapter {
    private static final Logger log = Logger.getLogger(ConfigAdapter.class);
    static final String ROOT = "quarkus.micrometer.export.";
    static final int TRIM_POS = ROOT.length();

    private ConfigAdapter() {
    }

    public static Map<String, String> captureProperties(Config config, String prefix) {
        final Map<String, String> properties = new HashMap<>();

        // Rename and store properties
        for (String name : config.getPropertyNames()) {
            if (name.startsWith(prefix)) {
                String key = convertKey(name);
                String value = config.getValue(name, String.class);
                properties.put(key, value);
            }
        }
        return properties;
    }

    public static <T extends MeterRegistryConfig> T validate(T config) {
        return validate(config, config.validate());
    }

    public static <T extends MeterRegistryConfig> T validate(T config, Validated validated) {
        List<Validated.Invalid<T>> errors = validated.failures();
        if (validated.isInvalid()) {
            errors.stream().forEach(x -> {
                String name = revertKey(x.getProperty());
                if (x.getReason() == InvalidReason.MISSING) {
                    log.errorf("%s is required", name);
                } else {
                    log.errorf(x.getException(), "%s is malformed", name);
                }
            });
        }
        return config;
    }

    static String convertKey(String name) {
        String key = name.substring(TRIM_POS);
        return camelHumpify(key);
    }

    static String revertKey(String name) {
        return ROOT + dashify(name);
    }

    static String camelHumpify(String s) {
        if (s.indexOf('-') >= 0) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '-') {
                    i++;
                    if (i < s.length()) {
                        b.append(Character.toUpperCase(s.charAt(i)));
                    }
                } else {
                    b.append(s.charAt(i));
                }
            }
            return b.toString();
        }
        return s;
    }

    static String dashify(String s) {
        final StringBuilder ret = new StringBuilder();
        final char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if (i != 0 && i != (chars.length - 1) && c >= 'A' && c <= 'Z') {
                ret.append('-');
            }
            ret.append(Character.toLowerCase(c));
        }
        return ret.toString();
    }
}
