package io.quarkus.it.jpa.attributeconverter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

@Path("/attribute-converter")
@ApplicationScoped
public class AttributeConverterResource {

    @Inject
    EntityManager em;

    @GET
    @Path("/with-cdi")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String withCdi(@RestQuery String theData) {
        EntityWithAttributeConverters entity = new EntityWithAttributeConverters();
        entity.setMyDataRequiringCDI(new MyDataRequiringCDI(theData));
        em.persist(entity);

        em.flush();
        em.clear();

        // This can only return `theData` if Hibernate ORM correctly instantiates MyDataConverter through CDI
        // so that MyDataConversionService is injected.
        return em.find(EntityWithAttributeConverters.class, entity.getId()).getMyDataRequiringCDI().getContent();
    }

    @GET
    @Path("/without-cdi")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String withoutCdi(@RestQuery String theData) {
        EntityWithAttributeConverters entity = new EntityWithAttributeConverters();
        entity.setMyDataNotRequiringCDI(new MyDataNotRequiringCDI(theData));
        em.persist(entity);

        em.flush();
        em.clear();

        // This can only return `theData` if Hibernate ORM correctly instantiates MyDataConverter through CDI
        // so that MyDataConversionService is injected.
        return em.find(EntityWithAttributeConverters.class, entity.getId()).getMyDataNotRequiringCDI().getContent();
    }
}
