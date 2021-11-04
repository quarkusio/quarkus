package io.quarkus.hibernate.orm.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BatchFetchSizeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MainEntity.class)
                    .addClass(OtherEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    Session session;

    @Inject
    UserTransaction transaction;

    @Test
    public void testDefaultBatchFetchSize() throws Exception {
        transaction.begin();
        for (int i = 0; i < 20; i++) {
            MainEntity mainEntity = new MainEntity();
            for (int j = 0; j < 10; j++) {
                OtherEntity otherEntity = new OtherEntity();
                session.persist(otherEntity);
                mainEntity.others.add(otherEntity);
            }
            session.persist(mainEntity);
        }
        transaction.commit();

        transaction.begin();
        List<MainEntity> entities = session.createQuery("from MainEntity", MainEntity.class).list();
        assertThat(entities).allSatisfy(e -> assertThat(Hibernate.isPropertyInitialized(e, "others"))
                .as("'others' initialized for " + e).isFalse());

        MainEntity entity = entities.get(0);
        // Trigger initialization for the collection from one entity.
        entity.others.get(0);
        assertThat(Hibernate.isPropertyInitialized(entity, "others"))
                .as("'others' initialized for " + entity).isTrue();

        // 20 entities were already in the session when the initialization above occurred,
        // so it should have triggered batch initialization of several 'others' collections.
        assertThat(entities).hasSize(20);
        assertThat(entities)
                .filteredOn(e -> Hibernate.isPropertyInitialized(e, "others"))
                .hasSize(16); // Default batch fetch size is 16
        transaction.commit();
    }

}
