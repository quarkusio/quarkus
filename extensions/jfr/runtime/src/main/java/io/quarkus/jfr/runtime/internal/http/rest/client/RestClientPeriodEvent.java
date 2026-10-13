package io.quarkus.jfr.runtime.internal.http.rest.client;

import io.quarkus.jfr.api.SpanIdRelational;
import io.quarkus.jfr.api.TraceIdRelational;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Label("REST Client")
@Category({ "Quarkus", "HTTP" })
@Name("quarkus.RestClient")
@Description("REST Client has been invoking a remote endpoint during this period")
@StackTrace(false)
public class RestClientPeriodEvent extends Event {

    @Label("Trace ID")
    @Description("Trace ID to identify the request")
    @TraceIdRelational
    String traceId;

    @Label("Span ID")
    @Description("Span ID to identify the request if necessary")
    @SpanIdRelational
    String spanId;

    @Label("HTTP Method")
    @Description("HTTP Method of the outgoing request")
    String httpMethod;

    @Label("URI")
    @Description("URI path template of the invoked REST Client method, or the URI path if no template is available")
    String uri;

    @Label("Address")
    @Description("Host and port of the remote endpoint")
    String address;

    @Label("Client Name")
    @Description("Config key of the REST Client that sent the request")
    String clientName;

    @Label("Status")
    @Description("HTTP status code of the response, or 0 if no response was received")
    int status;

    @Label("Failure")
    @Description("Class name of the exception if the request failed")
    String failure;
}
