package io.quarkus.it.hibernate.search.orm.elasticsearch.coordination.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;

public class OutboxPollingTestUtils {

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
                    List<Agent> agents = entityManager.createQuery("select a from Agent a order by a.id", Agent.class)
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
                            .createQuery("select e from OutboxEvent e order by e.id", OutboxEvent.class)
                            .getResultList();
                    assertThat(events).isEmpty();
                }));
    }

}
