package io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

class HibernateSearchElasticsearchDevInfo {
    private final Set<PersistenceUnit> persistenceUnits = new TreeSet<>(new PersistenceUnitComparator());

    public Set<PersistenceUnit> getPersistenceUnits() {
        return persistenceUnits;
    }

    public void add(PersistenceUnit indexedPersistenceUnit) {
        persistenceUnits.add(indexedPersistenceUnit);
    }

    public void addAll(HibernateSearchElasticsearchDevInfo right) {
        persistenceUnits.addAll(right.persistenceUnits);
    }

    public int getNumberOfIndexedEntities() {
        return persistenceUnits.stream().mapToInt(pu -> pu.indexedEntities.size()).sum();
    }

    static class PersistenceUnitComparator implements Comparator<PersistenceUnit> {
        Comparator<String> persistenceUnitNameComparator = new PersistenceUnitUtil.PersistenceUnitNameComparator();

        @Override
        public int compare(PersistenceUnit o1, PersistenceUnit o2) {
            return persistenceUnitNameComparator.compare(o1.persistenceUnitName, o2.persistenceUnitName);
        }
    }

    static class PersistenceUnit implements Comparable<PersistenceUnit> {
        public final String persistenceUnitName;

        public final List<IndexedEntity> indexedEntities;

        public PersistenceUnit(String persistenceUnitName, List<IndexedEntity> indexedEntities) {
            this.persistenceUnitName = persistenceUnitName;
            this.indexedEntities = indexedEntities;
        }

        @Override
        public int compareTo(PersistenceUnit o) {
            return this.persistenceUnitName.compareTo(o.persistenceUnitName);
        }
    }

    public static class IndexedEntity implements Comparable<IndexedEntity> {

        public final String jpaName;
        public final String javaClass;

        IndexedEntity(SearchIndexedEntity<?> searchIndexedEntity) {
            this.jpaName = searchIndexedEntity.jpaName();
            this.javaClass = searchIndexedEntity.javaClass().getName();
        }

        @Override
        public int compareTo(IndexedEntity o) {
            return this.jpaName.compareTo(o.jpaName);
        }
    }
}
