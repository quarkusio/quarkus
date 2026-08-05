package io.quarkus.it.spring.tx;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/spring-tx")
@Produces(MediaType.TEXT_PLAIN)
public class TransactionalResource {

    @Inject
    TransactionalService service;

    @Inject
    ClassLevelTransactionalService classLevelService;

    @GET
    @Path("/default")
    public boolean defaultTx() throws Exception {
        return service.defaultTx();
    }

    @GET
    @Path("/requires-new")
    public boolean requiresNew() throws Exception {
        return service.requiresNewTx();
    }

    @GET
    @Path("/supports")
    public boolean supports() throws Exception {
        return service.supportsTx();
    }

    @GET
    @Path("/never")
    public boolean never() throws Exception {
        return service.neverTx();
    }

    @GET
    @Path("/class-level/one")
    public boolean classLevelOne() throws Exception {
        return classLevelService.methodOne();
    }

    @GET
    @Path("/class-level/two")
    public boolean classLevelTwo() throws Exception {
        return classLevelService.methodTwo();
    }

    @GET
    @Path("/rollback-for")
    public String rollbackFor() {
        try {
            service.rollbackFor();
            return "NO_EXCEPTION";
        } catch (IllegalArgumentException e) {
            return "ROLLED_BACK";
        }
    }
}
