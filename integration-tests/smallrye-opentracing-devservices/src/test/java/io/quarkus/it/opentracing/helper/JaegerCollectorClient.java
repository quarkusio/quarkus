package io.quarkus.it.opentracing.helper;

import java.util.List;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

@RegisterRestClient(configKey = "collector-api")
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface JaegerCollectorClient {

    @GET
    @Path("/traces")
    @Traced(value = false)
    public JaegerCollectorResponse getTracedService(@QueryParam("service") String service,
            @QueryParam("start") Long startTimeMicro);

    @GET
    @Path("/services")
    @Traced(value = false)
    public Services getServices();

    public static class Services {

        @JsonProperty("data")
        public List<String> services;

        public List<String> getServices() {
            return services;
        }
    }
}
