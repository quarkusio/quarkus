package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.IndexSettings;

public class HibernateSearchConfigUtil {

    public static <T> void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, T value) {
        propertyCollector.accept(configPath, value);
    }

    public static void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, Optional<?> value) {
        if (value.isPresent()) {
            propertyCollector.accept(configPath, value.get());
        }
    }

    public static <T> void addBackendConfig(BiConsumer<String, Object> propertyCollector, String backendName, String configPath,
            T value) {
        propertyCollector.accept(BackendSettings.backendKey(backendName, configPath), value);
    }

    public static void addBackendConfig(BiConsumer<String, Object> propertyCollector, String backendName, String configPath,
            Optional<?> value) {
        addBackendConfig(propertyCollector, backendName, configPath, value, Optional::isPresent, Optional::get);
    }

    public static void addBackendConfig(BiConsumer<String, Object> propertyCollector, String backendName, String configPath,
            OptionalInt value) {
        addBackendConfig(propertyCollector, backendName, configPath, value, OptionalInt::isPresent, OptionalInt::getAsInt);
    }

    public static <T> void addBackendConfig(BiConsumer<String, Object> propertyCollector, String backendName, String configPath,
            T value,
            Function<T, Boolean> shouldBeAdded, Function<T, ?> getValue) {
        if (shouldBeAdded.apply(value)) {
            addBackendConfig(propertyCollector, backendName, configPath, getValue.apply(value));
        }
    }

    public static void addBackendIndexConfig(BiConsumer<String, Object> propertyCollector, String backendName,
            String indexName, String configPath, Optional<?> value) {
        addBackendIndexConfig(propertyCollector, backendName, indexName, configPath, value, Optional::isPresent, Optional::get);
    }

    public static void addBackendIndexConfig(BiConsumer<String, Object> propertyCollector, String backendName,
            String indexName, String configPath, OptionalInt value) {
        addBackendIndexConfig(propertyCollector, backendName, indexName, configPath, value, OptionalInt::isPresent,
                OptionalInt::getAsInt);
    }

    public static <T> void addBackendIndexConfig(BiConsumer<String, Object> propertyCollector, String backendName,
            String indexName, String configPath, T value,
            Function<T, Boolean> shouldBeAdded, Function<T, ?> getValue) {
        if (shouldBeAdded.apply(value)) {
            if (indexName != null) {
                propertyCollector.accept(
                        IndexSettings.indexKey(backendName, indexName, configPath), getValue.apply(value));
            } else {
                addBackendConfig(propertyCollector, backendName, configPath, getValue.apply(value));
            }
        }
    }

    public static void mergeInto(Map<String, Set<String>> target, Map<String, Set<String>> source) {
        for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
            mergeInto(target, entry.getKey(), entry.getValue());
        }
    }

    public static void mergeInto(Map<String, Set<String>> target, String key, Set<String> values) {
        target.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                .addAll(values);
    }
}
