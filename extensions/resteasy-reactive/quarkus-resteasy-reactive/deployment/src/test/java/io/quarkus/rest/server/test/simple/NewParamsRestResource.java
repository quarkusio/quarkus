package io.quarkus.rest.server.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestMatrix;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.common.core.QuarkusRestContext;
import org.jboss.resteasy.reactive.server.SimplifiedResourceInfo;
import org.junit.jupiter.api.Assertions;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

@Path("/new-params/{klass}/{regex:[^/]+}")
public class NewParamsRestResource {

    @GET
    @Path("{id}")
    public String get(String klass, String regex, String id) {
        return "GET:" + klass + ":" + regex + ":" + id;
    }

    @POST
    @Path("params/{p}")
    public String params(@RestPath String p,
            @RestQuery String q,
            @RestHeader int h,
            @RestForm String f,
            @RestMatrix String m,
            @RestCookie String c) {
        return "params: p: " + p + ", q: " + q + ", h: " + h + ", f: " + f + ", m: " + m + ", c: " + c;
    }

    @GET
    @Path("context")
    public String context(// Spec:
            UriInfo uriInfo,
            HttpHeaders headers,
            Request request,
            SecurityContext securityContext,
            Providers providers,
            ResourceContext resourceContext,
            Configuration configuration,
            // Extras
            ResourceInfo resourceInfo,
            SimplifiedResourceInfo simplifiedResourceInfo,
            QuarkusRestContext restContext,
            HttpServerRequest httpServerRequest,
            HttpServerResponse httpServerResponse) {
        Assertions.assertNotNull(uriInfo);
        Assertions.assertNotNull(headers);
        Assertions.assertNotNull(request);
        Assertions.assertNotNull(securityContext);
        Assertions.assertNotNull(providers);
        Assertions.assertNotNull(resourceContext);
        Assertions.assertNotNull(configuration);
        Assertions.assertNotNull(resourceInfo);
        Assertions.assertNotNull(simplifiedResourceInfo);
        Assertions.assertNotNull(restContext);
        Assertions.assertNotNull(httpServerRequest);
        Assertions.assertNotNull(httpServerResponse);
        return "OK";
    }

    @GET
    @Path("sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void eventStream(SseEventSink eventSink,
            Sse sse) {
        Assertions.assertNotNull(eventSink);
        Assertions.assertNotNull(sse);
        try (SseEventSink sink = eventSink) {
            eventSink.send(sse.newEvent("OK"));
        }
    }
}
