package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsReactiveModeSkipsBlockingPuTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-multiple-persistence-units-mode-reactive-skip.properties",
                            "application.properties"));

    @Test
    public void reactiveModeShouldAlwaysSkipBlockingUsersPu() {
        // Even if JDBC is configured for users, mode=REACTIVE must prevent creating a blocking PU.
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