package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.IndexSettings;

/**
 * @deprecated Use {@link io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchConfigUtil} instead.
 */
@Deprecated
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
}
