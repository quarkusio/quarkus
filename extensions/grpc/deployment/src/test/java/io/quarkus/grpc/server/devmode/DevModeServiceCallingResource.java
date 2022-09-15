package io.quarkus.grpc.server.devmode;

import java.time.Duration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import devmodetest.v1.DevModeService;
import devmodetest.v1.Devmodetest;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;

@Path("/dev-mode-test")
public class DevModeServiceCallingResource {

    @GrpcClient("devmode-client")
    DevModeService client;

    /**
     * the body of this method gets replaced with calling a method from DevModeService (that would be added in dev mode)
     * i.e. {@code return responseFor(client.streamCheck(request));}
     */
    @GET
    public String get() {
        Devmodetest.DevModeRequest request = Devmodetest.DevModeRequest.newBuilder()
                .setService("my-service").build();
        return "ORIGINAL_GET";
    }

    String responseFor(Multi<Devmodetest.DevModeResponse> response) {
        Devmodetest.DevModeResponse r = response.toUni().await().atMost(Duration.ofSeconds(10));
        return r.getStatus() == Devmodetest.DevModeResponse.Status.NOT_SERVING
                ? "OKAY"
                : "UNEXPECTED_STATUS: " + r.getStatus();
    }
}
