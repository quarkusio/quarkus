package io.quarkus.it.spring.data.jpa;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import io.quarkus.it.spring.data.jpa.PhoneCall.CallAgent;

@Path("/phonecall")
public class PhoneCallResource {

    @Inject
    PhoneCallRepository repository;

    @Path("{areaCode}/{number}")
    @GET
    @Produces("application/json")
    public PhoneCall phoneCallById(@PathParam("areaCode") String areaCode, @PathParam("number") String number) {
        return repository.findById(new PhoneCallId(areaCode, number)).orElse(null);
    }

    @Path("{areaCode}")
    @GET
    @Produces("application/json")
    public PhoneCall phoneCallByAreaCode(@PathParam("areaCode") String areaCode) {
        return repository.findByIdAreaCode(areaCode);
    }

    @Path("ids")
    @GET
    @Produces("application/json")
    public Set<PhoneCallId> allIds() {
        return repository.findAllIds();
    }

    @Path("call-agents")
    @GET
    @Produces("application/json")
    public Set<CallAgent> allCallAgents() {
        return repository.findAllCallAgents();
    }
}
