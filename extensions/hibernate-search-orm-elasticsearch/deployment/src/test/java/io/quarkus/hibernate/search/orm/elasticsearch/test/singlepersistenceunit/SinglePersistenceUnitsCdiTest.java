package io.quarkus.hibernate.search.orm.elasticsearch.test.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.search.orm.elasticsearch.test.util.TransactionUtils;
import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitsCdiTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TransactionUtils.class)
                    .addClass(DefaultPUEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    SearchMapping defaultPUMapping;

    @Inject
    SearchSession defaultPUSession;

    @Inject
    UserTransaction transaction;

    @Test
    public void testDefaultMapping() {
        assertThat(defaultPUMapping.allIndexedEntities())
                .hasSize(1)
                .element(0)
                .returns(DefaultPUEntity.class, SearchIndexedEntity::javaClass);
    }

    @Test
    @ActivateRequestContext
    public void testDefaultSession() {
        DefaultPUEntity entity = new DefaultPUEntity("someText");
        inTransaction(() -> defaultPUSession.toEntityManager().persist(entity));
        inTransaction(() -> assertThat(defaultPUSession.search(DefaultPUEntity.class)
                .where(f -> f.matchAll())
                .fetchHits(20))
                .hasSize(1)
                .element(0)
                .returns(entity.getId(), DefaultPUEntity::getId));
    }

    private void inTransaction(Runnable runnable) {
        TransactionUtils.inTransaction(transaction, runnable);
    }
}
