package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.smallrye.common.annotation.Blocking;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.util.Optional;
import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestMatrix;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.junit.jupiter.api.Assertions;

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
            @RestQuery Optional<String> q2,
            @RestQuery Optional<Integer> q3,
            @RestHeader int h,
            @RestHeader String xMyHeader,
            @RestHeader("Test-Header-Param") String testHeaderParam,
            @RestHeader("") String paramEmpty,
            @RestForm String f,
            @RestMatrix String m,
            @RestCookie String c) {
        return "params: p: " + p + ", q: " + q + ", h: " + h + ", xMyHeader: " + xMyHeader + ", testHeaderParam: "
                + testHeaderParam + ", paramEmpty: "
                + paramEmpty + ", f: " + f + ", m: " + m + ", c: "
                + c + ", q2: "
                + q2.orElse("empty") + ", q3: " + q3.orElse(-1);
    }

    @Blocking
    @POST
    @Path("form-blocking")
    public String formBlocking(@RestForm String f) {
        if (!BlockingOperationSupport.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return f;
    }

    @GET
    @Path("context")
    public String context(// Spec:
            UriInfo uriInfo,
            HttpHeaders headers,
            Request request,
            Providers providers,
            ResourceContext resourceContext,
            Configuration configuration,
            // Extras
            ResourceInfo resourceInfo,
            SimpleResourceInfo simplifiedResourceInfo,
            ServerRequestContext restContext,
            HttpServerRequest httpServerRequest,
            HttpServerResponse httpServerResponse) {
        Assertions.assertNotNull(uriInfo);
        Assertions.assertNotNull(headers);
        Assertions.assertNotNull(request);
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
