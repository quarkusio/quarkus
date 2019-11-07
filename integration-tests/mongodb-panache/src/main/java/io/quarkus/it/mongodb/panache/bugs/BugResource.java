package io.quarkus.it.mongodb.panache.bugs;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
}
