package io.quarkus.hibernate.orm.stateless;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that Hibernate ORM allows mixing regular Session and StatelessSession
 * within the same transaction.
 */
public class MixStatelessStatefulSessionTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties");

    @Inject
    Session session;

    @Inject
    StatelessSession statelessSession;

    @Test
    public void testRegularSessionThenStatelessSessionInSameTransaction() {
        // Use regular Session, then StatelessSession in same transaction
        QuarkusTransaction.requiringNew().run(() -> {
            MyEntity entity = new MyEntity("testRegular");
            session.persist(entity);
            session.flush();
            // Now use StatelessSession in the same transaction - should work without error
            List<String> list = statelessSession
                    .createSelectionQuery("SELECT e.name from MyEntity e WHERE e.name = 'testRegular'", String.class)
                    .getResultList();
            assertThat(list).containsOnly("testRegular");
        });

        // Verify it was persisted
        QuarkusTransaction.requiringNew().run(() -> {
            long count = session
                    .createSelectionQuery("SELECT count(e) from MyEntity e WHERE e.name = 'testRegular'", Long.class)
                    .getSingleResult();
            assertThat(count).isEqualTo(1);
        });
    }

    @Test
    public void testStatelessSessionThenRegularSessionInSameTransaction() {
        // Use StatelessSession, then regular Session in same transaction
        QuarkusTransaction.requiringNew().run(() -> {
            MyEntity entity = new MyEntity("testStateless");
            statelessSession.insert(entity);
            // Now use regular Session in the same transaction - should work without error
            List<String> list = session
                    .createSelectionQuery("SELECT e.name from MyEntity e WHERE e.name = 'testStateless'", String.class)
                    .getResultList();
            assertThat(list).containsOnly("testStateless");
        });

        // Verify it was persisted
        QuarkusTransaction.requiringNew().run(() -> {
            long count = session
                    .createSelectionQuery("SELECT count(e) from MyEntity e WHERE e.name = 'testStateless'", Long.class)
                    .getSingleResult();
            assertThat(count).isEqualTo(1);
        });
    }

    @Test
    public void testBothSessionsCommitTogether() {
        // Make changes via both sessions, verify both commit together
        QuarkusTransaction.requiringNew().run(() -> {
            MyEntity entity1 = new MyEntity("commitViaRegular");
            session.persist(entity1);
            session.flush();

            MyEntity entity2 = new MyEntity("commitViaStateless");
            statelessSession.insert(entity2);
        });

        // Verify both were persisted
        QuarkusTransaction.requiringNew().run(() -> {
            long count1 = session
                    .createSelectionQuery("SELECT count(e) from MyEntity e WHERE e.name = 'commitViaRegular'", Long.class)
                    .getSingleResult();
            long count2 = session
                    .createSelectionQuery("SELECT count(e) from MyEntity e WHERE e.name = 'commitViaStateless'", Long.class)
                    .getSingleResult();
            assertThat(count1).isEqualTo(1);
            assertThat(count2).isEqualTo(1);
        });
    }

    @Test
    public void testBothSessionsRollbackTogether() {
        // Make changes via both sessions, then rollback - verify both rolled back
        assertThatThrownBy(() -> {
            QuarkusTransaction.requiringNew().run(() -> {
                MyEntity entity1 = new MyEntity("rollbackViaRegular");
                session.persist(entity1);
                session.flush();

                MyEntity entity2 = new MyEntity("rollbackViaStateless");
                statelessSession.insert(entity2);

                throw new RuntimeException("Force rollback");
            });
        }).isInstanceOf(RuntimeException.class)
                .hasMessage("Force rollback");

        // Verify neither was persisted
        QuarkusTransaction.requiringNew().run(() -> {
            long count1 = session
                    .createSelectionQuery("SELECT count(e) from MyEntity e WHERE e.name = 'rollbackViaRegular'", Long.class)
                    .getSingleResult();
            long count2 = session
                    .createSelectionQuery("SELECT count(e) from MyEntity e WHERE e.name = 'rollbackViaStateless'", Long.class)
                    .getSingleResult();
            assertThat(count1).isEqualTo(0);
            assertThat(count2).isEqualTo(0);
        });
    }
}
