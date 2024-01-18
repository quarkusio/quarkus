package io.quarkus.restclient.jackson.deployment;

import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
                && zonedDateTime.getZone().equals(ZoneId.of("Europe/London"))) {
            return "OK";
        }

        return "INVALID";
    }
}
