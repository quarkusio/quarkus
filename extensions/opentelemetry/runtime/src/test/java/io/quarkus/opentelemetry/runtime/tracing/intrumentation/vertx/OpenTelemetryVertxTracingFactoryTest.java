package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;

import org.junit.jupiter.api.Test;

import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.tracing.TracingPolicy;

class OpenTelemetryVertxTracingFactoryTest {

    OpenTelemetryVertxTracingFactory openTelemetryVertxTracingFactory = new OpenTelemetryVertxTracingFactory();

    @Test
    void testTracer() {
        assertNotNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator());
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    @Test
    void testSendRequest() {
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().sendRequest(null, SpanKind.RPC,
                TracingPolicy.IGNORE, createRequest(), "GET", null, null));
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    @Test
    void testSendNullRequest() {
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().sendRequest(null, null, null, null, null,
                null, null));
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    @Test
    void testReceiveRequest() {
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().receiveRequest(null, SpanKind.RPC,
                TracingPolicy.IGNORE, createRequest(), "GET", null, null));
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    @Test
    void testReceiveNullRequest() {
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().receiveRequest(null, null, null, null,
                null, null, null));
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    @Test
    void testSendResponse() {
        assertDoesNotThrow(() -> openTelemetryVertxTracingFactory.getVertxTracerDelegator().sendResponse(null,
                createResponse(), null, new Exception("test"), null));
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    @Test
    void testSendNullResponse() {
        assertDoesNotThrow(() -> openTelemetryVertxTracingFactory.getVertxTracerDelegator().sendResponse(null, null,
                null, null, null));
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    @Test
    void testReceiveResponse() {
        assertDoesNotThrow(() -> openTelemetryVertxTracingFactory.getVertxTracerDelegator().receiveResponse(null,
                createResponse(), null, new Exception("test"), null));
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    @Test
    void testReceiveNullResponse() {
        assertDoesNotThrow(() -> openTelemetryVertxTracingFactory.getVertxTracerDelegator().receiveResponse(null, null,
                null, null, null));
        assertNull(openTelemetryVertxTracingFactory.getVertxTracerDelegator().getDelegate());
    }

    private static Response createResponse() {
        return new Response() {
            @Override
            public int getStatus() {
                return 0;
            }

            @Override
            public StatusType getStatusInfo() {
                return null;
            }

            @Override
            public Object getEntity() {
                return null;
            }

            @Override
            public <T> T readEntity(Class<T> entityType) {
                return null;
            }

            @Override
            public <T> T readEntity(GenericType<T> entityType) {
                return null;
            }

            @Override
            public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
                return null;
            }

            @Override
            public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
                return null;
            }

            @Override
            public boolean hasEntity() {
                return false;
            }

            @Override
            public boolean bufferEntity() {
                return false;
            }

            @Override
            public void close() {

            }

            @Override
            public MediaType getMediaType() {
                return null;
            }

            @Override
            public Locale getLanguage() {
                return null;
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Set<String> getAllowedMethods() {
                return null;
            }

            @Override
            public Map<String, NewCookie> getCookies() {
                return null;
            }

            @Override
            public EntityTag getEntityTag() {
                return null;
            }

            @Override
            public Date getDate() {
                return null;
            }

            @Override
            public Date getLastModified() {
                return null;
            }

            @Override
            public URI getLocation() {
                return null;
            }

            @Override
            public Set<Link> getLinks() {
                return null;
            }

            @Override
            public boolean hasLink(String relation) {
                return false;
            }

            @Override
            public Link getLink(String relation) {
                return null;
            }

            @Override
            public Link.Builder getLinkBuilder(String relation) {
                return null;
            }

            @Override
            public MultivaluedMap<String, Object> getMetadata() {
                return null;
            }

            @Override
            public MultivaluedMap<String, String> getStringHeaders() {
                return null;
            }

            @Override
            public String getHeaderString(String name) {
                return null;
            }
        };
    }

    private static Request createRequest() {
        return new Request() {
            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public Variant selectVariant(List<Variant> variants) {
                return null;
            }

            @Override
            public Response.ResponseBuilder evaluatePreconditions(EntityTag eTag) {
                return null;
            }

            @Override
            public Response.ResponseBuilder evaluatePreconditions(Date lastModified) {
                return null;
            }

            @Override
            public Response.ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
                return null;
            }

            @Override
            public Response.ResponseBuilder evaluatePreconditions() {
                return null;
            }
        };
    }
}
