package io.quarkus.resteasy.links.deployment;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.links.LinkResource;

@Path("/records")
public class TestResource {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private static final List<TestRecord> RECORDS = new LinkedList<>(Arrays.asList(
            new TestRecord(ID_COUNTER.incrementAndGet(), "first", "First value"),
            new TestRecord(ID_COUNTER.incrementAndGet(), "second", "Second value")));

    @GET
    @Produces({ MediaType.APPLICATION_JSON, "application/hal+json" })
    @LinkResource(entityClassName = "io.quarkus.resteasy.links.deployment.TestRecord", rel = "list")
    public List<TestRecord> getAll() {
        return RECORDS;
    }

    @GET
    @Path("/first")
    @Produces({ MediaType.APPLICATION_JSON, "application/hal+json" })
    @LinkResource(rel = "first")
    public TestRecord getFirst() {
        return RECORDS.get(0);
    }
}
