package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

public final class BuiltInReaderOverrideBuildItem extends MultiBuildItem {

    private final String readerClassName;
    private final String overrideClassName;

    public BuiltInReaderOverrideBuildItem(String readerClassName, String overrideClassName) {
        this.readerClassName = readerClassName;
        this.overrideClassName = overrideClassName;
    }

    public String getReaderClassName() {
        return readerClassName;
    }

    public String getOverrideClassName() {
        return overrideClassName;
    }

    public static Map<String, String> toMap(List<BuiltInReaderOverrideBuildItem> items) {
        if (items.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (BuiltInReaderOverrideBuildItem item : items) {
            String previousOverride = result.put(item.getReaderClassName(), item.getOverrideClassName());
            if (previousOverride != null) {
                throw new IllegalStateException(
                        "Providing multiple BuiltInReaderOverrideBuildItem for the same readerClassName is not supported");
            }
        }
        return result;
    }
}
