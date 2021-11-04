package io.quarkus.hibernate.search.elasticsearch.test.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitsCdiTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
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
        try {
            transaction.begin();
            try {
                runnable.run();
                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
            }
        } catch (SystemException | NotSupportedException e) {
            throw new IllegalStateException("Transaction exception", e);
        }
    }
}
