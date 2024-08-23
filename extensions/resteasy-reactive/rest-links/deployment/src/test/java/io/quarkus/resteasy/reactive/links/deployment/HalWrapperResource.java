package io.quarkus.resteasy.reactive.links.deployment;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.quarkus.hal.HalCollectionWrapper;
import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalService;
import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;

@Path("/hal")
public class HalWrapperResource {

    @Inject
    HalService halService;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(rel = "list")
    @InjectRestLinks
    public HalCollectionWrapper<TestRecordWithIdAndPersistenceIdAndRestLinkId> getRecords(@Context UriInfo uriInfo) {
        List<TestRecordWithIdAndPersistenceIdAndRestLinkId> items = List.of(
                new TestRecordWithIdAndPersistenceIdAndRestLinkId(1, 10, 100, "one"),
                new TestRecordWithIdAndPersistenceIdAndRestLinkId(2, 20, 200, "two"));

        HalCollectionWrapper<TestRecordWithIdAndPersistenceIdAndRestLinkId> halCollection = halService.toHalCollectionWrapper(
                items,
                "collectionName", TestRecordWithIdAndPersistenceIdAndRestLinkId.class);
        halCollection.addLinks(
                Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path(String.format("/hal/%d", 1))).rel("first-record").build());

        return halCollection;
    }

    @GET
    @Path("/{id}")
    @Produces({ MediaType.APPLICATION_JSON, RestMediaType.APPLICATION_HAL_JSON })
    @RestLink(rel = "self")
    @InjectRestLinks(RestLinkType.INSTANCE)
    public HalEntityWrapper<TestRecordWithIdAndPersistenceIdAndRestLinkId> getRecord(@PathParam("id") int id,
            @Context UriInfo uriInfo) {

        HalEntityWrapper<TestRecordWithIdAndPersistenceIdAndRestLinkId> halEntity = halService.toHalWrapper(
                new TestRecordWithIdAndPersistenceIdAndRestLinkId(1, 10, 100, "one"));
        halEntity.addLinks(Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path(String.format("/hal/%d/parent", id)))
                .rel("parent-record").build());

        return halEntity;
    }
}
