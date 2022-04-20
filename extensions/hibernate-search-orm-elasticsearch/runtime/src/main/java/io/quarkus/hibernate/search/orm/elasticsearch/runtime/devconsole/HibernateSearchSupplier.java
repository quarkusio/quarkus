package io.quarkus.hibernate.search.orm.elasticsearch.runtime.devconsole;

import static org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig;

public class HibernateSearchSupplier implements Supplier<HibernateSearchSupplier.IndexedPersistenceUnits> {

    private final HibernateSearchElasticsearchRuntimeConfig runtimeConfig;
    private final Set<String> persistenceUnitNames;

    HibernateSearchSupplier(HibernateSearchElasticsearchRuntimeConfig runtimeConfig, Set<String> persistenceUnitNames) {
        this.runtimeConfig = runtimeConfig;
        this.persistenceUnitNames = persistenceUnitNames;
    }

    @Override
    public IndexedPersistenceUnits get() {
        if (!isEnabled()) {
            return new IndexedPersistenceUnits();
        }
        Map<String, SearchMapping> mappings = searchMapping(persistenceUnitNames);
        if (mappings.isEmpty()) {
            return new IndexedPersistenceUnits();
        }
        return mappings.entrySet().stream()
                .map(mapping -> new IndexedPersistenceUnit(mapping.getKey(),
                        mapping.getValue().allIndexedEntities().stream().map(DevUiIndexedEntity::new).sorted()
                                .collect(Collectors.toList())))
                .collect(Collector.of(IndexedPersistenceUnits::new, IndexedPersistenceUnits::add,
                        (left, right) -> {
                            left.addAll(right);
                            return left;
                        }));
    }

    private boolean isEnabled() {
        return runtimeConfig.defaultPersistenceUnit.enabled;
    }

    public static Map<String, SearchMapping> searchMapping(Set<String> persistenceUnitNames) {
        return Arrays.stream(getPersistenceUnitQualifiers(persistenceUnitNames)).map(
                qualifier -> Arc.container().select(SearchMapping.class, qualifier).get())
                .collect(Collectors.toMap(HibernateSearchSupplier::getPersistenceUnitName, mapping -> mapping));
    }

    private static Annotation[] getPersistenceUnitQualifiers(Set<String> persistenceUnitNames) {
        return persistenceUnitNames.stream().map(PersistenceUnit.PersistenceUnitLiteral::new).toArray(Annotation[]::new);
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

    static class IndexedPersistenceUnits {
        private final Set<IndexedPersistenceUnit> persistenceUnits = new TreeSet<>(new PersistenceUnitComparator());

        public Set<IndexedPersistenceUnit> getPersistenceUnits() {
            return persistenceUnits;
        }

        public void add(IndexedPersistenceUnit indexedPersistenceUnit) {
            persistenceUnits.add(indexedPersistenceUnit);
        }

        public void addAll(IndexedPersistenceUnits right) {
            persistenceUnits.addAll(right.persistenceUnits);
        }

        public int getNumberOfIndexedEntities() {
            return persistenceUnits.stream().mapToInt(pu -> pu.indexedEntities.size()).sum();
        }
    }

    static class PersistenceUnitComparator implements Comparator<IndexedPersistenceUnit> {
        Comparator<String> persistenceUnitNameComparator = new PersistenceUnitUtil.PersistenceUnitNameComparator();

        @Override
        public int compare(IndexedPersistenceUnit o1, IndexedPersistenceUnit o2) {
            return persistenceUnitNameComparator.compare(o1.persistenceUnitName, o2.persistenceUnitName);
        }
    }

    static class IndexedPersistenceUnit implements Comparable<IndexedPersistenceUnit> {
        public final String persistenceUnitName;

        public final List<DevUiIndexedEntity> indexedEntities;

        public IndexedPersistenceUnit(String persistenceUnitName, List<DevUiIndexedEntity> indexedEntities) {
            this.persistenceUnitName = persistenceUnitName;
            this.indexedEntities = indexedEntities;
        }

        @Override
        public int compareTo(IndexedPersistenceUnit o) {
            return this.persistenceUnitName.compareTo(o.persistenceUnitName);
        }
    }

    public static class DevUiIndexedEntity implements Comparable<DevUiIndexedEntity> {

        public final String jpaName;
        public final String javaClass;

        DevUiIndexedEntity(SearchIndexedEntity<?> searchIndexedEntity) {
            this.jpaName = searchIndexedEntity.jpaName();
            this.javaClass = searchIndexedEntity.javaClass().getName();
        }

        @Override
        public int compareTo(DevUiIndexedEntity o) {
            return this.jpaName.compareTo(o.jpaName);
        }
    }
}
