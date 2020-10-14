package io.quarkus.hibernate.orm.panache.deployment.test.rowmapper;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.quarkus.test.QuarkusUnitTest;

public class RowMapperFunctionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties")
                    .addClasses(Person.class));

    @Transactional
    @Test
    public void testRowMapper() {
        Person person1 = new Person();
        person1.name = "Jo";
        person1.surname = "Ne";
        person1.height = 10;
        Person person2 = new Person();
        person2.name = "Yep";
        person2.surname = "Nope";
        person2.height = 20;
        Person.persist(person1, person2);

        PanacheQuery<Person> query = Person.find("order by name asc").project(asList("name"), result -> {
            Person p = new Person();
            p.name = result[0].toString();
            return p;
        });
        List<Person> names = query.list();
        assertEquals(2, names.size());
        assertEquals("Jo", names.get(0).name);
        assertEquals("Jo", query.firstResult().name);

        List<Integer> heights = Person.find("order by height desc").project(asList("height"), result -> (Integer) result[0])
                .list();

        assertEquals(2, heights.size());
        assertEquals(20, heights.get(0));
        assertEquals(10, heights.get(1));

        List<Person> namesAndSurnames = Person.find("order by height desc")
                .project(asList("name", "surname"), result -> {
                    Person p = new Person();
                    p.name = result[0].toString();
                    p.surname = result[1].toString();
                    return p;
                }).stream().collect(Collectors.toList());

        assertEquals(2, namesAndSurnames.size());
        assertEquals("Yep", namesAndSurnames.get(0).name);
        assertEquals("Nope", namesAndSurnames.get(0).surname);
    }

    @Test
    public void testExistingSelectQuery() {
        try {
            Person.find("order by name asc").project(Person.class).project(asList("name"), result -> null);
            fail();
        } catch (PanacheQueryException expected) {
        }
    }

}
