package io.quarkus.hibernate.orm.temporal;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.hibernate.audit.AuditLogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class AuditedEntityTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AuditedTestEntity.class, TestChangelog.class))
            .withConfigurationResource("application.properties");

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void testAuditedEntityChangesetQueries() {
        sessionFactory.inTransaction(session -> {
            session.persist(new AuditedTestEntity(1L, "Original Title"));
        });

        sessionFactory.inTransaction(session -> {
            var entity = session.find(AuditedTestEntity.class, 1L);
            entity.setTitle("Updated Title");
        });

        sessionFactory.inTransaction(session -> {
            var auditLog = AuditLogFactory.create(session);
            var changesetIds = auditLog.getChangesets(AuditedTestEntity.class, 1L);
            assertThat(changesetIds).hasSize(2);

            var cs1 = changesetIds.get(0);
            var cs2 = changesetIds.get(1);

            try (var s = sessionFactory.withOptions().atChangeset(cs1).open()) {
                var entity = s.find(AuditedTestEntity.class, 1L);
                assertThat(entity).isNotNull();
                assertThat(entity.getTitle()).isEqualTo("Original Title");
            }

            try (var s = sessionFactory.withOptions().atChangeset(cs2).open()) {
                var entity = s.find(AuditedTestEntity.class, 1L);
                assertThat(entity).isNotNull();
                assertThat(entity.getTitle()).isEqualTo("Updated Title");
            }
        });
    }

    // TODO create ORM Jira: AuditLogFactory.create(SessionFactory) unnecessarily casts to SessionFactoryImplementor,
    //  which fails with CDI proxies. Once fixed, change this test to use AuditLogFactory.create(sessionFactory) directly.
    @Test
    public void testAuditedEntityHistory() {
        sessionFactory.inTransaction(session -> {
            session.persist(new AuditedTestEntity(2L, "History Book"));
        });

        sessionFactory.inTransaction(session -> {
            var entity = session.find(AuditedTestEntity.class, 2L);
            entity.setTitle("Updated History Book");
        });

        sessionFactory.inTransaction(session -> {
            var auditLog = AuditLogFactory.create(session);
            var history = auditLog.getHistory(AuditedTestEntity.class, 2L);
            assertThat(history).hasSize(2);

            var entry1 = history.get(0);
            assertThat(entry1.entity()).isInstanceOf(AuditedTestEntity.class);
            assertThat(entry1.entity().getTitle()).isEqualTo("History Book");
            assertThat(entry1.changeset()).isInstanceOf(TestChangelog.class);

            var entry2 = history.get(1);
            assertThat(entry2.entity().getTitle()).isEqualTo("Updated History Book");
        });
    }

    @Test
    public void testAuditedEntityDelete() {
        sessionFactory.inTransaction(session -> {
            session.persist(new AuditedTestEntity(3L, "To Delete"));
        });

        sessionFactory.inTransaction(session -> {
            var entity = session.find(AuditedTestEntity.class, 3L);
            session.remove(entity);
        });

        sessionFactory.inTransaction(session -> {
            var auditLog = AuditLogFactory.create(session);
            var changesetIds = auditLog.getChangesets(AuditedTestEntity.class, 3L);
            assertThat(changesetIds).hasSize(2);

            try (var s = sessionFactory.withOptions().atChangeset(changesetIds.get(0)).open()) {
                var entity = s.find(AuditedTestEntity.class, 3L);
                assertThat(entity).isNotNull();
                assertThat(entity.getTitle()).isEqualTo("To Delete");
            }

            try (var s = sessionFactory.withOptions().atChangeset(changesetIds.get(1)).open()) {
                var entity = s.find(AuditedTestEntity.class, 3L);
                assertThat(entity).isNull();
            }
        });
    }
}
