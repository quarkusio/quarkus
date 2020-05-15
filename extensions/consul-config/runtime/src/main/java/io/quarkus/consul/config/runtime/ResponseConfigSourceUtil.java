package io.quarkus.consul.config.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.smallrye.config.common.MapBackedConfigSource;

class ResponseConfigSourceUtil {

    private static final Logger log = Logger.getLogger(ResponseConfigSourceUtil.class);

    private static final int ORDINAL = 270; // this is higher than the file system or jar ordinals, but lower than env vars

    public ConfigSource toConfigSource(Response response, ValueType valueType, Optional<String> prefix) {
        if (log.isDebugEnabled()) {
            log.debug("Attempting to convert data of key " + " '" + response.getKey()
                    + "' to a list of ConfigSource objects");
        }

        String keyWithoutPrefix = keyWithoutPrefix(response, prefix);

        ConfigSource result;
        if (valueType == ValueType.RAW) {
            result = new ConsulSingleValueConfigSource(keyWithoutPrefix, response.getDecodedValue(), ORDINAL);
        } else if (valueType == ValueType.PROPERTIES) {
            result = new ConsulPropertiesConfigSource(keyWithoutPrefix, response.getDecodedValue(), ORDINAL);
        } else {
            throw new IllegalArgumentException("Consul config value type '" + valueType + "' not supported");
        }

        log.debug("Done converting data of key '" + response.getKey() + "' into a ConfigSource");
        return result;
    }

    private String keyWithoutPrefix(Response response, Optional<String> prefix) {
        return prefix.isPresent() ? response.getKey().replace(prefix.get() + "/", "") : response.getKey();
    }

    private static final class ConsulSingleValueConfigSource extends MapBackedConfigSource {

        private static final String NAME_PREFIX = "ConsulSingleValueConfigSource[key=";

        public ConsulSingleValueConfigSource(String key, String value, int ordinal) {
            super(NAME_PREFIX + key + "]", Collections.singletonMap(effectiveKey(key), value), ordinal);
        }

        private static String effectiveKey(String key) {
            return key.replace('/', '.');
        }
    }

    private static class ConsulPropertiesConfigSource extends MapBackedConfigSource {

        private static final String NAME_FORMAT = "ConsulPropertiesConfigSource[key=%s]";

        ConsulPropertiesConfigSource(String key, String input, int ordinal) {
            super(String.format(NAME_FORMAT, key), readProperties(input), ordinal);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static Map<String, String> readProperties(String rawData) {
            try (StringReader br = new StringReader(rawData)) {
                final Properties properties = new Properties();
                properties.load(br);
                return (Map<String, String>) (Map) properties;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
