package io.quarkus.hibernate.reactive.transaction.mixing;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.transaction.Hero;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class MixStatelessStatefulSessionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Hero.class))
            .withConfigurationResource("application.properties");

    @Inject
    Mutiny.Session session;

    @Inject
    Mutiny.StatelessSession statelessSession;

    @Test
    @RunOnVertxContext
    public void testStatelessSessionThenRegularSessionInTransactional(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> transactionalMethodUsingStatelessSessionThenRegularSession(),
                e -> assertThat(e)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("stateless session for the same Persistence Unit is already opened")
                        .hasMessageContaining("Mixing different kinds of sessions is forbidden"));
    }

    @Test
    @RunOnVertxContext
    public void testRegularSessionThenStatelessSessionInTransactional(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> transactionalMethodUsingRegularSessionThenStatelessSession(),
                e -> assertThat(e)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("session for the same Persistence Unit is already opened")
                        .hasMessageContaining("Mixing different kinds of sessions is forbidden"));
    }

    @Transactional
    public Uni<Object> transactionalMethodUsingRegularSessionThenStatelessSession() {
        return session.createQuery("select count(1) from Hero h", Long.class).getSingleResult()
                .chain(count -> statelessSession.createQuery("select count(1) from Hero h", Long.class)
                        .getSingleResult());
    }

    @Transactional
    public Uni<Object> transactionalMethodUsingStatelessSessionThenRegularSession() {
        return statelessSession.createQuery("select count(1) from Hero h", Long.class).getSingleResult()
                .chain(count -> session.createQuery("select count(1) from Hero h", Long.class)
                        .getSingleResult());
    }

}
