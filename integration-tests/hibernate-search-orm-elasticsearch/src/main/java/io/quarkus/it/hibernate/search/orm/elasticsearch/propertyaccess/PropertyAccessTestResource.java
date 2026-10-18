package io.quarkus.it.hibernate.search.orm.elasticsearch.propertyaccess;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.Hibernate;
import org.hibernate.search.mapper.orm.session.SearchSession;

@Path("/test/property-access")
public class PropertyAccessTestResource {

    @Inject
    EntityManager entityManager;

    @Inject
    SearchSession searchSession;

    @Inject
    UserTransaction transaction;

    @GET
    @Path("/private-field")
    @Produces(MediaType.TEXT_PLAIN)
    public String testPrivateFieldAccess() {
        long id = 1L;
        String value = "foo";

        inTransaction(() -> assertThat(searchSession.search(PrivateFieldAccessEntity.class)
                .where(f -> f.match().field("property")
                        .matching(value))
                .fetchAllHits())
                .isEmpty());

        // While indexing, HSearch will read the entity property by calling a synthetic getter
        inTransaction(() -> entityManager.persist(new PrivateFieldAccessEntity(id, value)));

        inTransaction(() -> assertThat(searchSession.search(PrivateFieldAccessEntity.class)
                .where(f -> f.match().field("property")
                        .matching(value))
                .fetchAllHits())
                .hasSize(1));

        return "OK";
    }

    @GET
    @Path("/private-field-lazy-init")
    @Produces(MediaType.TEXT_PLAIN)
    public String testPrivateFieldAccessLazyInitialization() {
        long id = 2L;
        String value = "bar";

        inTransaction(() -> assertThat(searchSession.search(PrivateFieldAccessEntity.class)
                .where(f -> f.match().field("property")
                        .matching(value))
                .fetchAllHits())
                .isEmpty());

        inTransaction(() -> entityManager.persist(new PrivateFieldAccessEntity(id, value)));

        inTransaction(() -> {
            // "otherProperty" has no value in the index...
            assertThat(searchSession.search(PrivateFieldAccessEntity.class)
                    .where(f -> f.match().field("otherProperty")
                            .matching(value))
                    .fetchAllHits())
                    .isEmpty();

            // but "property" has.
            assertThat(searchSession.search(PrivateFieldAccessEntity.class)
                    .where(f -> f.match().field("property")
                            .matching(value))
                    .fetchAllHits())
                    .hasSize(1);
        });

        // While indexing, HSearch will read the entity property by calling a synthetic getter,
        // ensuring that it gets initialized automatically
        inTransaction(() -> {
            PrivateFieldAccessEntity entity = entityManager.getReference(PrivateFieldAccessEntity.class, id);
            // The entity is not initialized
            assertThat(entity)
                    .returns(false, e -> Hibernate.isPropertyInitialized(e, "property"))
                    .returns(false, e -> Hibernate.isPropertyInitialized(e, "otherProperty"));

            // We update "otherProperty"
            entity.setOtherProperty(value);

            // Consequently, "otherProperty" is initialized,
            // but "property" (which is in a different @LazyGroup) still isn't.
            assertThat(entity)
                    .returns(false, e -> Hibernate.isPropertyInitialized(e, "property"));
        });

        inTransaction(() -> {
            // "otherProperty" was updated in the index...
            assertThat(searchSession.search(PrivateFieldAccessEntity.class)
                    .where(f -> f.match().field("otherProperty")
                            .matching(value))
                    .fetchAllHits())
                    .hasSize(1);

            // and "property" still has a value, proving that it was lazily initialized upon indexing.
            assertThat(searchSession.search(PrivateFieldAccessEntity.class)
                    .where(f -> f.match().field("property")
                            .matching(value))
                    .fetchAllHits())
                    .hasSize(1);
        });

        return "OK";
    }

    @GET
    @Path("/method")
    @Produces(MediaType.TEXT_PLAIN)
    public String testMethodAccess() {
        long id = 1L;
        String value = "foo";

        inTransaction(() -> assertThat(searchSession.search(MethodAccessEntity.class)
                .where(f -> f.match().field("property")
                        .matching(value))
                .fetchAllHits())
                .isEmpty());

        // While indexing, HSearch will read the entity property through its getter method
        inTransaction(() -> entityManager.persist(new MethodAccessEntity(id, value)));

        inTransaction(() -> assertThat(searchSession.search(MethodAccessEntity.class)
                .where(f -> f.match().field("property")
                        .matching(value))
                .fetchAllHits())
                .hasSize(1));

        return "OK";
    }

    @GET
    @Path("/transient-method")
    @Produces(MediaType.TEXT_PLAIN)
    public String testTransientMethodAccess() {
        long id = 1L;
        String value1 = "foo1";
        String value2 = "foo2";

        inTransaction(() -> {
            assertThat(searchSession.search(TransientMethodAccessEntity.class)
                    .where(f -> f.match().field("property")
                            .matching(value1))
                    .fetchAllHits())
                    .isEmpty();
            assertThat(searchSession.search(TransientMethodAccessEntity.class)
                    .where(f -> f.match().field("property")
                            .matching(value2))
                    .fetchAllHits())
                    .isEmpty();
        });

        // While indexing, HSearch will read the (@Transient) entity property through its getter method
        inTransaction(() -> entityManager.persist(new TransientMethodAccessEntity(id, value1, value2)));

        inTransaction(() -> {
            assertThat(searchSession.search(TransientMethodAccessEntity.class)
                    .where(f -> f.match().field("property")
                            .matching(value1))
                    .fetchAllHits())
                    .hasSize(1);
            assertThat(searchSession.search(TransientMethodAccessEntity.class)
                    .where(f -> f.match().field("property")
                            .matching(value2))
                    .fetchAllHits())
                    .hasSize(1);
        });

        return "OK";
    }

    private void inTransaction(Runnable runnable) {
        try {
            transaction.begin();
            try {
                runnable.run();
                transaction.commit();
            } catch (Throwable t) {
                try {
                    transaction.rollback();
                } catch (RuntimeException e2) {
                    t.addSuppressed(e2);
                }
                throw t;
            }
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException
                | HeuristicRollbackException e) {
            throw new IllegalStateException(e);
        }
    }

}
