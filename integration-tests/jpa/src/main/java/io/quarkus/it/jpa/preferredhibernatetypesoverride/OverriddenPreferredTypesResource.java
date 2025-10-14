package io.quarkus.it.jpa.preferredhibernatetypesoverride;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.jboss.resteasy.reactive.RestQuery;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Path("/overridden-preferred-types")
@ApplicationScoped
public class OverriddenPreferredTypesResource {

    @Inject
    Session session;

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String test(@RestQuery String expectedSchema) {
        assertThat(findUsingNativeQuery(expectedSchema)).isEmpty();

        var entity = new EntityWithOverridablePreferredTypes();
        entity.isPersisted = true;
        entity.createdAt = Instant.now();
        entity.overridenDuration = Duration.ofDays(1);

        session.persist(entity);
        session.flush();
        session.clear();

        assertThat(findUsingNativeQuery(expectedSchema)).containsExactly(entity.id);

        return "OK";
    }

    private List<UUID> findUsingNativeQuery(String schema) {
        return session.createNativeQuery(
                        """
                        SELECT id FROM "%s".%s WHERE isPersisted = :isPersisted
                        """.formatted(schema, EntityWithOverridablePreferredTypes.NAME)
                )
                .addScalar("id", StandardBasicTypes.UUID_CHAR)
                .setParameter("isPersisted", true)
                .getResultList();
    }
}
