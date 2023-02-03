package io.quarkus.it.jpa.defaultcatalogandschema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.Session;
import org.hibernate.type.LongType;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@Path("/default-catalog-and-schema")
@ApplicationScoped
public class DefaultCatalogAndSchemaResource {

    @Inject
    Session session;

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String test(@QueryParam String expectedSchema) {
        assertThat(findUsingNativeQuery(expectedSchema, "foo")).isEmpty();

        EntityWithDefaultCatalogAndSchema entity = new EntityWithDefaultCatalogAndSchema();
        entity.basic = "foo";
        session.persist(entity);
        session.flush();
        session.clear();

        assertThat(findUsingNativeQuery(expectedSchema, "foo")).containsExactly(entity.id);

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private List<Long> findUsingNativeQuery(String schema, String value) {
        return session
                .createNativeQuery(
                        "select id from \"" + schema + "\"." + EntityWithDefaultCatalogAndSchema.NAME
                                + " where basic = :basic")
                .addScalar("id", LongType.INSTANCE)
                .setParameter("basic", value)
                .getResultList();
    }
}
