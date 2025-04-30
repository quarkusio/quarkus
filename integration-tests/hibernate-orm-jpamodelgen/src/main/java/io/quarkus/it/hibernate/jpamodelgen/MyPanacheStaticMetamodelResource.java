package io.quarkus.it.hibernate.jpamodelgen;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.hibernate.Session;
import org.hibernate.query.criteria.JpaRoot;
import org.jboss.resteasy.reactive.RestPath;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/panache/static-metamodel/")
public class MyPanacheStaticMetamodelResource {

    @Inject
    Session session;

    @POST
    @Transactional
    public void create(MyPanacheStaticMetamodelEntity entity) {
        session.persist(entity);
    }

    @GET
    @Transactional
    @Path("/by/name/{name}")
    public MyPanacheStaticMetamodelEntity getByName(@RestPath String name) {
        var b = session.getCriteriaBuilder();
        var query = b.createQuery(MyPanacheStaticMetamodelEntity.class);
        var e = query.from(MyPanacheStaticMetamodelEntity_.class_);
        query.where(e.get(MyPanacheStaticMetamodelEntity_.name).equalTo(name));
        return session.createQuery(query).uniqueResultOptional().orElseThrow(NotFoundException::new);
    }

    @POST
    @Transactional
    @Path("/rename/{before}/to/{after}")
    public void rename(@RestPath String before, @RestPath String after) {
        var b = session.getCriteriaBuilder();
        var query = b.createCriteriaUpdate(MyPanacheStaticMetamodelEntity.class);
        // Cast to work around https://hibernate.atlassian.net/browse/HHH-17682
        var e = (JpaRoot<MyPanacheStaticMetamodelEntity>) query.getRoot();
        query.where(e.get(MyPanacheStaticMetamodelEntity_.name).equalTo(before));
        query.set(e.get(MyPanacheStaticMetamodelEntity_.name), after);
        session.createMutationQuery(query).executeUpdate();
    }

    @DELETE
    @Transactional
    @Path("/by/name/{name}")
    public void deleteByName(@RestPath String name) {
        var b = session.getCriteriaBuilder();
        var query = b.createCriteriaDelete(MyPanacheStaticMetamodelEntity.class);
        // Cast to work around https://hibernate.atlassian.net/browse/HHH-17682
        var e = (JpaRoot<MyPanacheStaticMetamodelEntity>) query.getRoot();
        query.where(e.get(MyPanacheStaticMetamodelEntity_.name).equalTo(name));
        session.createMutationQuery(query).executeUpdate();
    }
}
