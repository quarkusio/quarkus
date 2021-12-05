package io.quarkus.hibernate.orm.multiplepersistenceunits;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsClassLevelAnnotationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setExpectedException(IllegalStateException.class)
            .withApplicationRoot((jar) -> jar
                    .addClass(EntityWithClassLevelPersistenceUnit.class)
                    .addAsResource("application-multiple-persistence-units.properties",
                            "application.properties"));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

    @Entity
    @PersistenceUnit("inventory")
    public static class EntityWithClassLevelPersistenceUnit {

        private long id;

        private String name;

        public EntityWithClassLevelPersistenceUnit() {
        }

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "planeSeq")
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
