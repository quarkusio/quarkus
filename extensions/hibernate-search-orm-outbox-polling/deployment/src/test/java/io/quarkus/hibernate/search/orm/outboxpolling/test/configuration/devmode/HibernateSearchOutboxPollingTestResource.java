package io.quarkus.hibernate.search.orm.outboxpolling.test.configuration.devmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.OutboxPollingAgentAdditionalMappingProducer;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalMappingProducer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

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

    @Entity
    @Indexed
    public static class Person {

        @Id
        @GeneratedValue
        private Long id;

        @FullTextField
        @KeywordField(name = "name_sort", sortable = Sortable.YES)
        private String name;

        Person() {
        }

        public Person(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class OutboxPollingTestUtils {

        public static void inTransaction(UserTransaction transaction, Runnable runnable) {
            try {
                transaction.begin();
                try {
                    runnable.run();
                    transaction.commit();
                } catch (Throwable t) {
                    try {
                        transaction.rollback();
                    } catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                    throw t;
                }
            } catch (SystemException | NotSupportedException | RollbackException | HeuristicMixedException
                    | HeuristicRollbackException e) {
                throw new IllegalStateException("Transaction exception", e);
            }
        }

        public static void awaitAgentsRunning(EntityManager entityManager, UserTransaction userTransaction,
                int expectedAgentCount) {
            await("Waiting for the formation of a cluster of " + expectedAgentCount + " agents")
                    .pollDelay(Duration.ZERO)
                    .pollInterval(Duration.ofMillis(5))
                    .atMost(Duration.ofSeconds(10)) // CI can be rather slow...
                    .untilAsserted(() -> inTransaction(userTransaction, () -> {
                        List<Agent> agents = entityManager
                                .createQuery("select a from " + OutboxPollingAgentAdditionalMappingProducer.ENTITY_NAME
                                        + " a order by a.id", Agent.class)
                                .getResultList();
                        assertThat(agents)
                                .hasSize(expectedAgentCount)
                                .allSatisfy(agent -> {
                                    assertThat(agent.getState()).isEqualTo(AgentState.RUNNING);
                                    assertThat(agent.getTotalShardCount()).isEqualTo(expectedAgentCount);
                                });
                    }));
        }

        public static void awaitNoMoreOutboxEvents(EntityManager entityManager, UserTransaction userTransaction) {
            await("Waiting for all outbox events to be processed")
                    .pollDelay(Duration.ZERO)
                    .pollInterval(Duration.ofMillis(5))
                    .atMost(Duration.ofSeconds(20)) // CI can be rather slow...
                    .untilAsserted(() -> inTransaction(userTransaction, () -> {
                        List<OutboxEvent> events = entityManager
                                .createQuery("select e from " + OutboxPollingOutboxEventAdditionalMappingProducer.ENTITY_NAME
                                        + " e order by e.id", OutboxEvent.class)
                                .getResultList();
                        assertThat(events).isEmpty();
                    }));
        }

    }
}
