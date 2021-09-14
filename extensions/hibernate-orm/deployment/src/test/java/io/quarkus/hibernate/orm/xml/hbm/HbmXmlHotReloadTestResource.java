package io.quarkus.hibernate.orm.xml.hbm;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
