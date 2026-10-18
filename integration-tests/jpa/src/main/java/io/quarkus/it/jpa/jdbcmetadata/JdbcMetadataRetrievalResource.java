package io.quarkus.it.jpa.jdbcmetadata;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/jdbc-metadata-retrieval")
@ApplicationScoped
public class JdbcMetadataRetrievalResource {

    @Inject
    EntityManager em;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String test() {
        EntityWithSequenceIdentityId entity = new EntityWithSequenceIdentityId();
        entity.setName("thePersistedName");

        // This cannot work unless Hibernate ORM correct detects that the JDBC driver/database
        // support getting generated keys after an insert,
        // which requires Hibernate ORM to retrieve JDBC metadata at runtime init.
        // If Hibernate ORM doesn't retrieve this metadata,
        // it will assume getting generated keys is not possible and throw an exception upon insert.
        em.persist(entity);

        em.clear();

        return em.find(EntityWithSequenceIdentityId.class, entity.getId()).getName();
    }
}
