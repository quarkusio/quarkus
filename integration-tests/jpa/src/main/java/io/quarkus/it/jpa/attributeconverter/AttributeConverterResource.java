package io.quarkus.it.jpa.attributeconverter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@Path("/attribute-converter")
@ApplicationScoped
public class AttributeConverterResource {

    @Inject
    EntityManager em;

    @GET
    @Path("/with-cdi")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String withCdi(@QueryParam String theData) {
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
    public String withoutCdi(@QueryParam String theData) {
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
