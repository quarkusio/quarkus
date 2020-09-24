package io.quarkus.rest.runtime.jaxrs;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import io.quarkus.rest.runtime.util.CaseInsensitiveMap;
import io.quarkus.rest.runtime.util.HeaderHelper;
import io.quarkus.rest.runtime.util.HttpHeaderNames;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.HttpServerRequest;

public class QuarkusRestResponseBuilder extends ResponseBuilder {

    private static final Map<Integer, String> defaultReasonPhrases = new HashMap<>();
    static {
        defaultReasonPhrases.put(200, "OK");
        defaultReasonPhrases.put(201, "Created");
        defaultReasonPhrases.put(202, "Accepted");
        defaultReasonPhrases.put(204, "No Content");
        defaultReasonPhrases.put(205, "Reset Content");
        defaultReasonPhrases.put(206, "Partial Content");
        defaultReasonPhrases.put(301, "Moved Permanently");
        defaultReasonPhrases.put(302, "Found");
        defaultReasonPhrases.put(303, "See Other");
        defaultReasonPhrases.put(304, "Not Modified");
        defaultReasonPhrases.put(305, "Use Proxy");
        defaultReasonPhrases.put(307, "Temporary Redirect");
        defaultReasonPhrases.put(400, "Bad Request");
        defaultReasonPhrases.put(401, "Unauthorized");
        defaultReasonPhrases.put(402, "Payment Required");
        defaultReasonPhrases.put(403, "Forbidden");
        defaultReasonPhrases.put(404, "Not Found");
        defaultReasonPhrases.put(405, "Method Not Allowed");
        defaultReasonPhrases.put(406, "Not Acceptable");
        defaultReasonPhrases.put(407, "Proxy Authentication Required");
        defaultReasonPhrases.put(408, "Request Timeout");
        defaultReasonPhrases.put(409, "Conflict");
        defaultReasonPhrases.put(410, "Gone");
        defaultReasonPhrases.put(411, "Length Required");
        defaultReasonPhrases.put(412, "Precondition Failed");
        defaultReasonPhrases.put(413, "Request Entity Too Large");
        defaultReasonPhrases.put(414, "Request-URI Too Long");
        defaultReasonPhrases.put(415, "Unsupported Media Type");
        defaultReasonPhrases.put(416, "Requested Range Not Satisfiable");
        defaultReasonPhrases.put(417, "Expectation Failed");
        defaultReasonPhrases.put(500, "Internal Server Error");
        defaultReasonPhrases.put(501, "Not Implemented");
        defaultReasonPhrases.put(502, "Bad Gateway");
        defaultReasonPhrases.put(503, "Service Unavailable");
        defaultReasonPhrases.put(504, "Gateway Timeout");
        defaultReasonPhrases.put(505, "HTTP Version Not Supported");
    }

    int status = -1;
    String reasonPhrase;
    Object entity;
    MultivaluedMap<String, Object> metadata = new CaseInsensitiveMap<>();
    Annotation[] entityAnnotations;

    public int getStatus() {
        return status;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public Object getEntity() {
        return entity;
    }

    public Annotation[] getEntityAnnotations() {
        return entityAnnotations;
    }

    public void setEntityAnnotations(Annotation[] entityAnnotations) {
        this.entityAnnotations = entityAnnotations;
    }

    @Override
    public QuarkusRestResponse build() {
        return populateResponse(new QuarkusRestResponse());
    }

    /**
     * Populates a response with the standard data
     * 
     * @return The given response
     */
    public <T extends QuarkusRestResponse> T populateResponse(T response) {
        response.entity = entity;
        if ((entity == null) && (status == -1)) {
            response.status = 204; // spec says that when no status is set and the entity is null, we need to return 204
        } else if (status == -1) {
            response.status = 200;
        } else {
            response.status = status;
        }
        response.reasonPhrase = reasonPhrase;
        response.headers = new CaseInsensitiveMap<>();
        response.headers.putAll(metadata);
        response.entityAnnotations = entityAnnotations;
        return response;
    }

    public void setAllHeaders(MultivaluedMap<String, String> values) {
        for (Map.Entry<String, List<String>> i : values.entrySet()) {
            for (String v : i.getValue()) {
                metadata.add(i.getKey(), v);
            }
        }
    }

    @Override
    public QuarkusRestResponseBuilder clone() {
        QuarkusRestResponseBuilder responseBuilder = new QuarkusRestResponseBuilder();
        responseBuilder.status = status;
        responseBuilder.reasonPhrase = reasonPhrase;
        responseBuilder.entity = entity;
        responseBuilder.metadata = new MultivaluedHashMap<>();
        responseBuilder.metadata.putAll(metadata);
        return responseBuilder;
    }

    @Override
    public Response.ResponseBuilder status(int status, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    @Override
    public Response.ResponseBuilder status(int status) {
        return status(status, defaultReasonPhrases.get(status));
    }

    @Override
    public Response.ResponseBuilder entity(Object entity) {
        this.entity = entity;
        return this;
    }

    @Override
    public Response.ResponseBuilder entity(Object entity, Annotation[] annotations) {
        this.entity = entity;
        this.entityAnnotations = annotations;
        return this;
    }

    @Override
    public Response.ResponseBuilder type(MediaType type) {
        if (type == null) {
            metadata.remove(HttpHeaderNames.CONTENT_TYPE);
            return this;
        }
        metadata.putSingle(HttpHeaderNames.CONTENT_TYPE, type);
        return this;
    }

    @Override
    public Response.ResponseBuilder type(String type) {
        if (type == null) {
            metadata.remove(HttpHeaderNames.CONTENT_TYPE);
            return this;
        }
        metadata.putSingle(HttpHeaderNames.CONTENT_TYPE, type);
        return this;
    }

    @Override
    public Response.ResponseBuilder variant(Variant variant) {
        if (variant == null) {
            type((String) null);
            language((String) null);
            metadata.remove(HttpHeaderNames.CONTENT_ENCODING);
            return this;
        }
        type(variant.getMediaType());
        language(variant.getLanguage());
        if (variant.getEncoding() != null)
            metadata.putSingle(HttpHeaderNames.CONTENT_ENCODING, variant.getEncoding());
        else
            metadata.remove(HttpHeaderNames.CONTENT_ENCODING);
        return this;
    }

    @Override
    public Response.ResponseBuilder variants(List<Variant> variants) {
        if (variants == null) {
            metadata.remove(HttpHeaderNames.VARY);
            return this;
        }
        String vary = createVaryHeader(variants);
        metadata.putSingle(HttpHeaderNames.VARY, vary);

        return this;
    }

    @Override
    public Response.ResponseBuilder language(String language) {
        if (language == null) {
            metadata.remove(HttpHeaderNames.CONTENT_LANGUAGE);
            return this;
        }
        metadata.putSingle(HttpHeaderNames.CONTENT_LANGUAGE, language);
        return this;
    }

    @Override
    public Response.ResponseBuilder location(URI location) {
        if (location == null) {
            metadata.remove(HttpHeaderNames.LOCATION);
            return this;
        }
        if (!location.isAbsolute()) {
            CDI<Object> cdi = null;
            try {
                cdi = CDI.current();
            } catch (IllegalStateException ignored) {

            }
            if (cdi != null) {
                // FIXME: this leaks server stuff onto the client
                CurrentVertxRequest cur = cdi.select(CurrentVertxRequest.class).get();
                HttpServerRequest req = cur.getCurrent().request();
                try {
                    String host = req.host();
                    int port = -1;
                    int index = host.indexOf(":");
                    if (index > -1) {
                        port = Integer.parseInt(host.substring(index + 1));
                        host = host.substring(0, index);
                    }
                    location = new URI(req.scheme(), null, host, port,
                            location.getPath().startsWith("/") ? location.getPath() : "/" + location.getPath(),
                            location.getQuery(), null);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        metadata.putSingle(HttpHeaderNames.LOCATION, location);
        return this;
    }

    @Override
    public Response.ResponseBuilder contentLocation(URI location) {
        if (location == null) {
            metadata.remove(HttpHeaderNames.CONTENT_LOCATION);
            return this;
        }
        if (!location.isAbsolute()) {
            CDI<Object> cdi = null;
            try {
                cdi = CDI.current();
            } catch (IllegalStateException ignored) {

            }
            if (cdi != null) {
                // FIXME: this leaks server stuff onto the client
                CurrentVertxRequest cur = CDI.current().select(CurrentVertxRequest.class).get();
                HttpServerRequest req = cur.getCurrent().request();
                try {
                    String host = req.host();
                    int port = -1;
                    int index = host.indexOf(":");
                    if (index > -1) {
                        port = Integer.parseInt(host.substring(index + 1));
                        host = host.substring(0, index);
                    }
                    location = new URI(req.scheme(), null, host, port,
                            location.getPath().startsWith("/") ? location.getPath() : "/" + location.getPath(),
                            location.getQuery(), null);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        metadata.putSingle(HttpHeaderNames.CONTENT_LOCATION, location);
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(EntityTag tag) {
        if (tag == null) {
            metadata.remove(HttpHeaderNames.ETAG);
            return this;
        }
        metadata.putSingle(HttpHeaderNames.ETAG, tag);
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(String tag) {
        if (tag == null) {
            metadata.remove(HttpHeaderNames.ETAG);
            return this;
        }
        return tag(new EntityTag(tag));
    }

    @Override
    public Response.ResponseBuilder lastModified(Date lastModified) {
        if (lastModified == null) {
            metadata.remove(HttpHeaderNames.LAST_MODIFIED);
            return this;
        }
        metadata.putSingle(HttpHeaderNames.LAST_MODIFIED, lastModified);
        return this;
    }

    @Override
    public Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
        if (cacheControl == null) {
            metadata.remove(HttpHeaderNames.CACHE_CONTROL);
            return this;
        }
        metadata.putSingle(HttpHeaderNames.CACHE_CONTROL, cacheControl);
        return this;
    }

    @Override
    public Response.ResponseBuilder header(String name, Object value) {
        if (value == null) {
            metadata.remove(name);
            return this;
        }
        metadata.add(name, value);
        return this;
    }

    @Override
    public Response.ResponseBuilder cookie(NewCookie... cookies) {
        if (cookies == null) {
            metadata.remove(HttpHeaderNames.SET_COOKIE);
            return this;
        }
        for (NewCookie cookie : cookies) {
            metadata.add(HttpHeaderNames.SET_COOKIE, cookie);
        }
        return this;
    }

    public Response.ResponseBuilder language(Locale language) {
        if (language == null) {
            metadata.remove(HttpHeaderNames.CONTENT_LANGUAGE);
            return this;
        }
        metadata.putSingle(HttpHeaderNames.CONTENT_LANGUAGE, language);
        return this;
    }

    public static SimpleDateFormat getDateFormatRFC822() {
        SimpleDateFormat dateFormatRFC822 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormatRFC822.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormatRFC822;
    }

    public Response.ResponseBuilder expires(Date expires) {
        if (expires == null) {
            metadata.remove(HttpHeaderNames.EXPIRES);
            return this;
        }
        metadata.putSingle(HttpHeaderNames.EXPIRES, getDateFormatRFC822().format(expires));
        return this;
    }
    // spec

    public Response.ResponseBuilder allow(String... methods) {
        if (methods == null) {
            return allow((Set<String>) null);
        }
        HashSet<String> set = new HashSet<>(Arrays.asList(methods));
        return allow(set);
    }

    public Response.ResponseBuilder allow(Set<String> methods) {
        HeaderHelper.setAllow(this.metadata, methods);
        return this;
    }

    @Override
    public Response.ResponseBuilder encoding(String encoding) {
        if (encoding == null) {
            metadata.remove(HttpHeaders.CONTENT_ENCODING);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_ENCODING, encoding);
        return this;
    }

    @Override
    public Response.ResponseBuilder variants(Variant... variants) {
        return this.variants(Arrays.asList(variants));
    }

    @Override
    public Response.ResponseBuilder links(Link... links) {
        if (links == null) {
            metadata.remove(HttpHeaders.LINK);
            return this;
        }
        for (Link link : links) {
            metadata.add(HttpHeaders.LINK, link);
        }
        return this;
    }

    @Override
    public Response.ResponseBuilder link(URI uri, String rel) {
        Link link = Link.fromUri(uri).rel(rel).build();
        metadata.add(HttpHeaders.LINK, link);
        return this;
    }

    @Override
    public Response.ResponseBuilder link(String uri, String rel) {
        Link link = Link.fromUri(uri).rel(rel).build();
        metadata.add(HttpHeaders.LINK, link);
        return this;
    }

    @Override
    public Response.ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
        metadata.clear();
        if (headers == null)
            return this;
        metadata.putAll(headers);
        return this;
    }

    public MultivaluedMap<String, Object> getMetadata() {
        return metadata;
    }

    public static String createVaryHeader(List<Variant> variants) {
        boolean accept = false;
        boolean acceptLanguage = false;
        boolean acceptEncoding = false;

        for (Variant variant : variants) {
            if (variant.getMediaType() != null)
                accept = true;
            if (variant.getLanguage() != null)
                acceptLanguage = true;
            if (variant.getEncoding() != null)
                acceptEncoding = true;
        }

        String vary = null;
        if (accept)
            vary = HttpHeaderNames.ACCEPT;
        if (acceptLanguage) {
            if (vary == null)
                vary = HttpHeaderNames.ACCEPT_LANGUAGE;
            else
                vary += ", " + HttpHeaderNames.ACCEPT_LANGUAGE;
        }
        if (acceptEncoding) {
            if (vary == null)
                vary = HttpHeaderNames.ACCEPT_ENCODING;
            else
                vary += ", " + HttpHeaderNames.ACCEPT_ENCODING;
        }
        return vary;
    }
}
