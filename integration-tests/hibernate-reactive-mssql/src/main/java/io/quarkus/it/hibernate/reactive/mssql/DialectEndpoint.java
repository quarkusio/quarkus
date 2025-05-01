package io.quarkus.it.hibernate.reactive.mssql;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.impl.MutinySessionFactoryImpl;

import io.quarkus.arc.ClientProxy;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;

@Path("/dialect/version")
@Produces(MediaType.TEXT_PLAIN)
public class DialectEndpoint {
    @Inject
    Mutiny.SessionFactory sessionFactory;

    @GET
    public String test() throws IOException {
        var version = ((MutinySessionFactoryImpl) ClientProxy.unwrap(sessionFactory)).getServiceRegistry()
                .requireService(JdbcServices.class).getDialect().getVersion();
        return DialectVersions.toString(version);
    }

}
