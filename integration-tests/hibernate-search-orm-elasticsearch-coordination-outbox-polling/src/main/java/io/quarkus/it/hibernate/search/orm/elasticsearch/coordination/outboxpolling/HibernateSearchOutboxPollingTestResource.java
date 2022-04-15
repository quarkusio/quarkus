package io.quarkus.it.hibernate.search.orm.elasticsearch.coordination.outboxpolling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

@Path("/test/hibernate-search-outbox-polling")
public class HibernateSearchOutboxPollingTestResource {

    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction userTransaction;

    @PUT
    @Path("/check-agents-running")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkAgentsRunning() {
        OutboxPollingTestUtils.awaitAgentsRunning(entityManager, userTransaction, 1);
        return "OK";
    }

    @PUT
    @Path("/init-data")
    @Transactional
    public void initData() {
        createPerson("John Irving");
        createPerson("David Lodge");
        createPerson("Paul Auster");
        createPerson("John Grisham");

        // Add many other entities, so that mass indexing has something to do.
        // DO NOT REMOVE, it's important to have many entities to fully test mass indexing.
        for (int i = 0; i < 1000; i++) {
            createPerson("Other entity #" + i);
        }
    }

    @PUT
    @Path("/await-event-processing")
    public void awaitEventProcessing() {
        OutboxPollingTestUtils.awaitNoMoreOutboxEvents(entityManager, userTransaction);
    }

    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testSearch() {
        SearchSession searchSession = Search.session(entityManager);

        List<Person> person = searchSession.search(Person.class)
                .where(f -> f.match().field("name").matching("john"))
                .sort(f -> f.field("name_sort"))
                .fetchHits(20);

        assertEquals(2, person.size());
        assertEquals("John Grisham", person.get(0).getName());
        assertEquals("John Irving", person.get(1).getName());

        assertEquals(4 + 1000, searchSession.search(Person.class)
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
    @Transactional
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

    private void createPerson(String name) {
        Person entity = new Person(name);
        entityManager.persist(entity);
    }
}
