package org.jboss.resteasy.reactive.common.jaxrs;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

public abstract class AbstractResponseBuilder extends Response.ResponseBuilder {

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
    public ResponseImpl build() {
        return populateResponse(new ResponseImpl());
    }

    public ResponseImpl build(boolean copyHeaders) {
        return populateResponse(new ResponseImpl(), copyHeaders);
    }

    /**
     * Populates a response with the standard data
     *
     * @return The given response
     */
    public <T extends ResponseImpl> T populateResponse(T response) {
        return populateResponse(response, true);
    }

    public <T extends ResponseImpl> T populateResponse(T response, boolean copyHeaders) {
        response.entity = entity;
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

    protected abstract AbstractResponseBuilder doClone();

    @Override
    public AbstractResponseBuilder clone() {
        AbstractResponseBuilder responseBuilder = doClone();
        responseBuilder.status = status;
        responseBuilder.reasonPhrase = reasonPhrase;
        responseBuilder.entity = entity;
        responseBuilder.metadata = new CaseInsensitiveMap<>();
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
        return status(status, AbstractRestResponseBuilder.defaultReasonPhrases.get(status));
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
            metadata.remove(HttpHeaders.CONTENT_TYPE);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    @Override
    public Response.ResponseBuilder type(String type) {
        if (type == null) {
            metadata.remove(HttpHeaders.CONTENT_TYPE);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    @Override
    public Response.ResponseBuilder variant(Variant variant) {
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
    public Response.ResponseBuilder variants(List<Variant> variants) {
        if (variants == null) {
            metadata.remove(HttpHeaders.VARY);
            return this;
        }
        String vary = AbstractResponseBuilder.createVaryHeader(variants);
        metadata.putSingle(HttpHeaders.VARY, vary);

        return this;
    }

    @Override
    public Response.ResponseBuilder language(String language) {
        if (language == null) {
            metadata.remove(HttpHeaders.CONTENT_LANGUAGE);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(EntityTag tag) {
        if (tag == null) {
            metadata.remove(HttpHeaders.ETAG);
            return this;
        }
        metadata.putSingle(HttpHeaders.ETAG, tag);
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(String tag) {
        if (tag == null) {
            metadata.remove(HttpHeaders.ETAG);
            return this;
        }
        return tag(new EntityTag(tag));
    }

    @Override
    public Response.ResponseBuilder lastModified(Date lastModified) {
        if (lastModified == null) {
            metadata.remove(HttpHeaders.LAST_MODIFIED);
            return this;
        }
        metadata.putSingle(HttpHeaders.LAST_MODIFIED, lastModified);
        return this;
    }

    @Override
    public Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
        if (cacheControl == null) {
            metadata.remove(HttpHeaders.CACHE_CONTROL);
            return this;
        }
        metadata.putSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
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
            metadata.remove(HttpHeaders.SET_COOKIE);
            return this;
        }
        for (NewCookie cookie : cookies) {
            metadata.add(HttpHeaders.SET_COOKIE, cookie);
        }
        return this;
    }

    public Response.ResponseBuilder language(Locale language) {
        if (language == null) {
            metadata.remove(HttpHeaders.CONTENT_LANGUAGE);
            return this;
        }
        metadata.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    public Response.ResponseBuilder expires(Date expires) {
        if (expires == null) {
            metadata.remove(HttpHeaders.EXPIRES);
            return this;
        }
        metadata.putSingle(HttpHeaders.EXPIRES, AbstractResponseBuilder.getDateFormatRFC822().format(expires));
        return this;
    }

    public Response.ResponseBuilder allow(String... methods) {
        if (methods == null) {
            return allow((Set<String>) null);
        }
        HashSet<String> set = new HashSet<>(Arrays.asList(methods));
        return allow(set);
    }

    public Response.ResponseBuilder allow(Set<String> methods) {
        HeaderUtil.setAllow(this.metadata, methods);
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
}
