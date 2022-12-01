package io.quarkus.it.jpa.defaultcatalogandschema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
