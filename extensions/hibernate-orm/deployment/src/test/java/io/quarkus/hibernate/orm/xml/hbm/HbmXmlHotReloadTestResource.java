package io.quarkus.hibernate.orm.xml.hbm;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.SchemaUtil;

@Path("/hbm-xml-hot-reload-test")
public class HbmXmlHotReloadTestResource {
    @Inject
    EntityManagerFactory entityManagerFactory;

    @GET
    @Path("/column-names")
    @Produces(MediaType.APPLICATION_JSON)
    public String getColumnNames() {
        return String.join("\n", SchemaUtil.getColumnNames(entityManagerFactory, NonAnnotatedEntity.class));
    }
}
