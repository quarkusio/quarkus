package io.quarkus.it.spring.data.jpa;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import io.quarkus.it.spring.data.jpa.PhoneCall.CallAgent;

@Path("/phonecall")
public class PhoneCallResource {

    @Inject
    PhoneCallRepository repository;

    @Path("{areaCode}/{number}")
    @GET
    @Produces("application/json")
    public PhoneCall phoneCallById(@PathParam("areaCode") String areaCode, @PathParam("number") String number) {
        return repository.findById(new PhoneNumberId(areaCode, number)).orElse(null);
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
    public Set<PhoneNumberId> allIds() {
        return repository.findAllIds();
    }

    @Path("call-agents")
    @GET
    @Produces("application/json")
    public Set<CallAgent> allCallAgents() {
        return repository.findAllCallAgents();
    }
}
