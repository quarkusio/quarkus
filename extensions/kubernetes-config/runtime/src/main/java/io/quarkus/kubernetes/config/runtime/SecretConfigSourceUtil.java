package io.quarkus.kubernetes.config.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.config.source.yaml.YamlConfigSource;

public class SecretConfigSourceUtil extends AbstractKubernetesConfigSourceUtil {

    @Override
    String getType() {
        return "Secret";
    }

    @Override
    OrdinalData ordinalData() {
        return OrdinalData.SECRET;
    }

    @Override
    ConfigSource createLiteralDataConfigSource(String kubernetesConfigSourceName, Map<String, String> propertyMap,
            int ordinal) {
        return new SecretLiteralDataPropertiesConfigSource(kubernetesConfigSourceName, propertyMap, ordinal);
    }

    @Override
    ConfigSource createPropertiesConfigSource(String kubernetesConfigSourceName, String fileName, String input, int ordinal) {
        return new SecretStringInputPropertiesConfigSource(kubernetesConfigSourceName, fileName, input, ordinal);
    }

    @Override
    ConfigSource createYamlConfigSource(String kubernetesConfigSourceName, String fileName, String input, int ordinal) {
        return new SecretStringInputYamlConfigSource(kubernetesConfigSourceName, fileName, input, ordinal);
    }

    static String decodeValue(String value) {
        return new String(Base64.getDecoder().decode(value));
    }

    static Map<String, String> decodeMapValues(Map<String, String> input) {
        Map<String, String> decodedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            decodedMap.put(entry.getKey(), decodeValue(entry.getValue()));
        }
        return decodedMap;
    }

    private static final class SecretLiteralDataPropertiesConfigSource extends MapBackedConfigSource {

        private static final String NAME_PREFIX = "SecretLiteralDataPropertiesConfigSource[secret=";

        public SecretLiteralDataPropertiesConfigSource(String secretName, Map<String, String> propertyMap, int ordinal) {
            super(NAME_PREFIX + secretName + "]", decodeMapValues(propertyMap), ordinal);
        }
    }

    private static class SecretStringInputPropertiesConfigSource extends MapBackedConfigSource {

        private static final String NAME_FORMAT = "SecretStringInputPropertiesConfigSource[secret=%s,file=%s]";

        SecretStringInputPropertiesConfigSource(String secretName, String fileName, String input, int ordinal) {
            super(String.format(NAME_FORMAT, secretName, fileName), readProperties(decodeValue(input)), ordinal);
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

    private static class SecretStringInputYamlConfigSource extends YamlConfigSource {

        private static final String NAME_FORMAT = "SecretStringInputYamlConfigSource[secret=%s,file=%s]";

        public SecretStringInputYamlConfigSource(String secretName, String fileName, String input, int ordinal) {
            super(String.format(NAME_FORMAT, secretName, fileName), decodeValue(input), ordinal);
        }
    }
}
