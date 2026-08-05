package io.quarkus.it.panache.reactive;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;

/**
 * Endpoint to verify that both {@code session.currentTransaction()} and
 * {@code Panache.currentTransaction()} detect the externally-opened transaction
 * inside a {@code @Transactional} method.
 *
 * @see <a href="https://github.com/hibernate/hibernate-reactive/issues/2852">HR #2852</a>
 */
@Path("test-transactional")
public class TransactionalTestEndpoint {

    @Inject
    TransactionalService service;

    @GET
    @Path("current-transaction")
    public Uni<String> testCurrentTransaction() {
        return service.persistAndCheckTransaction();
    }

    @ApplicationScoped
    public static class TransactionalService {

        @Inject
        Mutiny.Session session;

        @Transactional
        public Uni<String> persistAndCheckTransaction() {
            Person p = new Person();
            p.name = "currentTransaction";
            return session.persist(p)
                    .chain(session::flush)
                    .chain(() -> Panache.currentTransaction())
                    .map(panacheTx -> {
                        Mutiny.Transaction sessionTx = session.currentTransaction();
                        assertNotNull(sessionTx, "session.currentTransaction() should not be null");
                        assertNotNull(panacheTx, "Panache.currentTransaction() should not be null");
                        assertSame(sessionTx, panacheTx,
                                "Panache.currentTransaction() and session.currentTransaction() should return the same transaction");
                        return "OK";
                    })
                    .eventually(() -> session.remove(p));
        }
    }
}
