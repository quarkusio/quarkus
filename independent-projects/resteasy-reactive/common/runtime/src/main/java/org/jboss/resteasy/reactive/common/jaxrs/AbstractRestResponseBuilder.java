package org.jboss.resteasy.reactive.common.jaxrs;

import java.lang.annotation.Annotation;
import java.net.URI;
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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Variant;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

public abstract class AbstractRestResponseBuilder<T> extends RestResponse.ResponseBuilder<T> {

    static final Map<Integer, String> defaultReasonPhrases = new HashMap<>();
    static {
        defaultReasonPhrases.put(100, "Continue");
        defaultReasonPhrases.put(101, "Switching Protocols");
        defaultReasonPhrases.put(200, "OK");
        defaultReasonPhrases.put(201, "Created");
        defaultReasonPhrases.put(202, "Accepted");
        defaultReasonPhrases.put(203, "Non-Authoritative Information");
        defaultReasonPhrases.put(204, "No Content");
        defaultReasonPhrases.put(205, "Reset Content");
        defaultReasonPhrases.put(206, "Partial Content");
        defaultReasonPhrases.put(300, "Multiple Choices");
        defaultReasonPhrases.put(301, "Moved Permanently");
        defaultReasonPhrases.put(302, "Found");
        defaultReasonPhrases.put(303, "See Other");
        defaultReasonPhrases.put(304, "Not Modified");
        defaultReasonPhrases.put(305, "Use Proxy");
        defaultReasonPhrases.put(307, "Temporary Redirect");
        defaultReasonPhrases.put(308, "Permanent Redirect");
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
        defaultReasonPhrases.put(426, "Upgrade Required");
        defaultReasonPhrases.put(428, "Expectation Failed");
        defaultReasonPhrases.put(429, "Precondition Required");
        defaultReasonPhrases.put(431, "Too Many Requests");
        defaultReasonPhrases.put(500, "Internal Server Error");
        defaultReasonPhrases.put(501, "Not Implemented");
        defaultReasonPhrases.put(502, "Bad Gateway");
        defaultReasonPhrases.put(503, "Service Unavailable");
        defaultReasonPhrases.put(504, "Gateway Timeout");
        defaultReasonPhrases.put(505, "HTTP Version Not Supported");
        defaultReasonPhrases.put(511, "Network Authentication Required");
    }

    protected int status = -1;
    protected String reasonPhrase;
    protected Object entity;
    protected MultivaluedTreeMap<String, Object> metadata = new CaseInsensitiveMap<>();
    protected Annotation[] entityAnnotations;

    public static SimpleDateFormat getDateFormatRFC822() {
        SimpleDateFormat dateFormatRFC822 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormatRFC822.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormatRFC822;
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
            vary = HttpHeaders.ACCEPT;
        if (acceptLanguage) {
            if (vary == null)
                vary = HttpHeaders.ACCEPT_LANGUAGE;
            else
                vary += ", " + HttpHeaders.ACCEPT_LANGUAGE;
        }
        if (acceptEncoding) {
            if (vary == null)
                vary = HttpHeaders.ACCEPT_ENCODING;
            else
                vary += ", " + HttpHeaders.ACCEPT_ENCODING;
        }
        return vary;
    }

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
    public RestResponseImpl<T> build() {
        return populateResponse(new RestResponseImpl<T>());
    }

    public RestResponseImpl<T> build(boolean copyHeaders) {
        return populateResponse(new RestResponseImpl<T>(), copyHeaders);
    }

    /**
     * Populates a response with the standard data
     *
     * @return The given response
     */
    public <OtherT extends RestResponseImpl<T>> OtherT populateResponse(OtherT response) {
        return populateResponse(response, true);
    }

    @SuppressWarnings("unchecked")
    public <OtherT extends RestResponseImpl<T>> OtherT populateResponse(OtherT response, boolean copyHeaders) {
        response.entity = (T) entity;
        if ((entity == null) && (status == -1)) {
            response.status = 204; // spec says that when no status is set and the entity is null, we need to return 204
        } else if (status == -1) {
            response.status = 200;
        } else {
            response.status = status;
        }
        response.reasonPhrase = reasonPhrase;
        if (copyHeaders) {
            response.headers = new CaseInsensitiveMap<>();
            response.headers.putAll(metadata);
        } else {
            response.headers = metadata;
        }
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

    protected abstract AbstractRestResponseBuilder<T> doClone();

    @Override
    public AbstractRestResponseBuilder<T> clone() {
        AbstractRestResponseBuilder<T> responseBuilder = doClone();
        responseBuilder.status = status;
        responseBuilder.reasonPhrase = reasonPhrase;
        responseBuilder.entity = entity;
        responseBuilder.metadata = new CaseInsensitiveMap<>();
        responseBuilder.metadata.putAll(metadata);
        return responseBuilder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Ret extends T> RestResponse.ResponseBuilder<Ret> status(int status, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        return (ResponseBuilder<Ret>) this;
    }

    @Override
    public <Ret extends T> RestResponse.ResponseBuilder<Ret> status(int status) {
        return status(status, defaultReasonPhrases.get(status));
    }

    @Override
    public RestResponse.ResponseBuilder<T> entity(T entity) {
        this.entity = entity;
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> entity(T entity, Annotation[] annotations) {
        this.entity = entity;
        this.entityAnnotations = annotations;
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> type(MediaType type) {
        if (type == null) {
            metadata.remove(HttpHeaders.CONTENT_TYPE);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> type(String type) {
        if (type == null) {
            metadata.remove(HttpHeaders.CONTENT_TYPE);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> variant(Variant variant) {
        if (variant == null) {
            type((String) null);
            language((String) null);
            metadata.remove(HttpHeaders.CONTENT_ENCODING);
            return this;
        }
        type(variant.getMediaType());
        language(variant.getLanguage());
        if (variant.getEncoding() != null)
            metadata.putSingle(HttpHeaders.CONTENT_ENCODING, variant.getEncoding());
        else
            metadata.remove(HttpHeaders.CONTENT_ENCODING);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> variants(List<Variant> variants) {
        if (variants == null) {
            metadata.remove(HttpHeaders.VARY);
            return this;
        }
        String vary = AbstractRestResponseBuilder.createVaryHeader(variants);
        metadata.putSingle(HttpHeaders.VARY, vary);

        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> language(String language) {
        if (language == null) {
            metadata.remove(HttpHeaders.CONTENT_LANGUAGE);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> tag(EntityTag tag) {
        if (tag == null) {
            metadata.remove(HttpHeaders.ETAG);
            return this;
        }
        metadata.putSingle(HttpHeaders.ETAG, tag);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> tag(String tag) {
        if (tag == null) {
            metadata.remove(HttpHeaders.ETAG);
            return this;
        }
        return tag(new EntityTag(tag));
    }

    @Override
    public RestResponse.ResponseBuilder<T> lastModified(Date lastModified) {
        if (lastModified == null) {
            metadata.remove(HttpHeaders.LAST_MODIFIED);
            return this;
        }
        metadata.putSingle(HttpHeaders.LAST_MODIFIED, lastModified);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> cacheControl(CacheControl cacheControl) {
        if (cacheControl == null) {
            metadata.remove(HttpHeaders.CACHE_CONTROL);
            return this;
        }
        metadata.putSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> header(String name, Object value) {
        if (value == null) {
            metadata.remove(name);
            return this;
        }
        metadata.add(name, value);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> cookie(NewCookie... cookies) {
        if (cookies == null) {
            metadata.remove(HttpHeaders.SET_COOKIE);
            return this;
        }
        for (NewCookie cookie : cookies) {
            metadata.add(HttpHeaders.SET_COOKIE, cookie);
        }
        return this;
    }

    public RestResponse.ResponseBuilder<T> language(Locale language) {
        if (language == null) {
            metadata.remove(HttpHeaders.CONTENT_LANGUAGE);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    public RestResponse.ResponseBuilder<T> expires(Date expires) {
        if (expires == null) {
            metadata.remove(HttpHeaders.EXPIRES);
            return this;
        }
        metadata.putSingle(HttpHeaders.EXPIRES, AbstractRestResponseBuilder.getDateFormatRFC822().format(expires));
        return this;
    }

    public RestResponse.ResponseBuilder<T> allow(String... methods) {
        if (methods == null) {
            return allow((Set<String>) null);
        }
        HashSet<String> set = new HashSet<>(Arrays.asList(methods));
        return allow(set);
    }

    public RestResponse.ResponseBuilder<T> allow(Set<String> methods) {
        HeaderUtil.setAllow(this.metadata, methods);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> encoding(String encoding) {
        if (encoding == null) {
            metadata.remove(HttpHeaders.CONTENT_ENCODING);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_ENCODING, encoding);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> variants(Variant... variants) {
        return this.variants(Arrays.asList(variants));
    }

    @Override
    public RestResponse.ResponseBuilder<T> links(Link... links) {
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
    public RestResponse.ResponseBuilder<T> link(URI uri, String rel) {
        Link link = Link.fromUri(uri).rel(rel).build();
        metadata.add(HttpHeaders.LINK, link);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> link(String uri, String rel) {
        Link link = Link.fromUri(uri).rel(rel).build();
        metadata.add(HttpHeaders.LINK, link);
        return this;
    }

    @Override
    public RestResponse.ResponseBuilder<T> replaceAll(MultivaluedMap<String, Object> headers) {
        metadata.clear();
        if (headers == null)
            return this;
        metadata.putAll(headers);
        return this;
    }

    public MultivaluedMap<String, Object> getMetadata() {
        return metadata;
    }
}
