package io.quarkus.it.spring.data.jpa.generics;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/rest/multi-type-param-fathers")
@ApplicationScoped
public class MultiTypeParamResource {
    private final MultiTypeParamFatherRepository repository;

    public MultiTypeParamResource(MultiTypeParamFatherRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("/findAllTestById/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public int findAllTestById(@PathParam("id") Long id) {
        List<Father> fathers = repository.findAllTestById(id);
        return fathers.size();
    }

    @GET
    @Path("/findSomethingByIdAndName/{id}/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String findSomethingByIdAndName(@PathParam("id") Long id, @PathParam("name") String name) {
        Optional<MultiTypeParamBaseRepository.SmallParent> result = repository.findSomethingByIdAndName(id, name);
        return result.map(p -> p.getAge() + ":" + p.getTest()).orElse("empty");
    }

    @GET
    @Path("/findChildrenById/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public int findChildrenById(@PathParam("id") Long id) {
        MultiTypeParamBaseRepository.GenericParent<Child> result = repository.findChildrenById(id);
        return result.getChildren().size();
    }
}
