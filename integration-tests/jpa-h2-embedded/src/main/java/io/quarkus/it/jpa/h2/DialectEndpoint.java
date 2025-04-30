package io.quarkus.it.jpa.h2;

import java.io.IOException;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import io.quarkus.hibernate.orm.runtime.config.DialectVersions;

@Path("/dialect/")
@Produces(MediaType.TEXT_PLAIN)
public class DialectEndpoint {
    @Inject
    SessionFactory sessionFactory;
    @Inject
    DataSource dataSource;

    @GET
    @Path("version")
    public String version() throws IOException {
        var version = sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect().getVersion();
        return DialectVersions.toString(version);
    }

    @GET
    @Path("actual-db-version")
    public String actualDbVersion() throws IOException, SQLException {
        try (var conn = dataSource.getConnection()) {
            return conn.getMetaData().getDatabaseProductVersion();
        }
    }

}
