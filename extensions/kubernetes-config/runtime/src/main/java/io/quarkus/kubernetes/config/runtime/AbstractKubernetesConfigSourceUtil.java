package io.quarkus.kubernetes.config.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ObjectMeta;

abstract class AbstractKubernetesConfigSourceUtil {

    private static final Logger log = Logger.getLogger(AbstractKubernetesConfigSourceUtil.class);

    private static final String APPLICATION_YML = "application.yml";
    private static final String APPLICATION_YAML = "application.yaml";
    private static final String APPLICATION_PROPERTIES = "application.properties";

    abstract String getType();

    abstract OrdinalData ordinalData();

    abstract ConfigSource createLiteralDataConfigSource(String kubernetesConfigSourceName, Map<String, String> propertyMap,
            int ordinal);

    abstract ConfigSource createPropertiesConfigSource(String kubernetesConfigSourceName, String fileName, String input,
            int ordinal);

    abstract ConfigSource createYamlConfigSource(String kubernetesConfigSourceName, String fileName, String input, int ordinal);

    /**
     * Returns a list of {@code ConfigSource} for the literal data that is contained in the ConfigMap/Secret
     * and for the application.{properties|yaml|yml} files that might be contained in it as well
     *
     * All the {@code ConfigSource} objects use the same ordinal which is higher than the ordinal
     * of normal configuration files, but lower than that of environment variables
     */
    List<ConfigSource> toConfigSources(ObjectMeta metadata, Map<String, String> kubernetesConfigSourceDataMap,
            int ordinalOffset) {
        /*
         * use a name that uniquely identifies the secret/configmap - which can be used an application
         * to fully report its startup state or even respond to secret/configmap changes
         */
        String kubernetesConfigSourceName = metadata.getNamespace() + "/" + metadata.getName() + "/" + metadata.getUid() + "/"
                + metadata.getResourceVersion();
        if (log.isDebugEnabled()) {
            log.debug("Attempting to convert data in " + getType() + " '" + kubernetesConfigSourceName
                    + "' to a list of ConfigSource objects");
        }

        CategorizedConfigSourceData categorizedConfigSourceData = categorize(kubernetesConfigSourceDataMap);
        List<ConfigSource> result = new ArrayList<>(categorizedConfigSourceData.fileData.size() + 1);

        int ordinal = getOrdinal(ordinalOffset);

        if (!categorizedConfigSourceData.literalData.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Adding a ConfigSource for the literal data of " + getType() + " '" + kubernetesConfigSourceName + "'");
            }
            result.add(createLiteralDataConfigSource(kubernetesConfigSourceName,
                    categorizedConfigSourceData.literalData, ordinal));
        }
        for (Map.Entry<String, String> entry : categorizedConfigSourceData.fileData) {
            String fileName = entry.getKey();
            String rawFileData = entry.getValue();
            if (APPLICATION_PROPERTIES.equals(fileName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding a Properties ConfigSource for file '" + fileName + "' of " + getType()
                            + " '" + kubernetesConfigSourceName + "'");
                }
                result.add(createPropertiesConfigSource(kubernetesConfigSourceName, fileName, rawFileData, ordinal));
            } else if (APPLICATION_YAML.equals(fileName) || APPLICATION_YML.equals(fileName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding a YAML ConfigSource for file '" + fileName + "' of " + getType()
                            + " '" + kubernetesConfigSourceName + "'");
                }
                result.add(createYamlConfigSource(kubernetesConfigSourceName, fileName, rawFileData, ordinal));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(getType() + " '" + kubernetesConfigSourceName + "' converted into " + result.size()
                    + "ConfigSource objects");
        }
        return result;
    }

    private int getOrdinal(int ordinalOffset) {
        final OrdinalData ordinalData = ordinalData();
        /*
         * We don't want a large list of sources to cause an "overflow" into an Ordinal of another ConfigSource
         * so we just let the last ones all use the max ordinal
         * this is not fool proof, but it's very unlikely that an application will need to define
         * a list with 10+ sources...
         */
        return Math.min(ordinalData.getBase() + ordinalOffset, ordinalData.getMax());
    }

    private static CategorizedConfigSourceData categorize(Map<String, String> data) {
        if ((data == null) || data.isEmpty()) {
            return new CategorizedConfigSourceData(Collections.emptyMap(), Collections.emptyList());
        }

        Map<String, String> literalData = new HashMap<>();
        List<Map.Entry<String, String>> fileData = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            if ((key.startsWith("application")) &&
                    ((key.endsWith(".yml") || key.endsWith(".yaml") || key.endsWith(".properties")))) {
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
