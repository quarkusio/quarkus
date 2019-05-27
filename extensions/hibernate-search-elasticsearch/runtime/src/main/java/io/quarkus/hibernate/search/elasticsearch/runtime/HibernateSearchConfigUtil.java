package io.quarkus.hibernate.search.elasticsearch.runtime;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

public class HibernateSearchConfigUtil {

    public static <T> void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, T value) {
        propertyCollector.accept(configKey(configPath), value);
    }

    public static void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, Optional<?> value) {
        if (value.isPresent()) {
            propertyCollector.accept(configKey(configPath), value.get());
        }
    }

    public static <T> void addBackendConfig(BiConsumer<String, Object> propertyCollector, String backendName, String configPath,
            T value) {
        propertyCollector.accept(backendConfigKey(backendName, configPath), value);
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
            propertyCollector.accept(backendConfigKey(backendName, configPath), getValue.apply(value));
        }
    }

    public static void addBackendDefaultIndexConfig(BiConsumer<String, Object> propertyCollector, String backendName,
            String configPath, Optional<?> value) {
        addBackendDefaultIndexConfig(propertyCollector, backendName, configPath, value, Optional::isPresent, Optional::get);
    }

    public static void addBackendDefaultIndexConfig(BiConsumer<String, Object> propertyCollector, String backendName,
            String configPath, OptionalInt value) {
        addBackendDefaultIndexConfig(propertyCollector, backendName, configPath, value, OptionalInt::isPresent,
                OptionalInt::getAsInt);
    }

    public static <T> void addBackendDefaultIndexConfig(BiConsumer<String, Object> propertyCollector, String backendName,
            String configPath, T value,
            Function<T, Boolean> shouldBeAdded, Function<T, ?> getValue) {
        addBackendConfig(propertyCollector, backendName, BackendSettings.INDEX_DEFAULTS + "." + configPath, value,
                shouldBeAdded, getValue);
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
        addBackendConfig(propertyCollector, backendName, BackendSettings.INDEXES + "." + indexName + "." + configPath, value,
                shouldBeAdded, getValue);
    }

    private static String configKey(String configPath) {
        return HibernateOrmMapperSettings.PREFIX + configPath;
    }

    private static String backendConfigKey(String backendName, String configPath) {
        return configKey(EngineSettings.BACKENDS + "." + backendName + "." + configPath);
    }
}
