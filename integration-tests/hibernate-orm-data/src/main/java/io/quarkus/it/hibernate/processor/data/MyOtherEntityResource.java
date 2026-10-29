package io.quarkus.it.hibernate.processor.data;

import java.util.List;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.it.hibernate.processor.data.puother.MyOtherEntity;
import io.quarkus.it.hibernate.processor.data.puother.MyOtherEntity_;
import io.quarkus.it.hibernate.processor.data.puother.MyOtherRepository;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/data/other")
public class MyOtherEntityResource {

    @Inject
    MyOtherRepository repository;

    @Inject
    @PersistenceUnit("other")
    EntityManager entityManager;

    @POST
    @Transactional
    public void create(MyOtherEntity entity) {
        repository.insert(entity);
    }

    @GET
    @Path("/unannotated/by/name/{name}")
    public List<MyOtherEntity> getByNameFromUnannotatedRepository(@RestPath String name) {
        // The named query is only registered in the "other" persistence unit if the unannotated repository was
        // assigned to it through the package-based rules.
        return entityManager.createNamedQuery(
                "io.quarkus.it.hibernate.processor.data.puother.UnannotatedMyOtherRepository#findByName(java.lang.String)",
                MyOtherEntity.class).setParameter("name", name).getResultList();
    }

    @GET
    public List<MyOtherEntity> get() {
        return repository.findAll(Order.by(Sort.asc(MyOtherEntity_.NAME))).toList();
    }

    @GET
    @Transactional
    @Path("/by/name/{name}")
    public MyOtherEntity getByName(@RestPath String name) {
        List<MyOtherEntity> entities = repository.findByName(name);
        if (entities.isEmpty()) {
            throw new NotFoundException();
        }
        return entities.get(0);
    }

    @POST
    @Transactional
    @Path("/rename/{before}/to/{after}")
    public void rename(@RestPath String before, @RestPath String after) {
        MyOtherEntity byName = getByName(before);
        byName.name = after;
        repository.update(byName);
    }

    @DELETE
    @Transactional
    @Path("/by/name/{name}")
    public void deleteByName(@RestPath String name) {
        repository.delete(name);
    }
}
