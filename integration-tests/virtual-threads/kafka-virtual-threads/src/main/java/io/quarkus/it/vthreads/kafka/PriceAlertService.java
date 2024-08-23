package io.quarkus.it.vthreads.kafka;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("price")
@RegisterRestClient(configKey = "price-alert")
public interface PriceAlertService {

    @Path("alert")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    String alert(double value);

    @Path("alert-message")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    String alertMessage(double value);
}
