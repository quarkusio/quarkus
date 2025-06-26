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

        var entity2 = new EntityWithCustomIdGeneratorType();
        assertThat(entity2.id).isNull();
        em.persist(entity2);
        em.flush();
        assertThat(entity2.id).isEqualTo(MyCustomIdGenerator.STUB_VALUE);

        var entity3 = new EntityWithCustomValueGeneratorType();
        assertThat(entity3.customGenerated).isNull();
        em.persist(entity3);
        em.flush();
        assertThat(entity3.customGenerated).isEqualTo(MyCustomValueGenerator.STUB_VALUE);

        var entity4 = new EntityWithCustomGenericGeneratorReferencedAsClass();
        assertThat(entity4.id).isNull();
        em.persist(entity4);
        em.flush();
        assertThat(entity4.id).isEqualTo(MyCustomGenericGeneratorReferencedAsClass.STUB_VALUE);

        var entity5 = new EntityWithCustomGenericGeneratorReferencedAsClassName();
        assertThat(entity5.id).isNull();
        em.persist(entity5);
        em.flush();
        assertThat(entity5.id).isEqualTo(MyCustomGenericGeneratorReferencedAsClassName.STUB_VALUE);

        return "OK";
    }
}
