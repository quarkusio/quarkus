package io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
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
            return persistenceUnitNameComparator.compare(o1.name, o2.name);
        }
    }

    static class PersistenceUnit implements Comparable<PersistenceUnit> {
        public final String name;

        public final List<IndexedEntity> indexedEntities;

        public PersistenceUnit(String name, List<IndexedEntity> indexedEntities) {
            this.name = name;
            this.indexedEntities = indexedEntities;
        }

        @Deprecated // Only useful for the legacy Dev UI
        public String getPersistenceUnitName() {
            return name;
        }

        @Override
        public int compareTo(PersistenceUnit o) {
            return this.name.compareTo(o.name);
        }
    }

    public static class IndexedEntity implements Comparable<IndexedEntity> {

        public final String jpaName;
        public final String javaClass;
        public final Set<String> indexNames = new HashSet<>();

        IndexedEntity(SearchIndexedEntity<?> searchIndexedEntity) {
            this.jpaName = searchIndexedEntity.jpaName();
            this.javaClass = searchIndexedEntity.javaClass().getName();
            ElasticsearchIndexManager indexManager = searchIndexedEntity.indexManager()
                    .unwrap(ElasticsearchIndexManager.class);
            indexNames.add(indexManager.descriptor().readName());
            indexNames.add(indexManager.descriptor().writeName());
        }

        @Override
        public int compareTo(IndexedEntity o) {
            return this.jpaName.compareTo(o.jpaName);
        }
    }
}
