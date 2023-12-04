package io.quarkus.hibernate.orm.panache.deployment.test.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class RecordInPanacheTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(Person.class, PersonName.class, Status.class));

    @Test
    @Transactional
    void testRecordInPanache() {
        var person1 = new Person();
        person1.firstname = "Loïc";
        person1.lastname = "Mathieu";
        person1.status = Status.ALIVE;
        person1.persist();

        var person2 = new Person();
        person1.firstname = "Zombie";
        person2.lastname = "Zombie";
        person2.status = Status.DEAD;
        person2.persist();

        assertEquals(2L, Person.count());

    }

    @Test
    @Transactional
    void testHqlPanacheProject() {
        var mark = new Person();
        mark.firstname = "Mark";
        mark.lastname = "Mark";
        mark.persistAndFlush();

        var hqlWithoutSpace = """
                select
                    firstname,
                    lastname
                from
                    io.quarkus.hibernate.orm.panache.deployment.test.record.Person
                where
                    firstname = ?1
                """;
        var persistedWithoutSpace = Person.find(hqlWithoutSpace, "Mark").project(PersonName.class).firstResult();
        assertEquals("Mark", persistedWithoutSpace.firstname());

        // We need to escape the whitespace in Java otherwise the compiler removes it.
        var hqlWithSpace = """
                select\s
                    firstname,
                    lastname
                from
                    io.quarkus.hibernate.orm.panache.deployment.test.record.Person
                where
                    firstname = ?1
                """;
        var persistedWithSpace = Person.find(hqlWithSpace, "Mark").project(PersonName.class).firstResult();
        assertEquals("Mark", persistedWithSpace.firstname());
    }
}
