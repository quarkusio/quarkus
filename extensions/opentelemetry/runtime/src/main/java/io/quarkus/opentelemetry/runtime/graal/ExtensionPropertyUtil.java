package io.quarkus.opentelemetry.runtime.graal;

import java.util.Map;

import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExtensionPropertyUtil")
final class ExtensionPropertyUtil {

    // Upstream's any-setter, which every model setExtensionProperty call reaches. It converts a known typed property
    // with Jackson 2's ModelMapper, which the declarative-config artifact cannot link without Jackson. Quarkus sets
    // only unknown extension properties, which take the put path, so a value that would need converting fails here.
    @Substitute
    public static void handleAnySetter(String key, Object value, Map<String, Object> extensionProperties,
            Map<String, Class<?>> experimentalProperties, Map<String, Class<?>> stableProperties,
            boolean allowsAdditionalProperties) {
        if (key.endsWith("/development")) {
            String stableKey = key.substring(0, key.length() - "/development".length());
            Class<?> stableType = stableProperties.get(stableKey);
            if (stableType != null) {
                Logger.getLogger("io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExtensionPropertyUtil")
                        .warn("Property '" + key + "' has been stabilized. Use '" + stableKey + "' instead.");
                extensionProperties.put(key, ExtensionPropertyConversion.convert(key, value, stableType));
                return;
            }
        }
        Class<?> type = experimentalProperties.get(key);
        if (type != null) {
            extensionProperties.put(key, ExtensionPropertyConversion.convert(key, value, type));
            return;
        }
        if (allowsAdditionalProperties) {
            extensionProperties.put(key, value);
            return;
        }
        Logger.getLogger("io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExtensionPropertyUtil")
                .warn("Unknown property '" + key + "' is not recognized and will be ignored.");
    }
}

// Not part of the substitution: GraalVM rejects unannotated methods in a @TargetClass class
final class ExtensionPropertyConversion {

    private ExtensionPropertyConversion() {
    }

    static Object convert(String key, Object value, Class<?> type) {
        if (value == null || type.isInstance(value)) {
            return value;
        }
        throw new IllegalStateException(
                "Property '" + key + "' needs a " + type.getName() + " value; converting it needs Jackson 2");
    }
}
