package io.quarkus.deployment.configuration.tracker;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;

import io.quarkus.bootstrap.util.PropertyUtils;
import io.smallrye.config.SmallRyeConfig;

/**
 * Transforms configuration values before they are written to a file
 */
public class ConfigTrackingValueTransformer {

    private static final String NOT_CONFIGURED = "quarkus.config-tracking:not-configured";
    private static final String PATH_ELEMENT_SEPARATOR = "/";
    private static final String USER_HOME_DIR_ALIAS = "~";

    private static volatile MessageDigest SHA512;

    private static MessageDigest getSHA512() {
        if (SHA512 == null) {
            try {
                SHA512 = MessageDigest.getInstance("SHA-512");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return SHA512;
    }

    public static ConfigTrackingValueTransformer newInstance(Config config) {
        return new ConfigTrackingValueTransformer(
                config.unwrap(SmallRyeConfig.class).getConfigMapping(ConfigTrackingConfig.class));
    }

    public static ConfigTrackingValueTransformer newInstance(ConfigTrackingConfig config) {
        return new ConfigTrackingValueTransformer(config);
    }

    /**
     * Returns a non-null string value for a given {@link org.eclipse.microprofile.config.ConfigValue} instance.
     *
     * @param value configuration value
     * @return non-null string value for a given {@link org.eclipse.microprofile.config.ConfigValue} instance
     */
    public static String asString(ConfigValue value) {
        if (value == null) {
            return NOT_CONFIGURED;
        }
        var strValue = value.getValue();
        return strValue == null ? NOT_CONFIGURED : strValue;
    }

    private final String userHomeDir;
    private final List<Pattern> hashOptionsPatterns;

    private ConfigTrackingValueTransformer(ConfigTrackingConfig config) {
        userHomeDir = config.useUserHomeAliasInPaths() ? PropertyUtils.getUserHome() : null;
        hashOptionsPatterns = config.getHashOptionsPatterns();
    }

    /**
     * Returns a string value that can be persisted to file.
     *
     * @param name option name
     * @param value configuration value
     * @return string value that can be persisted to file
     */
    public String transform(String name, ConfigValue value) {
        return value == null ? NOT_CONFIGURED : transform(name, value.getValue());
    }

    /**
     * Returns a string value that can be persisted to file.
     *
     * @param name option name
     * @param original configuration value
     * @return string value that can be persisted to file
     */
    public String transform(String name, String original) {
        if (original == null) {
            return NOT_CONFIGURED;
        }

        for (Pattern pattern : hashOptionsPatterns) {
            if (pattern.matcher(name).matches()) {
                return sha512(original);
            }
        }

        // replace user home path with an alias
        if (userHomeDir != null && original.startsWith(userHomeDir)) {
            var relativePath = original.substring(userHomeDir.length());
            if (relativePath.isEmpty()) {
                return USER_HOME_DIR_ALIAS;
            }
            if (File.separator.equals(PATH_ELEMENT_SEPARATOR)) {
                return USER_HOME_DIR_ALIAS + relativePath;
            }
            final StringJoiner joiner = new StringJoiner("/");
            joiner.add(USER_HOME_DIR_ALIAS);
            var path = Path.of(relativePath);
            for (int i = 0; i < path.getNameCount(); ++i) {
                joiner.add(path.getName(i).toString());
            }
            return joiner.toString();
        }

        return original;
    }

    public static String sha512(String value) {
        return sha512(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha512(byte[] value) {
        final byte[] digest = getSHA512().digest(value);
        final StringBuilder sb = new StringBuilder(40);
        for (int i = 0; i < digest.length; ++i) {
            sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
}
