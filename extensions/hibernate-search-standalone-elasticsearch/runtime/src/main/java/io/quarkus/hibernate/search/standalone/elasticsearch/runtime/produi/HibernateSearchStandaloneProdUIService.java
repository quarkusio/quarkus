package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.produi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;

import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the standalone Hibernate Search indexed entity
 * types. It exposes the indexed entities and their backing index names derived
 * from the always-present {@link SearchMapping} bean. It deliberately omits the
 * Dev UI's mass-indexer / reindex action and exposes no Elasticsearch host or
 * credential information.
 */
@ApplicationScoped
public class HibernateSearchStandaloneProdUIService {

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the standalone Hibernate Search indexed entity types and their index names")
    public List<IndexedEntityInfo> getIndexedEntities() {
        InstanceHandle<SearchMapping> handle = Arc.container().instance(SearchMapping.class);
        if (!handle.isAvailable()) {
            return List.of();
        }
        List<IndexedEntityInfo> entities = new ArrayList<>();
        for (SearchIndexedEntity<?> indexedEntity : handle.get().allIndexedEntities()) {
            TreeSet<String> indexNames = new TreeSet<>();
            ElasticsearchIndexManager indexManager = indexedEntity.indexManager().unwrap(ElasticsearchIndexManager.class);
            indexNames.add(indexManager.descriptor().readName());
            indexNames.add(indexManager.descriptor().writeName());
            entities.add(new IndexedEntityInfo(indexedEntity.name(), indexedEntity.javaClass().getName(),
                    new ArrayList<>(indexNames)));
        }
        entities.sort(Comparator.comparing(IndexedEntityInfo::name));
        return entities;
    }

    public record IndexedEntityInfo(String name, String javaClass, List<String> indexNames) {
    }
}
