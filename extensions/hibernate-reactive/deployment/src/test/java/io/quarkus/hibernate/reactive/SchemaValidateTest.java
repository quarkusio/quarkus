package io.quarkus.hibernate.reactive;

import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SchemaValidateTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Hero.class))
            .withConfigurationResource("application.properties")
            .assertException(SchemaValidateTest::isSchemaValidationException)
            .overrideConfigKey("quarkus.hibernate-orm.database.generation", "validate");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void testSchemaValidationException() {
        Assertions.fail("We expect an exception because the db is empty");
    }

    private static void isSchemaValidationException(Throwable t) {
        Throwable cause = t;
        while (cause != null && !cause.getClass().getName().equals(SchemaManagementException.class.getName())) {
            cause = cause.getCause();
        }
        String causeName = cause != null ? cause.getClass().getName() : null;
        Assertions.assertEquals(SchemaManagementException.class.getName(), causeName);
        Assertions.assertTrue(cause.getMessage().contains("Schema-validation: missing table [" + Hero.TABLE + "]"));
    }

    @Entity(name = "Hero")
    @Table(name = Hero.TABLE)
    public static class Hero {

        public static final String TABLE = "Hero_for_validation";

        @jakarta.persistence.Id
        @jakarta.persistence.GeneratedValue
        public java.lang.Long id;

        @Column(unique = true)
        public String name;

        public String otherName;

        public int level;

        public String picture;

        @Column(columnDefinition = "TEXT")
        public String powers;

    }
}
