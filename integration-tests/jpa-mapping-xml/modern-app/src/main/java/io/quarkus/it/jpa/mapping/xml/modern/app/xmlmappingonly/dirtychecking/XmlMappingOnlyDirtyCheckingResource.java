package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly.dirtychecking;

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

@Path("/xml-mapping-only/dirty-checking")
@ApplicationScoped
public class XmlMappingOnlyDirtyCheckingResource {

    @Inject
    @PersistenceUnit("xml-mapping-only-dirty-checking")
    EntityManager entityManager;

    @GET
    @Path("/basic/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String basic() {
        MyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        String updated = "updated";
        assertThat(entity.getBasic()).isNotEqualTo(updated);
        entity.setBasic(updated);

        entity = flushClearAndRetrieve(entity);
        assertThat(entity.getBasic()).isEqualTo(updated);

        return "OK";
    }

    @GET
    @Path("/embedded/replace/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String embedded_replace() {
        MyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        String updated = "updated";
        assertThat(entity.getEmbedded().getEmbeddedBasic()).isNotEqualTo(updated);
        MyEmbeddable updatedEmbeddable = new MyEmbeddable();
        updatedEmbeddable.setEmbeddedBasic(updated);
        entity.setEmbedded(updatedEmbeddable);

        entity = flushClearAndRetrieve(entity);
        assertThat(entity.getEmbedded().getEmbeddedBasic()).isEqualTo(updated);

        return "OK";
    }

    @GET
    @Path("/embedded/update/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String embedded_update() {
        MyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        String updated = "updated";
        assertThat(entity.getEmbedded().getEmbeddedBasic()).isNotEqualTo(updated);
        entity.getEmbedded().setEmbeddedBasic(updated);

        entity = flushClearAndRetrieve(entity);
        assertThat(entity.getEmbedded().getEmbeddedBasic()).isEqualTo(updated);

        return "OK";
    }

    @GET
    @Path("/element-collection/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String elementCollection() {
        MyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        String updated = "updated";
        assertThat(entity.getElementCollection()).doesNotContain(updated);
        entity.getElementCollection().add(updated);

        entity = flushClearAndRetrieve(entity);
        assertThat(entity.getElementCollection()).contains(updated);

        return "OK";
    }

    @GET
    @Path("/one-to-one/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String oneToOne() {
        MyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        MyOtherEntity updated = new MyOtherEntity();
        entityManager.persist(updated);
        assertThat(entity.getOneToOne()).isNotEqualTo(updated);
        entity.setOneToOne(updated);

        entity = flushClearAndRetrieve(entity);
        updated = entityManager.find(MyOtherEntity.class, updated.getId());
        assertThat(entity.getOneToOne()).isEqualTo(updated);

        return "OK";
    }

    @GET
    @Path("/many-to-one/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String manyToOne() {
        MyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        MyOtherEntity updated = new MyOtherEntity();
        entityManager.persist(updated);
        assertThat(entity.getManyToOne()).isNotEqualTo(updated);
        entity.setManyToOne(updated);

        entity = flushClearAndRetrieve(entity);
        updated = entityManager.find(MyOtherEntity.class, updated.getId());
        assertThat(entity.getManyToOne()).isEqualTo(updated);

        return "OK";
    }

    @GET
    @Path("/one-to-many/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String oneToMany() {
        MyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        MyOtherEntity updated = new MyOtherEntity();
        entityManager.persist(updated);
        assertThat(entity.getOneToMany()).doesNotContain(updated);
        entity.getOneToMany().add(updated);

        entity = flushClearAndRetrieve(entity);
        updated = entityManager.find(MyOtherEntity.class, updated.getId());
        assertThat(entity.getOneToMany()).contains(updated);

        return "OK";
    }

    @GET
    @Path("/many-to-many/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String manyToMany() {
        MyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        MyOtherEntity updated = new MyOtherEntity();
        entityManager.persist(updated);
        assertThat(entity.getManyToMany()).doesNotContain(updated);
        entity.getManyToMany().add(updated);

        entity = flushClearAndRetrieve(entity);
        updated = entityManager.find(MyOtherEntity.class, updated.getId());
        assertThat(entity.getManyToMany()).contains(updated);

        return "OK";
    }

    private MyEntity flushClearAndRetrieve(MyEntity entity) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(MyEntity.class, entity.getId());
    }

    private MyEntity persistOneEntity() {
        MyEntity entity = new MyEntity();
        entity.setBasic("initial");
        entity.setEmbedded(new MyEmbeddable());
        entity.getEmbedded().setEmbeddedBasic("initial");
        entity.getElementCollection().add("initial");

        MyOtherEntity initialOther = new MyOtherEntity();
        entityManager.persist(initialOther);

        entity.setOneToOne(initialOther);
        entity.setManyToOne(initialOther);
        entity.getOneToMany().add(initialOther);
        entity.getManyToMany().add(initialOther);

        entityManager.persist(entity);
        entityManager.flush();

        return entity;
    }

}
