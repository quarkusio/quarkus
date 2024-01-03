package io.quarkus.resteasy.reactive.links.deployment;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.smallrye.mutiny.Uni;

@Path("/recordsNoId")
@Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
public class TestResourceNoId {

    private static final List<TestRecordNoId> RECORDS = new LinkedList<>(Arrays.asList(
            new TestRecordNoId("first_value"),
            new TestRecordNoId("second_value")));

    @GET

    @RestLink(entityType = TestRecordNoId.class)
    @InjectRestLinks
    public Uni<List<TestRecordNoId>> getAll() {
        return Uni.createFrom().item(RECORDS).onItem().delayIt().by(Duration.ofMillis(100));
    }

    @GET
    @Path("/by-name/{name}")
    @RestLink(entityType = TestRecordNoId.class)
    @InjectRestLinks(RestLinkType.INSTANCE)
    public TestRecordNoId getByNothing(@PathParam("name") String name) {
        return RECORDS.stream()
                .filter(record -> record.getName().equals(name))
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }
}
