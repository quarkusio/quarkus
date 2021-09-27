package io.quarkus.kubernetes.config.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.config.source.yaml.YamlConfigSource;

public class ConfigMapConfigSourceUtil extends AbstractKubernetesConfigSourceUtil {

    @Override
    String getType() {
        return "ConfigMap";
    }

    @Override
    OrdinalData ordinalData() {
        return OrdinalData.CONFIG_MAP;
    }

    @Override
    ConfigSource createLiteralDataConfigSource(String kubernetesConfigSourceName, Map<String, String> propertyMap,
            int ordinal) {
        return new ConfigMapLiteralDataPropertiesConfigSource(kubernetesConfigSourceName, propertyMap, ordinal);
    }

    @Override
    ConfigSource createPropertiesConfigSource(String kubernetesConfigSourceName, String fileName, String input, int ordinal) {
        return new ConfigMapStringInputPropertiesConfigSource(kubernetesConfigSourceName, fileName, input, ordinal);
    }

    @Override
    ConfigSource createYamlConfigSource(String kubernetesConfigSourceName, String fileName, String input, int ordinal) {
        return new ConfigMapStringInputYamlConfigSource(kubernetesConfigSourceName, fileName, input, ordinal);
    }

    private static final class ConfigMapLiteralDataPropertiesConfigSource extends MapBackedConfigSource {

        private static final String NAME_PREFIX = "ConfigMapLiteralDataPropertiesConfigSource[configMap=";

        public ConfigMapLiteralDataPropertiesConfigSource(String configMapName, Map<String, String> propertyMap, int ordinal) {
            super(NAME_PREFIX + configMapName + "]", propertyMap, ordinal);
        }
    }

    private static class ConfigMapStringInputPropertiesConfigSource extends MapBackedConfigSource {

        private static final String NAME_FORMAT = "ConfigMapStringInputPropertiesConfigSource[configMap=%s,file=%s]";

        ConfigMapStringInputPropertiesConfigSource(String configMapName, String fileName, String input, int ordinal) {
            super(String.format(NAME_FORMAT, configMapName, fileName), readProperties(input), ordinal);
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

    private static class ConfigMapStringInputYamlConfigSource extends YamlConfigSource {

        private static final String NAME_FORMAT = "ConfigMapStringInputYamlConfigSource[configMap=%s,file=%s]";

        public ConfigMapStringInputYamlConfigSource(String configMapName, String fileName, String input, int ordinal) {
            super(String.format(NAME_FORMAT, configMapName, fileName), input, ordinal);
        }
    }
}
