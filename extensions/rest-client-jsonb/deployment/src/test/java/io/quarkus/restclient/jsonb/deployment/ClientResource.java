package io.quarkus.restclient.jsonb.deployment;

import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/client")
public class ClientResource {

    @Inject
    @RestClient
    RestInterface restInterface;

    @GET
    @Path("/hello")
    public String hello() {
        DateDto dateDto = restInterface.get();
        ZonedDateTime zonedDateTime = dateDto.getDate();

        if (zonedDateTime.getMonth().equals(Month.NOVEMBER)
                && zonedDateTime.getZone().equals(ZoneId.of("Europe/Paris"))) {
            return "OK";
        }

        return "INVALID";
    }
}
