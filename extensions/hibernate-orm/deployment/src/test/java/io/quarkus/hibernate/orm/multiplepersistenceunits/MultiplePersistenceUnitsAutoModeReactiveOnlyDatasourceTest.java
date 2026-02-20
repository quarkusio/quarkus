package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.shared.SharedEntity;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsAutoModeReactiveOnlyDatasourceTest {

    static final String QUARKUS_VERSION = System.getProperty("project.version", "999-SNAPSHOT");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-reactive-pg-client", QUARKUS_VERSION)))
            .withApplicationRoot((jar) -> jar
                    .addClass(SharedEntity.class)
                    .addAsResource("application-multiple-persistence-units-mode-auto-reactive-only.properties",
                            "application.properties"));

    @Test
    public void autoModeReactiveOnlyShouldSkipBlockingUsersPu() {
        assertTrue(CDI.current().select(EntityManager.class).isResolvable());
        assertTrue(CDI.current().select(EntityManager.class, new PersistenceUnitLiteral("users")).isUnsatisfied());
    }

    static final class PersistenceUnitLiteral extends AnnotationLiteral<PersistenceUnit> implements PersistenceUnit {
        private final String value;

        PersistenceUnitLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}