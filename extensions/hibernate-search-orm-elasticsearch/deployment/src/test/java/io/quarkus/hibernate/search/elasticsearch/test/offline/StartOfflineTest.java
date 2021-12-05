package io.quarkus.hibernate.search.elasticsearch.test.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
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

    @Inject
    SearchSession searchSession;

    @Test
    public void testHibernateSearchStarted() {
        assertThat(searchMapping.allIndexedEntities())
                .hasSize(1)
                .element(0)
                .returns(IndexedEntity.class, SearchIndexedEntity::javaClass);
    }

    @Test
    @Transactional
    public void testSchemaManagementAvailableButFailsSinceElasticsearchNotStarted() {
        assertThatThrownBy(() -> searchSession.schemaManager(IndexedEntity.class).createIfMissing())
                .isInstanceOf(SearchException.class)
                .hasMessageContaining("Elasticsearch request failed: Connection refused");
    }

    @Test
    @Transactional
    public void testSearchAvailableButFailsSinceElasticsearchNotStarted() {
        assertThatThrownBy(() -> searchSession.search(IndexedEntity.class)
                .where(f -> f.matchAll()).fetchHits(20))
                        .isInstanceOf(SearchException.class)
                        .hasMessageContaining("Elasticsearch request failed: Connection refused");
    }

}
