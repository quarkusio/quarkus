package io.quarkus.hibernate.search.standalone.elasticsearch.test.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that an application can be configured to start successfully
 * even if the Elasticsearch cluster is offline when the application starts.
 */
public class StartOfflineTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(IndexedEntity.class)
                    .addAsResource("application-start-offline.properties", "application.properties"));

    @Inject
    SearchMapping searchMapping;

    @Test
    public void testHibernateSearchStarted() {
        assertThat(searchMapping.allIndexedEntities())
                .hasSize(1)
                .element(0)
                .returns(IndexedEntity.class, SearchIndexedEntity::javaClass);
    }

    @Test
    public void testSchemaManagementAvailableButFailsSinceElasticsearchNotStarted() {
        try (var searchSession = searchMapping.createSession()) {
            assertThatThrownBy(() -> searchSession.schemaManager(IndexedEntity.class).createIfMissing())
                    .isInstanceOf(SearchException.class)
                    .hasMessageContaining("Elasticsearch request failed: Connection refused");
        }
    }

    @Test
    public void testSearchAvailableButFailsSinceElasticsearchNotStarted() {
        try (var searchSession = searchMapping.createSession()) {
            assertThatThrownBy(() -> searchSession.search(IndexedEntity.class)
                    .where(f -> f.matchAll()).fetchHits(20))
                    .isInstanceOf(SearchException.class)
                    .hasMessageContaining("Elasticsearch request failed: Connection refused");
        }
    }

}
