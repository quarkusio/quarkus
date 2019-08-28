package io.quarkus.it.hibernate.search.elasticsearch.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

@Path("/test/hibernate-search")
public class HibernateSearchTestResource {

    @Inject
    EntityManager entityManager;

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        createPerson("John Irving", "Burlington");
        createPerson("David Lodge", "London");
        createPerson("Paul Auster", "New York");
        createPerson("John Grisham", "Oxford");
    }

    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSearch() {
        SearchSession searchSession = Search.session(entityManager);

        List<Person> person = searchSession.search(Person.class)
                .predicate(f -> f.match().onField("name").matching("john"))
                .sort(f -> f.byField("name_sort"))
                .fetchHits();

        assertEquals(2, person.size());
        assertEquals("John Grisham", person.get(0).getName());
        assertEquals("John Irving", person.get(1).getName());

        person = searchSession.search(Person.class)
                .predicate(f -> f.nested().onObjectField("address").nest(f.match().onField("address.city").matching("london")))
                .sort(f -> f.byField("name_sort"))
                .fetchHits();

        assertEquals(1, person.size());
        assertEquals("David Lodge", person.get(0).getName());

        return "OK";
    }

    @PUT
    @Path("/mass-indexer")
    @Produces(MediaType.TEXT_PLAIN)
    public String testMassIndexer() throws InterruptedException {
        SearchSession searchSession = Search.session(entityManager);

        searchSession.massIndexer().startAndWait();

        return "OK";
    }

    private void createPerson(String name, String city) {
        Address address = new Address(city);
        entityManager.persist(address);

        Person person = new Person(name, address);
        entityManager.persist(person);
    }
}
