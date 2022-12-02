package io.quarkus.it.jackson;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
