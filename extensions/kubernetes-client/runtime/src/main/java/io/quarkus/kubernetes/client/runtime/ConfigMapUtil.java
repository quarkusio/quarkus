package io.quarkus.kubernetes.client.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.config.source.yaml.YamlConfigSource;

final class ConfigMapUtil {

    private static final Logger log = Logger.getLogger(ConfigMapUtil.class);

    private static final String APPLICATION_YML = "application.yml";
    private static final String APPLICATION_YAML = "application.yaml";
    private static final String APPLICATION_PROPERTIES = "application.properties";

    private static final int ORDINAL = 270; // this is higher than the file system or jar ordinals, but lower than env vars

    private ConfigMapUtil() {
    }

    /**
     * Returns a list of {@code ConfigSource} for the literal data that is contained in the ConfigMap
     * and for the application.{properties|yaml|yml} files that might be contained in it as well
     *
     * All the {@code ConfigSource} objects use the same ordinal which is higher than the ordinal
     * of normal configuration files, but lower than that of environment variables
     */
    static List<ConfigSource> toConfigSources(ConfigMap configMap) {
        String configMapName = configMap.getMetadata().getName();
        if (log.isDebugEnabled()) {
            log.debug("Attempting to convert data in ConfigMap '" + configMapName + "' to a list of ConfigSource objects");
        }

        ConfigMapData configMapData = parse(configMap);
        List<ConfigSource> result = new ArrayList<>(configMapData.fileData.size() + 1);

        if (!configMapData.literalData.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Adding a ConfigSource for the literal data of ConfigMap '" + configMapName + "'");
            }
            result.add(new ConfigMapLiteralDataPropertiesConfigSource(configMapName + "-literalData", configMapData.literalData,
                    ORDINAL));
        }
        for (Map.Entry<String, String> entry : configMapData.fileData) {
            String fileName = entry.getKey();
            String rawFileData = entry.getValue();
            if (APPLICATION_PROPERTIES.equals(fileName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding a Properties ConfigSource for file '" + fileName + "' of ConfigMap '" + configMapName
                            + "'");
                }
                result.add(new ConfigMapStringInputPropertiesConfigSource(configMapName, fileName, rawFileData, ORDINAL));
            } else if (APPLICATION_YAML.equals(fileName) || APPLICATION_YML.equals(fileName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding a YAML ConfigSource for file '" + fileName + "' of ConfigMap '" + configMapName + "'");
                }
                result.add(new ConfigMapStringInputYamlConfigSource(configMapName, fileName, rawFileData, ORDINAL));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("ConfigMap's '" + configMapName + "' converted into " + result.size() + "ConfigSource objects");
        }
        return result;
    }

    private static ConfigMapData parse(ConfigMap configMap) {
        Map<String, String> data = configMap.getData();
        if ((data == null) || data.isEmpty()) {
            return new ConfigMapData(Collections.emptyMap(), Collections.emptyList());
        }

        Map<String, String> literalData = new HashMap<>();
        List<Map.Entry<String, String>> fileData = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(".yml") || key.endsWith(".yaml") || key.endsWith(".properties")) {
                fileData.add(entry);
            } else {
                literalData.put(key, entry.getValue());
            }
        }

        return new ConfigMapData(literalData, fileData);
    }

    private static class ConfigMapData {
        final Map<String, String> literalData;
        final List<Map.Entry<String, String>> fileData;

        ConfigMapData(Map<String, String> literalData, List<Map.Entry<String, String>> fileData) {
            this.literalData = literalData;
            this.fileData = fileData;
        }
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
