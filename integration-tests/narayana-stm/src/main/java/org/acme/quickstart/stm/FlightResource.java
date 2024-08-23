package org.acme.quickstart.stm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/stm")
@RequestScoped
public class FlightResource {
    @Inject
    FlightServiceFactory factory;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> bookingCount() {
        return CompletableFuture.supplyAsync(
                () -> getInfo(factory.getInstance()));
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> asynBook() {
        return CompletableFuture.supplyAsync(() -> {
            FlightService flightService = factory.getInstance();

            flightService.makeBooking("BA123");

            return getInfo(flightService);
        });
    }

    @POST
    @Path("sync")
    @Produces(MediaType.TEXT_PLAIN)
    public String book() {
        FlightService flightService = factory.getInstance();

        flightService.makeBooking("BA123");

        return getInfo(flightService);
    }

    private String getInfo(FlightService flightService) {
        return Thread.currentThread().getName()
                + ":  Booking Count=" + flightService.getNumberOfBookings();
    }
}
