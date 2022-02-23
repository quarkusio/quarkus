package io.quarkus.hibernate.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class MultiLineImportTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .withConfigurationResource("application.properties");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void integerIdentifierWithStageAPI(UniAsserter asserter) {
        asserter.assertThat(() -> sessionFactory.withSession(s -> s.createQuery(
                "from Hero h where h.name = :name").setParameter("name", "Galadriel").getResultList()),
                list -> assertThat(list).hasSize(1));
    }

    @Entity(name = "Hero")
    @Table(name = "hero")
    public static class Hero {

        @javax.persistence.Id
        @javax.persistence.GeneratedValue
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
