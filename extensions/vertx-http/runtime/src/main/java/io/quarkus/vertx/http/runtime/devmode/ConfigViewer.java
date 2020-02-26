package io.quarkus.vertx.http.runtime.devmode;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.configuration.ExpandingConfigSource;

/**
 * Turns a {@link Config} into a JSON object with all config sources and properties as JSON. The config sources are
 * sorted descending by ordinal, the properties by name. If no config is defined an empty JSON object is returned.
 *
 * <p>
 * A typical output might look like:
 * </p>
 *
 * <pre>
 * {
 *   "sources": [
 *     {
 *       "source": "source0",
 *       "ordinal": 200,
 *       "properties": {
 *         "key": "value"
 *       }
 *     },
 *     {
 *       "source": "source1",
 *       "ordinal": 100,
 *       "properties": {
 *         "key": "value"
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
class ConfigViewer {

    private static final Logger LOGGER = Logger.getLogger(ConfigViewer.class.getName());

    String dump(Config config) {
        JsonObject json = new JsonObject();
        if (config != null) {
            boolean old = ExpandingConfigSource.setExpanding(false);
            try {
                if (config.getConfigSources().iterator().hasNext()) {
                    JsonArray jsonSources = new JsonArray();
                    for (ConfigSource source : config.getConfigSources()) {
                        JsonObject jsonSource = new JsonObject();
                        jsonSource.put("source", source.getName())
                                .put("ordinal", source.getOrdinal());
                        Set<String> propertyNames = source.getPropertyNames();
                        if (!propertyNames.isEmpty()) {
                            SortedSet<String> sortedPropertyNames = new TreeSet<>(propertyNames);
                            JsonObject jsonProperties = new JsonObject();
                            for (String propertyName : sortedPropertyNames) {
                                try {
                                    jsonProperties.put(propertyName, source.getValue(propertyName));
                                } catch (Throwable t) {
                                    LOGGER.errorf("Cannot get configuration value for '%s': %s",
                                            propertyName, t.getMessage());
                                }
                            }
                            jsonSource.put("properties", jsonProperties);
                        }
                        jsonSources.put(jsonSource);
                    }
                    json.put("sources", jsonSources);
                }
            } finally {
                ExpandingConfigSource.setExpanding(old);
            }
        }
        return json.toString(2);
    }
}
