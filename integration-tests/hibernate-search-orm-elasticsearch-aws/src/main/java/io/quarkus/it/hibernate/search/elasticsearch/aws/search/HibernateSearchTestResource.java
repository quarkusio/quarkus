package io.quarkus.it.hibernate.search.elasticsearch.aws.search;

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

        // Add many other entities, so that mass indexing has something to do.
        // DO NOT REMOVE, it's important to have many entities to fully test mass indexing.
        for (int i = 0; i < 2000; i++) {
            createPerson("Other Person #" + i, "Other City #" + i);
        }
    }

    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSearch() {
        SearchSession searchSession = Search.session(entityManager);

        List<Person> person = searchSession.search(Person.class)
                .where(f -> f.match().field("name").matching("john"))
                .sort(f -> f.field("name_sort"))
                .fetchHits(20);

        assertEquals(2, person.size());
        assertEquals("John Grisham", person.get(0).getName());
        assertEquals("John Irving", person.get(1).getName());

        person = searchSession.search(Person.class)
                .where(f -> f.nested().objectField("address").nest(
                        f.match().field("address.city").matching("london")))
                .sort(f -> f.field("name_sort"))
                .fetchHits(20);

        assertEquals(1, person.size());
        assertEquals("David Lodge", person.get(0).getName());

        assertEquals(4 + 2000, searchSession.search(Person.class)
                .where(f -> f.matchAll())
                .fetchTotalHitCount());

        return "OK";
    }

    @PUT
    @Path("/purge")
    @Produces(MediaType.TEXT_PLAIN)
    public String testPurge() {
        SearchSession searchSession = Search.session(entityManager);

        searchSession.workspace().purge();

        return "OK";
    }

    @PUT
    @Path("/refresh")
    @Produces(MediaType.TEXT_PLAIN)
    public String testRefresh() {
        SearchSession searchSession = Search.session(entityManager);

        searchSession.workspace().refresh();

        return "OK";
    }

    @GET
    @Path("/search-empty")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSearchEmpty() {
        SearchSession searchSession = Search.session(entityManager);

        List<Person> person = searchSession.search(Person.class)
                .where(f -> f.matchAll())
                .fetchHits(20);

        assertEquals(0, person.size());

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
