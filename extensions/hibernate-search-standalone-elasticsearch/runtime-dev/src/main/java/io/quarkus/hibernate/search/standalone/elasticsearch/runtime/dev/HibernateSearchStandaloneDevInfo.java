package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.dev;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;

class HibernateSearchStandaloneDevInfo {

    private final List<IndexedEntity> indexedEntities;

    public HibernateSearchStandaloneDevInfo(List<IndexedEntity> indexedEntities) {
        this.indexedEntities = indexedEntities;
    }

    public List<IndexedEntity> getIndexedEntities() {
        return indexedEntities;
    }

    public int getNumberOfIndexedEntities() {
        return indexedEntities.size();
    }

    public static class IndexedEntity implements Comparable<IndexedEntity> {

        public final String name;
        public final String javaClass;
        public final Set<String> indexNames = new HashSet<>();

        IndexedEntity(SearchIndexedEntity<?> searchIndexedEntity) {
            this.name = searchIndexedEntity.name();
            this.javaClass = searchIndexedEntity.javaClass().getName();
            ElasticsearchIndexManager indexManager = searchIndexedEntity.indexManager().unwrap(ElasticsearchIndexManager.class);
            indexNames.add(indexManager.descriptor().readName());
            indexNames.add(indexManager.descriptor().writeName());
        }

        @Override
        public int compareTo(IndexedEntity o) {
            return this.name.compareTo(o.name);
        }
    }
}
