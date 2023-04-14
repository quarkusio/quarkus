package io.quarkus.observability.promql.client;

import static io.quarkus.observability.promql.client.rest.InstantFormat.Kind.EPOCH_SECONDS;

import java.time.Instant;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.observability.promql.client.data.Dur;
import io.quarkus.observability.promql.client.data.LabelsResponse;
import io.quarkus.observability.promql.client.data.QueryResponse;
import io.quarkus.observability.promql.client.data.SeriesResponse;
import io.quarkus.observability.promql.client.rest.InstantFormat;
import io.quarkus.observability.promql.client.rest.RequestDebugFilter;
import io.quarkus.observability.promql.client.rest.ResponseDebugFilter;

/**
 * You can URL-encode these parameters directly in the request body
 * by using the POST method and Content-Type: application/x-www-form-urlencoded header.
 * This is useful when specifying a large query that may breach server-side URL character limits.
 */
@SuppressWarnings("RestParamTypeInspection")
@RegisterRestClient(configKey = "promql")
@RegisterProvider(RequestDebugFilter.class)
@RegisterProvider(ResponseDebugFilter.class)
public interface PromQLService {

    @GET
    @Path("/api/v1/query")
    QueryResponse getInstantQuery(
            @QueryParam("query") String query,
            @QueryParam("time") @InstantFormat(EPOCH_SECONDS) Instant time,
            @QueryParam("timeout") Dur timeout);

    @POST
    @Path("/api/v1/query")
    @Consumes("application/x-www-form-urlencoded")
    QueryResponse postInstantQuery(
            @FormParam("query") String query,
            @FormParam("time") @InstantFormat(EPOCH_SECONDS) Instant time,
            @FormParam("timeout") Dur timeout);

    @GET
    @Path("/api/v1/query_range")
    QueryResponse getRangeQuery(
            @QueryParam("query") String query,
            @QueryParam("start") @InstantFormat(EPOCH_SECONDS) Instant start,
            @QueryParam("end") @InstantFormat(EPOCH_SECONDS) Instant end,
            @QueryParam("step") Dur step,
            @QueryParam("timeout") Dur timeout);

    @POST
    @Path("/api/v1/query_range")
    @Consumes("application/x-www-form-urlencoded")
    QueryResponse postRangeQuery(
            @FormParam("query") String query,
            @FormParam("start") @InstantFormat(EPOCH_SECONDS) Instant start,
            @FormParam("end") @InstantFormat(EPOCH_SECONDS) Instant end,
            @FormParam("step") Dur step,
            @FormParam("timeout") Dur timeout);

    @GET
    @Path("/api/v1/series")
    SeriesResponse getSeries(
            @QueryParam("match[]") String seriesSelector,
            @QueryParam("start") @InstantFormat(EPOCH_SECONDS) Instant start,
            @QueryParam("end") @InstantFormat(EPOCH_SECONDS) Instant end);

    @POST
    @Path("/api/v1/series")
    @Consumes("application/x-www-form-urlencoded")
    SeriesResponse postSeries(
            @FormParam("match[]") String seriesSelector,
            @FormParam("start") @InstantFormat(EPOCH_SECONDS) Instant start,
            @FormParam("end") @InstantFormat(EPOCH_SECONDS) Instant end);

    @GET
    @Path("/api/v1/labels")
    LabelsResponse getLabels(
            @QueryParam("match[]") String seriesSelector,
            @QueryParam("start") @InstantFormat(EPOCH_SECONDS) Instant start,
            @QueryParam("end") @InstantFormat(EPOCH_SECONDS) Instant end);

    @POST
    @Path("/api/v1/labels")
    @Consumes("application/x-www-form-urlencoded")
    LabelsResponse postLabels(
            @FormParam("match[]") String seriesSelector,
            @FormParam("start") @InstantFormat(EPOCH_SECONDS) Instant start,
            @FormParam("end") @InstantFormat(EPOCH_SECONDS) Instant end);

    @GET
    @Path("/api/v1/label/{label}/values")
    LabelsResponse getLabelValues(
            @PathParam("label") String label,
            @QueryParam("match[]") String seriesSelector,
            @QueryParam("start") @InstantFormat(EPOCH_SECONDS) Instant start,
            @QueryParam("end") @InstantFormat(EPOCH_SECONDS) Instant end);

    @POST
    @Path("/api/v1/label/{label}/values")
    @Consumes("application/x-www-form-urlencoded")
    LabelsResponse postLabelValues(
            @PathParam("label") String label,
            @FormParam("match[]") String seriesSelector,
            @FormParam("start") @InstantFormat(EPOCH_SECONDS) Instant start,
            @FormParam("end") @InstantFormat(EPOCH_SECONDS) Instant end);
}
