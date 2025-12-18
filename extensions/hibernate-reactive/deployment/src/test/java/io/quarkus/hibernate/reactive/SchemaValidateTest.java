package io.quarkus.hibernate.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.assertj.core.api.InstanceOfAssertFactories;
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
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "validate");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void testSchemaValidationException() {
        Assertions.fail("We expect an exception because the db is empty");
    }

    private static void isSchemaValidationException(Throwable t) {
        assertThat(t)
                .extracting(SchemaValidateTest::getSelfAndCauses, InstanceOfAssertFactories.stream(Throwable.class))
                .anySatisfy(e -> assertThat(e)
                        .hasMessageContaining("Schema validation: missing table [" + Hero.TABLE + "]")
                        .extracting(e2 -> e2.getClass().getName()).isEqualTo(SchemaManagementException.class.getName()));
    }

    private static Stream<Throwable> getSelfAndCauses(Throwable e) {
        return Stream.iterate(e, Objects::nonNull, Throwable::getCause);
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
