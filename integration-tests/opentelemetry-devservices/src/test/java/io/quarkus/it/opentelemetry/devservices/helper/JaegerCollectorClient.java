package io.quarkus.it.opentelemetry.devservices.helper;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "collector-api")
public interface JaegerCollectorClient {

    @GET
    @Path("/traces")
    public JaegerCollectorResponse getTracedService(@QueryParam("service") String service,
            @QueryParam("start") Long startTimeMicro);

    @GET
    @Path("/services")
    public Services getServices();

    public static class Services {

        @JsonProperty("data")
        public List<String> services;

        public List<String> getServices() {
            return services;
        }
    }
}
