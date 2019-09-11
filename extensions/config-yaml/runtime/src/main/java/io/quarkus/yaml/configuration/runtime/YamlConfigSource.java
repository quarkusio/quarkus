package io.quarkus.yaml.configuration.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.yaml.snakeyaml.Yaml;

public class YamlConfigSource implements ConfigSource, Serializable {

    private static final long serialVersionUID = -875865410658150582L;

    private static final String CONFIG_ORDINAL_KEY = "config_ordinal";
    private static final String CONFIG_ORDINAL_DEFAULT_VALUE = "100";
    private static final String PROPERTY_KEY_SEPARATOR = ".";
    private static final String COMMA = ",";
    private static final String ESCAPED_COMMA = "\\,";

    private final URL source;
    private final int ordinal;
    private final Map<String, String> properties;

    public YamlConfigSource(URL source, int ordinal) {
        this.source = source;
        this.properties = buildConfigPropertiesFromYaml();
        if (properties.containsKey(CONFIG_ORDINAL_KEY)) {
            this.ordinal = Integer.parseInt(properties.getOrDefault(CONFIG_ORDINAL_KEY, CONFIG_ORDINAL_DEFAULT_VALUE));
        } else {
            this.ordinal = ordinal;
        }
    }

    private Map<String, String> buildConfigPropertiesFromYaml() {
        if (source == null) {
            return Collections.emptyMap();
        }
        try (InputStream is = source.openStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlContent = new Yaml().loadAs(is, TreeMap.class);
            Map<String, String> configProperties = new TreeMap<>();
            convertYamlMapToProperties(configProperties, "", yamlContent); // The root `baseKey` is empty.
            return configProperties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void convertYamlMapToProperties(Map<String, String> properties, String baseKey, Object object) {
        if (object instanceof Map) {
            for (Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
                String entryKey = baseKey + (baseKey.isEmpty() ? "" : PROPERTY_KEY_SEPARATOR) + entry.getKey();
                convertYamlMapToProperties(properties, entryKey, entry.getValue());
            }
        } else {
            String value;
            if (object instanceof List) {
                value = stringifyList(object);
            } else {
                value = String.valueOf(object);
            }
            properties.put(baseKey, value);
        }
    }

    private String stringifyList(Object list) {
        StringJoiner stringifiedElements = new StringJoiner(COMMA);
        for (Object elem : (List<?>) list) {
            String stringifiedElem = String.valueOf(elem);
            if (stringifiedElem.contains(COMMA)) {
                stringifiedElem = stringifiedElem.replace(COMMA, ESCAPED_COMMA);
            }
            stringifiedElements.add(stringifiedElem);
        }
        return stringifiedElements.toString();
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String getValue(String key) {
        return properties.get(key);
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String getName() {
        return "YamlConfigSource[source=" + source + "]";
    }

    @Override
    public String toString() {
        return getName();
    }
}
