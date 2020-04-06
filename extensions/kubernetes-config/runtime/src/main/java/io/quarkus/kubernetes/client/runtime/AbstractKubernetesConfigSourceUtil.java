package io.quarkus.kubernetes.client.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

abstract class AbstractKubernetesConfigSourceUtil {

    private static final Logger log = Logger.getLogger(AbstractKubernetesConfigSourceUtil.class);

    private static final String APPLICATION_YML = "application.yml";
    private static final String APPLICATION_YAML = "application.yaml";
    private static final String APPLICATION_PROPERTIES = "application.properties";

    private static final int ORDINAL = 270; // this is higher than the file system or jar ordinals, but lower than env vars

    abstract String getType();

    abstract ConfigSource createLiteralDataConfigSource(String kubernetesConfigSourceName, Map<String, String> propertyMap,
            int ordinal);

    abstract ConfigSource createPropertiesConfigSource(String kubernetesConfigSourceName, String fileName, String input,
            int ordinal);

    abstract ConfigSource createYamlConfigSource(String kubernetesConfigSourceName, String fileName, String input, int ordinal);

    /**
     * Returns a list of {@code ConfigSource} for the literal data that is contained in the ConfigMap
     * and for the application.{properties|yaml|yml} files that might be contained in it as well
     *
     * All the {@code ConfigSource} objects use the same ordinal which is higher than the ordinal
     * of normal configuration files, but lower than that of environment variables
     */
    List<ConfigSource> toConfigSources(String kubernetesConfigSourceName, Map<String, String> kubernetesConfigSourceDataMap) {
        if (log.isDebugEnabled()) {
            log.debug("Attempting to convert data in " + getType() + " '" + kubernetesConfigSourceName
                    + "' to a list of ConfigSource objects");
        }

        CategorizedConfigSourceData categorizedConfigSourceData = categorize(kubernetesConfigSourceDataMap);
        List<ConfigSource> result = new ArrayList<>(categorizedConfigSourceData.fileData.size() + 1);

        if (!categorizedConfigSourceData.literalData.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Adding a ConfigSource for the literal data of " + getType() + " '" + kubernetesConfigSourceName + "'");
            }
            result.add(createLiteralDataConfigSource(kubernetesConfigSourceName + "-literalData",
                    categorizedConfigSourceData.literalData,
                    ORDINAL));
        }
        for (Map.Entry<String, String> entry : categorizedConfigSourceData.fileData) {
            String fileName = entry.getKey();
            String rawFileData = entry.getValue();
            if (APPLICATION_PROPERTIES.equals(fileName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding a Properties ConfigSource for file '" + fileName + "' of " + getType()
                            + " '" + kubernetesConfigSourceName + "'");
                }
                result.add(createPropertiesConfigSource(kubernetesConfigSourceName, fileName, rawFileData, ORDINAL));
            } else if (APPLICATION_YAML.equals(fileName) || APPLICATION_YML.equals(fileName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding a YAML ConfigSource for file '" + fileName + "' of " + getType()
                            + " '" + kubernetesConfigSourceName + "'");
                }
                result.add(createYamlConfigSource(kubernetesConfigSourceName, fileName, rawFileData, ORDINAL));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(getType() + " '" + kubernetesConfigSourceName + "' converted into " + result.size()
                    + "ConfigSource objects");
        }
        return result;
    }

    private static CategorizedConfigSourceData categorize(Map<String, String> data) {
        if ((data == null) || data.isEmpty()) {
            return new CategorizedConfigSourceData(Collections.emptyMap(), Collections.emptyList());
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

        return new CategorizedConfigSourceData(literalData, fileData);
    }

    private static class CategorizedConfigSourceData {
        final Map<String, String> literalData;
        final List<Map.Entry<String, String>> fileData;

        CategorizedConfigSourceData(Map<String, String> literalData, List<Map.Entry<String, String>> fileData) {
            this.literalData = literalData;
            this.fileData = fileData;
        }
    }
}
