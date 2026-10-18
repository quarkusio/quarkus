package io.quarkus.it.jpa.mssql;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import io.quarkus.hibernate.orm.runtime.config.DialectVersions;

@Path("/dialect/version")
@Produces(MediaType.TEXT_PLAIN)
public class DialectEndpoint {
    @Inject
    SessionFactory sessionFactory;

    @GET
    public String test() throws IOException {
        var version = sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect().getVersion();
        return DialectVersions.toString(version);
    }

}
