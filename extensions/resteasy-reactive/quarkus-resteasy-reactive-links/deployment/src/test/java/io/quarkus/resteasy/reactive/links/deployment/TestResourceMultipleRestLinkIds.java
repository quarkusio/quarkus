package io.quarkus.resteasy.reactive.links.deployment;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.smallrye.mutiny.Uni;

@Path("/recordsMultipleRestLinkIds")
@Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
public class TestResourceMultipleRestLinkIds {

    private static final List<TestRecordMultipleRestLinkIds> RECORDS = new LinkedList<>(Arrays.asList(
            new TestRecordMultipleRestLinkIds(10, 20, "first_value"),
            new TestRecordMultipleRestLinkIds(11, 22, "second_value")));

    @GET
    @RestLink(entityType = TestRecordMultipleRestLinkIds.class)
    @InjectRestLinks
    public Uni<List<TestRecordMultipleRestLinkIds>> getAll() {
        return Uni.createFrom().item(RECORDS).onItem().delayIt().by(Duration.ofMillis(100));
    }
}
