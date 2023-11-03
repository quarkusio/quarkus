package io.quarkus.it.jpa.generatedvalue;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/generated-value")
@ApplicationScoped
@Produces(MediaType.TEXT_PLAIN)
public class GeneratedValueResource {

    @Inject
    EntityManager em;

    @GET
    @Path("/test")
    @Transactional
    public String test() throws Exception {
        var entity = new EntityWithGeneratedValues();
        assertThat(entity).satisfies(
                e -> assertThat(e.id).isNull(),
                e -> assertThat(e.tenant).isNull(),
                e -> assertThat(e.creationTimestamp).isNull(),
                e -> assertThat(e.updateTimestamp).isNull(),
                e -> assertThat(e.currentTimestamp).isNull(),
                e -> assertThat(e.generated).isNull(),
                e -> assertThat(e.generatedColumn).isNull());
        em.persist(entity);
        em.flush();
        assertThat(entity).satisfies(
                e -> assertThat(e.id).isNotNull(),
                e -> assertThat(e.tenant).isNotNull(),
                e -> assertThat(e.creationTimestamp).isNotNull(),
                e -> assertThat(e.updateTimestamp).isNotNull(),
                e -> assertThat(e.currentTimestamp).isNotNull(),
                e -> assertThat(e.generated).isNotNull(),
                e -> assertThat(e.generatedColumn).isNotNull());

        return "OK";
    }
}
