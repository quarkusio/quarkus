package io.quarkus.it.jpa.mariadb;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import io.quarkus.hibernate.orm.PersistenceUnit;

@Path("/offline2/dialect")
@Produces(MediaType.APPLICATION_JSON)
public class Offline2DialectEndpoint {
    @Inject
    @PersistenceUnit("offline2")
    SessionFactory sessionFactory;

    @GET
    public OfflineDialectDescriptor test() throws IOException {
        return new OfflineDialectDescriptor(
                (MariaDBDialect) sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect());
    }
}
