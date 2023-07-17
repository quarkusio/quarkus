package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/datedeserializers")
public class DateDeserializerPojoResource {

    @Inject
    private ObjectMapper objectMapper;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sql/timestamp")
    public String timestamp(String body) throws IOException {
        SqlTimestampPojo model = objectMapper.readValue(body, SqlTimestampPojo.class);
        return objectMapper.writeValueAsString(model);
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sql/date")
    public String date(String body) throws IOException {
        SqlDatePojo model = objectMapper.readValue(body, SqlDatePojo.class);
        return objectMapper.writeValueAsString(model);
    }

}
