package io.quarkus.it.jpa.mapping.xml.modern.app.xmlmappingonly;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.PersistenceUnit;

@Path("/xml-mapping-only/dirty-checking")
@ApplicationScoped
public class XmlMappingOnlyDirtyCheckingResource {

    @Inject
    @PersistenceUnit("xmlMappingOnly")
    EntityManager entityManager;

    @GET
    @Path("/basic/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String basic() {
        XmlMappingOnlyEntity entity = persistOneEntity();

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
        XmlMappingOnlyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        String updated = "updated";
        assertThat(entity.getEmbedded().getEmbeddedBasic()).isNotEqualTo(updated);
        XmlMappingOnlyEmbeddable updatedEmbeddable = new XmlMappingOnlyEmbeddable();
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
        XmlMappingOnlyEntity entity = persistOneEntity();

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
        XmlMappingOnlyEntity entity = persistOneEntity();

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
        XmlMappingOnlyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        XmlMappingOnlyOtherEntity updated = new XmlMappingOnlyOtherEntity();
        entityManager.persist(updated);
        assertThat(entity.getOneToOne()).isNotEqualTo(updated);
        entity.setOneToOne(updated);

        entity = flushClearAndRetrieve(entity);
        updated = entityManager.find(XmlMappingOnlyOtherEntity.class, updated.getId());
        assertThat(entity.getOneToOne()).isEqualTo(updated);

        return "OK";
    }

    @GET
    @Path("/many-to-one/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String manyToOne() {
        XmlMappingOnlyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        XmlMappingOnlyOtherEntity updated = new XmlMappingOnlyOtherEntity();
        entityManager.persist(updated);
        assertThat(entity.getManyToOne()).isNotEqualTo(updated);
        entity.setManyToOne(updated);

        entity = flushClearAndRetrieve(entity);
        updated = entityManager.find(XmlMappingOnlyOtherEntity.class, updated.getId());
        assertThat(entity.getManyToOne()).isEqualTo(updated);

        return "OK";
    }

    @GET
    @Path("/one-to-many/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String oneToMany() {
        XmlMappingOnlyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        XmlMappingOnlyOtherEntity updated = new XmlMappingOnlyOtherEntity();
        entityManager.persist(updated);
        assertThat(entity.getOneToMany()).doesNotContain(updated);
        entity.getOneToMany().add(updated);

        entity = flushClearAndRetrieve(entity);
        updated = entityManager.find(XmlMappingOnlyOtherEntity.class, updated.getId());
        assertThat(entity.getOneToMany()).contains(updated);

        return "OK";
    }

    @GET
    @Path("/many-to-many/")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String manyToMany() {
        XmlMappingOnlyEntity entity = persistOneEntity();

        entity = flushClearAndRetrieve(entity);
        XmlMappingOnlyOtherEntity updated = new XmlMappingOnlyOtherEntity();
        entityManager.persist(updated);
        assertThat(entity.getManyToMany()).doesNotContain(updated);
        entity.getManyToMany().add(updated);

        entity = flushClearAndRetrieve(entity);
        updated = entityManager.find(XmlMappingOnlyOtherEntity.class, updated.getId());
        assertThat(entity.getManyToMany()).contains(updated);

        return "OK";
    }

    private XmlMappingOnlyEntity flushClearAndRetrieve(XmlMappingOnlyEntity entity) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(XmlMappingOnlyEntity.class, entity.getId());
    }

    private XmlMappingOnlyEntity persistOneEntity() {
        XmlMappingOnlyEntity entity = new XmlMappingOnlyEntity();
        entity.setBasic("initial");
        entity.setEmbedded(new XmlMappingOnlyEmbeddable());
        entity.getEmbedded().setEmbeddedBasic("initial");
        entity.getElementCollection().add("initial");

        XmlMappingOnlyOtherEntity initialOther = new XmlMappingOnlyOtherEntity();
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
