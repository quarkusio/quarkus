package io.quarkus.hibernate.orm.envers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.enhanced.SequenceIdRevisionMapping;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionMapping;
import org.junit.jupiter.api.Test;

import io.quarkus.test.QuarkusExtensionTest;

abstract class AbstractSequenceIdRevisionEntityTest {

    static QuarkusExtensionTest createRunner(boolean trackEntitiesChanged) {
        return new QuarkusExtensionTest()
                .withApplicationRoot(jar -> jar.addClass(MyAuditedEntity.class))
                .overrideConfigKey("quarkus.hibernate-envers.use-revision-entity-with-native-id", "false")
                .overrideConfigKey("quarkus.hibernate-envers.track-entities-changed-in-revision",
                        Boolean.toString(trackEntitiesChanged));
    }

    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction transaction;

    abstract Class<? extends SequenceIdRevisionMapping> revisionEntityClass();

    @Test
    void persistAndReadRevision() throws Exception {
        MyAuditedEntity entity = new MyAuditedEntity();
        entity.setName("audited");
        transaction.begin();
        try {
            entityManager.persist(entity);
            transaction.commit();
        } finally {
            if (transaction.getStatus() == Status.STATUS_ACTIVE) {
                transaction.rollback();
            }
        }

        transaction.begin();
        try {
            AuditReader reader = AuditReaderFactory.get(entityManager);
            List<Number> revisions = reader.getRevisions(MyAuditedEntity.class, entity.getId());
            assertThat(revisions).hasSize(1);
            SequenceIdRevisionMapping revision = reader.findRevision(revisionEntityClass(), revisions.get(0));
            assertThat(revision).isExactlyInstanceOf(revisionEntityClass());
            assertThat(revision.getId()).isEqualTo(revisions.get(0).intValue());
            assertThat(revision.getTimestamp()).isPositive();
            assertThat(reader.find(MyAuditedEntity.class, entity.getId(), revisions.get(0)).getName()).isEqualTo("audited");
            if (revision instanceof SequenceIdTrackingModifiedEntitiesRevisionMapping trackingRevision) {
                assertThat(trackingRevision.getModifiedEntityNames()).containsExactly(MyAuditedEntity.class.getName());
            }
        } finally {
            transaction.rollback();
        }
    }
}
