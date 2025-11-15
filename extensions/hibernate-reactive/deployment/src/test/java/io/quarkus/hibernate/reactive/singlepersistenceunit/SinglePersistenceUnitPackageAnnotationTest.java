package io.quarkus.hibernate.reactive.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;
import java.util.logging.Level;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.singlepersistenceunit.entityassignment.excludedpackage.ExcludedEntity;
import io.quarkus.hibernate.reactive.singlepersistenceunit.entityassignment.packageincludedthroughannotation.EntityIncludedThroughPackageAnnotation;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class SinglePersistenceUnitPackageAnnotationTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(EntityIncludedThroughPackageAnnotation.class.getPackage().getName())
                    .addPackage(ExcludedEntity.class.getPackage().getName()))
            .withConfigurationResource("application.properties")
            // Expect a warning on startup
            .setLogRecordPredicate(
                    record -> record.getMessage().contains("Could not find a suitable persistence unit for model classes"))
            .assertLogRecords(records -> assertThat(records)
                    .as("Warnings on startup")
                    .hasSize(1)
                    .element(0).satisfies(record -> {
                        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
                        assertThat(LOG_FORMATTER.formatMessage(record))
                                .contains(
                                        io.quarkus.hibernate.reactive.singlepersistenceunit.entityassignment.excludedpackage.ExcludedEntity.class
                                                .getName());
                    }));

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void testIncluded(UniAsserter asserter) {
        EntityIncludedThroughPackageAnnotation entity = new EntityIncludedThroughPackageAnnotation("default-reactive");
        asserter.assertThat(
                () -> {
                    return persist(entity).chain(() -> {
                        return find(
                                EntityIncludedThroughPackageAnnotation.class,
                                entity.id);
                    });
                },
                retrievedEntity -> assertThat(retrievedEntity.name).isEqualTo(entity.name));
    }

    @Test
    @RunOnVertxContext
    public void testExcluded(UniAsserter asserter) {
        ExcludedEntity entity = new ExcludedEntity("gsmet");
        asserter.assertFailedWith(() -> persist(entity), t -> {
            assertThat(t).hasMessageContaining("Unknown entity type");
        });
    }

    private Uni<Void> persist(Object entity) {
        return sessionFactory.withTransaction(s -> s.persist(entity));
    }

    private <T> Uni<T> find(Class<T> entityClass, Object id) {
        return sessionFactory.withSession(s -> s.find(entityClass, id));
    }
}
