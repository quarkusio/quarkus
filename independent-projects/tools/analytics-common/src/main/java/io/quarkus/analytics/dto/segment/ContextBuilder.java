package io.quarkus.analytics.dto.segment;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ContextBuilder {

    public static final String PROP_NAME = "name";
    public static final String PROP_VERSION = "version";
    public static final String PROP_APP = "app";
    public static final String PROP_IP = "ip";
    public static final String PROP_LOCALE_COUNTRY = "locale_country";
    public static final String PROP_LOCATION = "location";
    public static final String PROP_OS = "os";
    public static final String PROP_OS_ARCH = "os_arch";
    public static final String PROP_TIMEZONE = "timezone";

    /**
     * Must not track ips.
     * We don't want the server side to try to infer the IP if the field is not present in the payload.
     * Sending invalid data is safer and makes sure it's really anonymous.
     */
    public static final String VALUE_NULL_IP = "0.0.0.0";

    public static final String PROP_JAVA = "java";
    public static final String PROP_VENDOR = "vendor";
    public static final String PROP_GRAALVM = "graalvm";
    public static final String PROP_JAVA_VERSION = "java_version";

    public static final String PROP_BUILD = "build";
    public static final String PROP_MAVEN_VERSION = "maven_version";
    public static final String PROP_GRADLE_VERSION = "gradle_version";

    public static final String PROP_QUARKUS = "quarkus";

    public static final String PROP_CI = "ci";
    public static final String PROP_CI_NAME = "name";
    public static final String PROP_KUBERNETES = "kubernetes";
    public static final String PROP_DETECTED = "detected";

    private final Map<String, Object> map = new HashMap<>();

    public ContextBuilder pair(String key, String value) {
        map.put(key, value);
        return this;
    }

    public ContextBuilder pair(String key, Object value) {
        map.put(key, value);
        return this;
    }

    public ContextBuilder pairs(Collection<AbstractMap.SimpleEntry<String, Object>> entries) {
        if (entries == null) {
            return this;
        }
        entries.stream().forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        return this;
    }

    public MapValueBuilder mapPair(String key) {
        return new MapValueBuilder(key);
    }

    public Map<String, Object> build() {
        return map;
    }

    public class MapValueBuilder {
        private final Map<String, Object> map = new HashMap<>();
        private final String key;

        private MapValueBuilder(String key) {
            this.key = key;
        }

        public MapValueBuilder pair(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public MapValueBuilder pairs(Collection<AbstractMap.SimpleEntry<String, Object>> entries) {
            if (entries == null) {
                return this;
            }
            entries.stream().forEach(entry -> map.put(entry.getKey(), entry.getValue()));
            return this;
        }

        public ContextBuilder build() {
            ContextBuilder.this.pair(key, map);
            return ContextBuilder.this;
        }
    }

    public static class CommonSystemProperties {
        public static final String APP_NAME = "app.name";
        public static final String MAVEN_VERSION = "maven.version";
        public static final String GRADLE_VERSION = "gradle.version";
        public static final String QUARKUS_VERSION = "quarkus.version";
        public static final String GRAALVM_VERSION_VERSION = "graalvm.version.version";
        public static final String GRAALVM_VERSION_JAVA = "graalvm.version.java";
        public static final String GRAALVM_VERSION_DISTRIBUTION = "graalvm.version.distribution";
    }

}
