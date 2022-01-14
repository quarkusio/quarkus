package io.quarkus.it.hibernate.search.orm.elasticsearch.coordination.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;

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
                throw new IllegalStateException("Transaction exception", t);
            }
        } catch (SystemException | NotSupportedException e) {
            throw new IllegalStateException("Transaction exception", e);
        }
    }

    public static void awaitAgentsRunning(EntityManager entityManager, UserTransaction userTransaction,
            int expectedAgentCount) {
        await("Waiting for the formation of a cluster of " + expectedAgentCount + " agents")
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(5))
                .atMost(Duration.ofSeconds(5))
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

}
