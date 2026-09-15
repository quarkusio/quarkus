package io.quarkus.hibernate.search.orm.elasticsearch.runtime.produi;

import static org.hibernate.cfg.AvailableSettings.PERSISTENCE_UNIT_NAME;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the Hibernate Search indexed entity types. For each
 * persistence unit it exposes the indexed entities and their backing index
 * names, derived from the always-present {@link SearchMapping} beans. It
 * deliberately omits the Dev UI's mass-indexer / reindex action and exposes no
 * Elasticsearch host or credential information.
 */
@ApplicationScoped
public class HibernateSearchElasticsearchProdUIService {

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the Hibernate Search indexed entity types and their index names per persistence unit")
    public List<PersistenceUnitInfo> getPersistenceUnits() {
        List<PersistenceUnitInfo> result = new ArrayList<>();
        for (InstanceHandle<SearchMapping> handle : Arc.container()
                .select(SearchMapping.class, Any.Literal.INSTANCE).handles()) {
            if (!handle.isAvailable()) {
                continue;
            }
            SearchMapping mapping = handle.get();
            String name = persistenceUnitName(mapping);
            try {
                result.add(new PersistenceUnitInfo(name, collectIndexedEntities(mapping), null));
            } catch (RuntimeException e) {
                result.add(new PersistenceUnitInfo(name, List.of(), e.getMessage()));
            }
        }
        result.sort(Comparator.comparing(PersistenceUnitInfo::name,
                new PersistenceUnitUtil.PersistenceUnitNameComparator()));
        return result;
    }

    private static List<IndexedEntityInfo> collectIndexedEntities(SearchMapping mapping) {
        List<IndexedEntityInfo> entities = new ArrayList<>();
        for (SearchIndexedEntity<?> indexedEntity : mapping.allIndexedEntities()) {
            TreeSet<String> indexNames = new TreeSet<>();
            ElasticsearchIndexManager indexManager = indexedEntity.indexManager().unwrap(ElasticsearchIndexManager.class);
            indexNames.add(indexManager.descriptor().readName());
            indexNames.add(indexManager.descriptor().writeName());
            entities.add(new IndexedEntityInfo(indexedEntity.jpaName(), indexedEntity.javaClass().getName(),
                    new ArrayList<>(indexNames)));
        }
        entities.sort(Comparator.comparing(IndexedEntityInfo::jpaName));
        return entities;
    }

    private static String persistenceUnitName(SearchMapping mapping) {
        SessionFactoryImplementor sessionFactory = mapping.toOrmSessionFactory().unwrap(SessionFactoryImplementor.class);
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

    public record PersistenceUnitInfo(String name, List<IndexedEntityInfo> indexedEntities, String error) {
    }

    public record IndexedEntityInfo(String jpaName, String javaClass, List<String> indexNames) {
    }
}
