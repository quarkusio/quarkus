package io.quarkus.observability.victoriametrics.client;

import static io.prometheus.client.exporter.common.TextFormat.CONTENT_TYPE_OPENMETRICS_100;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.prometheus.client.Collector;
import io.prometheus.client.exporter.common.TextFormat;
import io.quarkus.observability.promql.client.PromQLService;
import io.quarkus.observability.promql.client.rest.RequestDebugFilter;
import io.quarkus.observability.promql.client.rest.ResponseDebugFilter;
import io.quarkus.runtime.util.EnumerationUtil;

/**
 * VictoriaMetrics specific extension of {@link PromQLService}.
 */
@RegisterRestClient(configKey = "victoriametrics")
@RegisterProvider(RequestDebugFilter.class)
@RegisterProvider(ResponseDebugFilter.class)
public interface VictoriaMetricsService extends PromQLService {

    @POST
    @Path("/api/v1/import/prometheus")
    @Consumes(CONTENT_TYPE_OPENMETRICS_100)
    void importPrometheus(String openmetricsText);

    static void importPrometheus(VictoriaMetricsService service, Stream<? extends Collector> collectors) {
        var sw = new StringWriter();
        try {
            TextFormat.writeFormat(
                    CONTENT_TYPE_OPENMETRICS_100,
                    sw,
                    EnumerationUtil.from(collectors.flatMap(c -> c.collect().stream())));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        service.importPrometheus(sw.toString());
    }

    /**
     * Delete time series matching the given selector. Storage space for the deleted time series
     * isn't freed instantly - it is freed during subsequent background merges of data files.
     * Note that background merges may never occur for data from previous months, so storage space
     * won't be freed for historical data. In this case forced merge may help to free up storage space.
     *
     * @param seriesSelector the selector for series to be deleted
     */
    @POST
    @Path("/api/v1/admin/tsdb/delete_series")
    @Consumes("application/x-www-form-urlencoded")
    void deleteSeries(
            @FormParam("match[]") String seriesSelector);

    /**
     * Triggers compaction (forced merge) for specified month partition.
     * Returns immediately while compaction is performed in the background.
     *
     * @param partition the month partition to compact (force-merge)
     *        in format YYYY_MM
     */
    @POST
    @Path("/internal/force_merge")
    @Consumes("application/x-www-form-urlencoded")
    void compactMonthPartition(
            @FormParam("partition_prefix") String partition);

    /**
     * Data becomes available for querying in a few seconds after inserting.
     * It is possible to flush in-memory buffers to persistent storage.
     * This handler is mostly needed for testing and debugging purposes.
     */
    @POST
    @Path("/internal/force_flush")
    void flush();

    /**
     * If you see gaps on the graphs, try resetting the cache.
     * If this removes gaps on the graphs, then it is likely data with timestamps
     * older than -search.cacheTimestampOffset is ingested into VictoriaMetrics.
     * Make sure that data sources have synchronized time with VictoriaMetrics.
     */
    @POST
    @Path("/internal/resetRollupResultCache")
    void resetRollupResultCache();
}
