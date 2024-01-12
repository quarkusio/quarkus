package io.quarkus.hibernate.search.orm.outboxpolling.runtime;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;

public final class HibernateSearchOutboxPollingConfigUtil {

    private HibernateSearchOutboxPollingConfigUtil() {
    }

    public static <T> void addCoordinationConfig(BiConsumer<String, Object> propertyCollector,
            String configPath, T value) {
        addCoordinationConfig(propertyCollector, null, configPath, value);
    }

    public static void addCoordinationConfig(BiConsumer<String, Object> propertyCollector, String configPath,
            Optional<?> value) {
        addCoordinationConfig(propertyCollector, null, configPath, value);
    }

    public static <T> void addCoordinationConfig(BiConsumer<String, Object> propertyCollector, String tenantId,
            String configPath, T value) {
        String key = HibernateOrmMapperOutboxPollingSettings.coordinationKey(tenantId, configPath);
        propertyCollector.accept(key, value);
    }

    public static void addCoordinationConfig(BiConsumer<String, Object> propertyCollector, String tenantId, String configPath,
            Optional<?> value) {
        addCoordinationConfig(propertyCollector, tenantId, configPath, value, Optional::isPresent, Optional::get);
    }

    public static void addCoordinationConfig(BiConsumer<String, Object> propertyCollector, String tenantId, String configPath,
            OptionalInt value) {
        addCoordinationConfig(propertyCollector, tenantId, configPath, value, OptionalInt::isPresent, OptionalInt::getAsInt);
    }

    public static <T> void addCoordinationConfig(BiConsumer<String, Object> propertyCollector, String tenantId,
            String configPath, T value, Function<T, Boolean> shouldBeAdded, Function<T, ?> getValue) {
        if (shouldBeAdded.apply(value)) {
            propertyCollector.accept(HibernateOrmMapperOutboxPollingSettings.coordinationKey(tenantId, configPath),
                    getValue.apply(value));
        }
    }

}
