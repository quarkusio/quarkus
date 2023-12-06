package io.quarkus.it.jpa.postgresql;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.it.jpa.postgresql.otherpu.EntityWithJsonOtherPU;
import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Various tests covering JPA functionality. All tests should work in both standard JVM and in native mode.
 */
@Path("/jpa/testfunctionality")
@Produces(MediaType.TEXT_PLAIN)
public class JPAFunctionalityTestEndpoint {

    @Inject
    EntityManager em;

    @Inject
    @PersistenceUnit("other")
    EntityManager otherEm;

    @GET
    public String json() {
        QuarkusTransaction.requiringNew().run(() -> {
            EntityWithJson entity = new EntityWithJson(
                    new EntityWithJson.ToBeSerializedWithDateTime(LocalDate.of(2023, 7, 28)));
            em.persist(entity);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            List<EntityWithJson> entities = em
                    .createQuery("select e from EntityWithJson e", EntityWithJson.class)
                    .getResultList();
            if (entities.isEmpty()) {
                throw new AssertionError("No entities with json were found");
            }
        });

        QuarkusTransaction.requiringNew().run(() -> {
            em.createQuery("delete from EntityWithJson").executeUpdate();
        });

        Exception exception = null;
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                EntityWithJsonOtherPU otherPU = new EntityWithJsonOtherPU(
                        new EntityWithJsonOtherPU.ToBeSerializedWithDateTime(LocalDate.of(2023, 7, 28)));
                otherEm.persist(otherPU);
            });
        } catch (Exception e) {
            exception = e;
        }

        if (exception == null) {
            throw new AssertionError(
                    "Default mapper cannot process date/time properties. So we were expecting transaction to fail, but it did not!");
        }
        if (!(exception instanceof UnsupportedOperationException)
                || !exception.getMessage().contains("I cannot convert anything to JSON")) {
            throw new AssertionError("flush failed for a different reason than expected.", exception);
        }

        return "OK";
    }

}
