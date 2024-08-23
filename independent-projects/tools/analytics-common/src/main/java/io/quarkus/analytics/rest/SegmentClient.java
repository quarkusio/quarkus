package io.quarkus.analytics.rest;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import io.quarkus.analytics.dto.config.Identity;
import io.quarkus.analytics.dto.segment.Track;

/**
 * Client to post the analytics data to the upstream collection tool.
 */
public interface SegmentClient {
    /**
     * Posts the anonymous identity to the upstream collection tool.
     * Usually this is done once per user's UUID
     *
     * @param identity
     */
    CompletableFuture<HttpResponse<String>> postIdentity(final Identity identity);

    /**
     * Posts the trace to the upstream collection tool.
     * This contains the actual data to be collected.
     *
     * @param track
     */
    CompletableFuture<HttpResponse<String>> postTrack(final Track track);
}
