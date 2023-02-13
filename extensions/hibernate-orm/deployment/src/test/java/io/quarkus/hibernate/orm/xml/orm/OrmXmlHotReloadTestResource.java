package io.quarkus.hibernate.orm.xml.orm;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.SchemaUtil;

@Path("/orm-xml-hot-reload-test")
public class OrmXmlHotReloadTestResource {
    @Inject
    EntityManagerFactory entityManagerFactory;

    @GET
    @Path("/column-names")
    @Produces(MediaType.APPLICATION_JSON)
    public String getColumnNames() {
        return String.join("\n", SchemaUtil.getColumnNames(entityManagerFactory, NonAnnotatedEntity.class));
    }
}
