package io.quarkus.it.mongodb.panache.bugs;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/bugs")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class BugResource {

    @Inject
    Bug5274EntityRepository bug5274EntityRepository;

    @GET
    @Path("5274")
    public String testBug5274() {
        bug5274EntityRepository.count();
        return "OK";
    }

    @Inject
    Bug5885EntityRepository bug5885EntityRepository;

    @GET
    @Path("5885")
    public String testBug5885() {
        bug5885EntityRepository.findById(1L);
        return "OK";
    }

    @Inject
    Bug6324Repository bug6324Repository;

    @GET
    @Path("6324")
    public Response testNeedReflection() {
        return Response.ok(bug6324Repository.listAll()).build();
    }

    @Inject
    Bug6324ConcreteRepository bug6324ConcreteRepository;

    @GET
    @Path("6324/abstract")
    public Response testNeedReflectionAndAbstract() {
        return Response.ok(bug6324ConcreteRepository.listAll()).build();
    }
}
