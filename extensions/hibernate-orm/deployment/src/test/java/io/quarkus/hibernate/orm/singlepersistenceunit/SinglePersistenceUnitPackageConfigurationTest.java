package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Formatter;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.singlepersistenceunit.entityassignment.excludedpackage.ExcludedEntity;
import io.quarkus.hibernate.orm.singlepersistenceunit.entityassignment.packageincludedthroughconfig.EntityIncludedThroughPackageConfig;
import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitPackageConfigurationTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(EntityIncludedThroughPackageConfig.class.getPackage().getName())
                    .addPackage(ExcludedEntity.class.getPackage().getName()))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.packages",
                    EntityIncludedThroughPackageConfig.class.getPackage().getName())
            // Expect a warning on startup
            .setLogRecordPredicate(
                    record -> record.getMessage().contains("Could not find a suitable persistence unit for model classes"))
            .assertLogRecords(records -> assertThat(records)
                    .as("Warnings on startup")
                    .hasSize(1)
                    .element(0).satisfies(record -> {
                        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
                        assertThat(LOG_FORMATTER.formatMessage(record))
                                .contains(ExcludedEntity.class.getName());
                    }));

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void testIncluded() {
        EntityIncludedThroughPackageConfig entity = new EntityIncludedThroughPackageConfig("default");
        entityManager.persist(entity);

        EntityIncludedThroughPackageConfig retrievedEntity = entityManager.find(EntityIncludedThroughPackageConfig.class,
                entity.id);
        assertEquals(entity.name, retrievedEntity.name);
    }

    @Test
    @Transactional
    public void testExcluded() {
        ExcludedEntity entity = new ExcludedEntity("gsmet");
        assertThatThrownBy(() -> entityManager.persist(entity)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity");
    }
}
