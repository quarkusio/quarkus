package io.quarkus.it.jpa.mapping.xml.modern.app;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.it.jpa.mapping.xml.modern.library_a.LibraryAEntity;
import io.quarkus.it.jpa.mapping.xml.modern.library_b.LibraryBEntity;

@Path("/modern-app")
@ApplicationScoped
public class ModernAppResource {

    @Inject
    @PersistenceUnit("libraryA")
    EntityManager emA;

    @Inject
    @PersistenceUnit("libraryB")
    EntityManager emB;

    @GET
    @Path("/library-a")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String libraryA() {
        // Check that orm.xml was taken into account
        assertThat(SchemaUtil.getColumnNames(emA.getEntityManagerFactory(), LibraryAEntity.class))
                .contains("name_a")
                .doesNotContain("name");
        // Check that persistence works
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(emA,
                LibraryAEntity.class, LibraryAEntity::new,
                LibraryAEntity::getId, LibraryAEntity::setName, LibraryAEntity::getName);
        return "OK";
    }

    @GET
    @Path("/library-b")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String libraryB() {
        // Check that orm.xml was taken into account
        assertThat(SchemaUtil.getColumnNames(emB.getEntityManagerFactory(), LibraryBEntity.class))
                .contains("name_b")
                .doesNotContain("name");
        // Check that persistence works
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(emB,
                LibraryBEntity.class, LibraryBEntity::new,
                LibraryBEntity::getId, LibraryBEntity::setName, LibraryBEntity::getName);
        return "OK";
    }

}
