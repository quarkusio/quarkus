package io.quarkus.it.hibernate.jpamodelgen.data;

import java.util.List;

import jakarta.data.Order;
import jakarta.data.Sort;
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

import org.jboss.resteasy.reactive.RestPath;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/data/")
public class MyEntityResource {

    @Inject
    MyRepository repository;

    @POST
    @Transactional
    public void create(MyEntity entity) {
        repository.insert(entity);
    }

    @GET
    public List<MyEntity> get() {
        return repository.findAll(Order.by(Sort.asc(MyEntity_.NAME))).toList();
    }

    @GET
    @Transactional
    @Path("/by/name/{name}")
    public MyEntity getByName(@RestPath String name) {
        List<MyEntity> entities = repository.findByName(name);
        if (entities.isEmpty()) {
            throw new NotFoundException();
        }
        return entities.get(0);
    }

    @POST
    @Transactional
    @Path("/rename/{before}/to/{after}")
    public void rename(@RestPath String before, @RestPath String after) {
        MyEntity byName = getByName(before);
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
