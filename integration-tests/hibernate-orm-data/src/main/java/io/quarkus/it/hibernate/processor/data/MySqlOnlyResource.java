package io.quarkus.it.hibernate.processor.data;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.hibernate.Session;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.arc.Arc;
import io.quarkus.it.hibernate.processor.data.pusqlonly.MySqlOnlyRepository;
import io.quarkus.runtime.Startup;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/data/sqlonly")
public class MySqlOnlyResource {

    @Inject
    MySqlOnlyRepository repository;

    @Startup
    @Transactional
    public void init() {
        // Workaround for Hibernate ORM not running schema management at all
        // (not even import.sql) when there are no entities

        Session session = Arc.container().instance(Session.class).get();

        session.createNativeQuery("DROP TABLE myuser IF EXISTS")
                .executeUpdate();
        session.createNativeQuery("""
                CREATE TABLE myuser (
                   id INT,
                   username VARCHAR(255),
                   password VARCHAR(255),
                   role VARCHAR(255)
                );
                """)
                .executeUpdate();
    }

    @PUT
    @Transactional
    @Path("/myuser/{id}")
    public void insert(@RestPath Integer id, MySqlOnlyRepository.MyUserDto user) {
        repository.insert(id, user.username(), user.role());
    }

    @GET
    @Transactional
    @Path("/myuser/by/username/{name}")
    public MySqlOnlyRepository.MyUserDto getByName(@RestPath String name) {
        return repository.findByUsername(name).orElseThrow(NotFoundException::new);
    }

}
