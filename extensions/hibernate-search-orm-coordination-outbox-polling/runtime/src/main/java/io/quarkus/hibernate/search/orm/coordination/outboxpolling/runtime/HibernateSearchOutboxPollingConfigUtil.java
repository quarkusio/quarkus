package io.quarkus.hibernate.search.orm.coordination.outboxpolling.runtime;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;

public final class HibernateSearchOutboxPollingConfigUtil {

    private HibernateSearchOutboxPollingConfigUtil() {
    }

    public static <T> void addCoordinationConfig(BiConsumer<String, Object> propertyCollector, String tenantId,
            String configPath, T value) {
        String key = coordinationKey(tenantId, configPath);
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
            propertyCollector.accept(coordinationKey(tenantId, configPath),
                    getValue.apply(value));
        }
    }

    private static String coordinationKey(String tenantId, String configPath) {
        if (tenantId == null) {
            return HibernateOrmMapperOutboxPollingSettings.PREFIX
                    + HibernateOrmMapperOutboxPollingSettings.Radicals.COORDINATION_PREFIX
                    + configPath;
        } else {
            return HibernateOrmMapperOutboxPollingSettings.coordinationKey(tenantId, configPath);
        }
    }

}
