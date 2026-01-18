package io.quarkus.infinispan.client.runtime.jfr.event;

import io.quarkus.jfr.api.SpanIdRelational;
import io.quarkus.jfr.api.TraceIdRelational;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

abstract class AbstractCacheEvent extends Event {

    @Label("Trace ID")
    @Description("Trace ID to identify the request")
    @TraceIdRelational
    protected String traceId;

    @Label("Span ID")
    @Description("Span ID to identify the request if necessary")
    @SpanIdRelational
    protected String spanId;

    @Label("Method")
    @Description("The cache API method that was invoked")
    protected String method;

    @Label("Cache Name")
    @Description("The name of the cache")
    protected String cacheName;

    @Label("Cluster Name")
    @Description("The name of the cluster")
    protected String clusterName;
}
