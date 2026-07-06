package io.quarkus.gradle.tasks;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.util.StringUtil;

final class AdditionalForcedProperties {

    private static final String NATIVE_PROPERTY_NAMESPACE = "quarkus.native";

    private AdditionalForcedProperties() {
    }

    static Map<String, String> of(Map<String, String> nativeArguments, Map<String, String> taskProperties) {
        Map<String, String> result = new HashMap<>();
        nativeArguments.forEach((key, value) -> result.put(expandNativeArgumentKey(key), value));
        result.putAll(taskProperties);
        return result;
    }

    static String expandNativeArgumentKey(String key) {
        String hyphenatedKey = StringUtil.hyphenate(key);
        if (hyphenatedKey.startsWith(NATIVE_PROPERTY_NAMESPACE)) {
            return hyphenatedKey;
        }
        return NATIVE_PROPERTY_NAMESPACE + "." + hyphenatedKey;
    }
}
