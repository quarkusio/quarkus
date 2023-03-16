package io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev;

import static org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public class HibernateSearchElasticsearchDevController {

    private static final HibernateSearchElasticsearchDevController INSTANCE = new HibernateSearchElasticsearchDevController();

    public static HibernateSearchElasticsearchDevController get() {
        return INSTANCE;
    }

    private final Set<String> activePersistenceUnitNames = new HashSet<>();

    private HibernateSearchElasticsearchDevController() {
    }

    void setActivePersistenceUnitNames(Set<String> activePersistenceUnitNames) {
        this.activePersistenceUnitNames.clear();
        this.activePersistenceUnitNames.addAll(activePersistenceUnitNames);
    }

    public HibernateSearchElasticsearchDevInfo getInfo() {
        Map<String, SearchMapping> mappings = searchMapping(activePersistenceUnitNames);
        if (mappings.isEmpty()) {
            return new HibernateSearchElasticsearchDevInfo();
        }
        return mappings.entrySet().stream()
                .map(mapping -> new HibernateSearchElasticsearchDevInfo.PersistenceUnit(mapping.getKey(),
                        mapping.getValue().allIndexedEntities().stream()
                                .map(HibernateSearchElasticsearchDevInfo.IndexedEntity::new).sorted()
                                .collect(Collectors.toList())))
                .collect(Collector.of(HibernateSearchElasticsearchDevInfo::new, HibernateSearchElasticsearchDevInfo::add,
                        (left, right) -> {
                            left.addAll(right);
                            return left;
                        }));
    }

    public Map<String, SearchMapping> searchMapping(Set<String> persistenceUnitNames) {
        return Arrays.stream(getPersistenceUnitQualifiers(persistenceUnitNames)).map(
                qualifier -> Arc.container().select(SearchMapping.class, qualifier).get())
                .collect(Collectors.toMap(HibernateSearchElasticsearchDevController::getPersistenceUnitName,
                        mapping -> mapping));
    }

    private static Annotation[] getPersistenceUnitQualifiers(Set<String> persistenceUnitNames) {
        return persistenceUnitNames.stream().map(io.quarkus.hibernate.orm.PersistenceUnit.PersistenceUnitLiteral::new)
                .toArray(Annotation[]::new);
    }

    private static String getPersistenceUnitName(SearchMapping searchMapping) {
        SessionFactoryImplementor sessionFactory = searchMapping.toOrmSessionFactory().unwrap(SessionFactoryImplementor.class);
        String name = sessionFactory.getName();
        if (name != null) {
            return name;
        }
        Object persistenceUnitName = sessionFactory.getProperties().get(PERSISTENCE_UNIT_NAME);
        if (persistenceUnitName != null) {
            return persistenceUnitName.toString();
        }
        return PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
    }

}
