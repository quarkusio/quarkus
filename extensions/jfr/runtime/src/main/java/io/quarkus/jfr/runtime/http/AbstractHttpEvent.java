package io.quarkus.jfr.runtime.http;

import io.quarkus.jfr.runtime.SpanIdRelational;
import io.quarkus.jfr.runtime.TraceIdRelational;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

public abstract class AbstractHttpEvent extends Event {

    @Label("Trace ID")
    @Description("Trace ID to identify the request")
    @TraceIdRelational
    protected String traceId;

    @Label("Span ID")
    @Description("Span ID to identify the request if necessary")
    @SpanIdRelational
    protected String spanId;

    @Label("HTTP Method")
    @Description("HTTP Method accessed by the client")
    protected String httpMethod;

    @Label("URI")
    @Description("URI accessed by the client")
    protected String uri;

    @Label("Resource Class")
    @Description("Class name executed by Quarkus")
    protected String resourceClass;

    @Label("Resource Method")
    @Description("Method name executed by Quarkus")
    protected String resourceMethod;

    @Label("Client")
    @Description("Client accessed")
    protected String client;

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setResourceClass(String resourceClass) {
        this.resourceClass = resourceClass;
    }

    public void setResourceMethod(String resourceMethod) {
        this.resourceMethod = resourceMethod;
    }

    public void setClient(String client) {
        this.client = client;
    }
}
