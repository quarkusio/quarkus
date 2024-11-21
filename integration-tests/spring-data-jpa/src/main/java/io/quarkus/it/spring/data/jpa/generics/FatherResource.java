package io.quarkus.it.spring.data.jpa.generics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/rest/fathers")
@ApplicationScoped
public class FatherResource {
    private final FatherBaseRepository<Father> fatherRepository;

    public FatherResource(FatherBaseRepository<Father> fatherRepository) {
        this.fatherRepository = fatherRepository;
    }

    @GET
    @Path("/{childName}")
    @Produces(MediaType.APPLICATION_JSON)
    public long getParents(@PathParam("childName") String childName) {
        long nameFathers = fatherRepository.countParentsByChildren_Name(childName);
        return nameFathers;
    }

}
