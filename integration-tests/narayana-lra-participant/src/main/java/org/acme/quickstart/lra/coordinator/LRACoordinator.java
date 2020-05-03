package org.acme.quickstart.lra.coordinator;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

//import io.narayana.lra.coordinator.api.Coordinator;

//@Path("lra-coordinator")
public class LRACoordinator {//extends Coordinator {
    @POST
    @Path("start")
    @Produces({ "text/plain" })
    public Response startLRA(@QueryParam("ClientID") @DefaultValue("") String clientId,
            @QueryParam("TimeLimit") @DefaultValue("0") Long timelimit,
            @QueryParam("ParentLRA") @DefaultValue("") String parentLRA, @HeaderParam("Long-Running-Action") String parentId)
            throws WebApplicationException {
        Response response = null;//super.startLRA(clientId, timelimit, parentLRA, parentId);
        return response;
    }
}
