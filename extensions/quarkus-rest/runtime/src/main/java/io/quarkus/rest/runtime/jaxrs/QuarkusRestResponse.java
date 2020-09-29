package io.quarkus.rest.runtime.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import io.quarkus.rest.runtime.client.QuarkusRestClientResponse;
import io.quarkus.rest.runtime.util.DateUtil;
import io.quarkus.rest.runtime.util.LocaleHelper;

/**
 * This is the Response class for user-created responses. The client response
 * object has more deserialising powers in @{link {@link QuarkusRestClientResponse}.
 */
public class QuarkusRestResponse extends Response {

    int status;
    String reasonPhrase;
    Object entity;
    MultivaluedMap<String, Object> headers;
    InputStream entityStream;
    private QuarkusRestStatusType statusType;
    private MultivaluedHashMap<String, String> stringHeaders;
    Annotation[] entityAnnotations;
    protected boolean consumed;
    protected boolean closed;
    protected boolean buffered;

    @Override
    public int getStatus() {
        return status;
    }

    /**
     * Internal: this is just cheaper than duplicating the response just to change the status
     */
    public void setStatus(int status) {
        this.status = status;
        statusType = null;
    }

    @Override
    public StatusType getStatusInfo() {
        if (statusType == null) {
            statusType = new QuarkusRestStatusType(status, reasonPhrase);
        }
        return statusType;
    }

    /**
     * Internal: this is just cheaper than duplicating the response just to change the status
     */
    public void setStatusInfo(StatusType statusType) {
        this.statusType = QuarkusRestStatusType.valueOf(statusType);
        status = statusType.getStatusCode();
    }

    @Override
    public Object getEntity() {
        // The spec says that getEntity() can be called after readEntity() to obtain the same entity,
        // but it also sort-of implies that readEntity() calls Reponse.close(), and the TCK does check
        // that we throw if closed and non-buffered
        checkClosed();
        return entity;
    }

    protected void setEntity(Object entity) {
        this.entity = entity;
        if (entity instanceof InputStream) {
            this.entityStream = (InputStream) entity;
        }
    }

    public InputStream getEntityStream() {
        return entityStream;
    }

    public void setEntityStream(InputStream entityStream) {
        this.entityStream = entityStream;
    }

    protected <T> T readEntity(Class<T> entityType, Type genericType, Annotation[] annotations) {
        // TODO: we probably need better state handling
        if (hasEntity() && entityType.isInstance(getEntity())) {
            // Note that this works if entityType is InputStream where we return it without closing it, as per spec
            return (T) getEntity();
        }
        // FIXME: does the spec really tell us to do this? sounds like a workaround for not having a string reader
        if (hasEntity() && entityType.equals(String.class)) {
            return (T) getEntity().toString();
        }
        checkClosed();
        // Spec says to throw this
        throw new ProcessingException(
                "Request could not be mapped to type " + (genericType != null ? genericType : entityType));
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return readEntity(entityType, entityType, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        return (T) readEntity(entityType.getRawType(), entityType.getType(), null);
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return readEntity(entityType, entityType, annotations);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return (T) readEntity(entityType.getRawType(), entityType.getType(), annotations);
    }

    @Override
    public boolean hasEntity() {
        // The TCK checks that
        checkClosed();
        return entity != null;
    }

    @Override
    public boolean bufferEntity() {
        checkClosed();
        if (entityStream != null && !consumed) {
            // let's not try this again, even if it fails
            consumed = true;
            // we're supposed to read the entire stream, but if we can rewind it there's no point so let's keep it
            if (!entityStream.markSupported()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                try {
                    while ((read = entityStream.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    entityStream.close();
                } catch (IOException x) {
                    throw new UncheckedIOException(x);
                }
                entityStream = new ByteArrayInputStream(os.toByteArray());
            }
            buffered = true;
            return true;
        }
        return false;
    }

    protected void checkClosed() {
        // apparently the TCK says that buffered responses don't care about being closed
        if (closed && !buffered)
            throw new IllegalStateException("Response has been closed");
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (entityStream != null) {
                try {
                    entityStream.close();
                } catch (IOException e) {
                    throw new ProcessingException(e);
                }
            }
        }
    }

    @Override
    public MediaType getMediaType() {
        Object first = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (first instanceof String) {
            String contentType = (String) first;
            return MediaType.valueOf(contentType);
        } else {
            return (MediaType) first;
        }
    }

    @Override
    public Locale getLanguage() {
        Object obj = headers.getFirst(HttpHeaders.CONTENT_LANGUAGE);
        if (obj == null) {
            return null;
        }
        if (obj instanceof Locale) {
            return (Locale) obj;
        }
        return LocaleHelper.extractLocale(headerToString(obj));
    }

    @Override
    public int getLength() {
        Object obj = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (obj == null) {
            return -1;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        return Integer.parseInt(headerToString(obj));
    }

    @Override
    public Set<String> getAllowedMethods() {
        List<Object> allowed = headers.get(HttpHeaders.ALLOW);
        if ((allowed == null) || allowed.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> allowedMethods = new HashSet<>();
        for (Object header : allowed) {
            if (header instanceof String) {
                String[] list = ((String) header).split(",");
                for (String str : list) {
                    String trimmed = str.trim();
                    if (!trimmed.isEmpty()) {
                        allowedMethods.add(trimmed.toUpperCase());
                    }
                }
            } else {
                allowedMethods.add(headerToString(header).toUpperCase());
            }
        }
        return allowedMethods;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        List<?> list = headers.get(HttpHeaders.SET_COOKIE);
        if ((list == null) || list.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, NewCookie> cookies = new HashMap<>();
        for (Object obj : list) {
            if (obj instanceof NewCookie) {
                NewCookie cookie = (NewCookie) obj;
                cookies.put(cookie.getName(), cookie);
            } else {
                String str = headerToString(obj);
                NewCookie cookie = NewCookie.valueOf(str);
                cookies.put(cookie.getName(), cookie);
            }
        }
        return cookies;
    }

    @Override
    public EntityTag getEntityTag() {
        Object d = headers.getFirst(HttpHeaders.ETAG);
        if (d == null) {
            return null;
        }
        if (d instanceof EntityTag) {
            return (EntityTag) d;
        }
        return EntityTag.valueOf(headerToString(d));
    }

    @Override
    public Date getDate() {
        return firstHeaderToDate(HttpHeaders.DATE);
    }

    @Override
    public Date getLastModified() {
        return firstHeaderToDate(HttpHeaders.LAST_MODIFIED);
    }

    private Date firstHeaderToDate(String date) {
        Object d = headers.getFirst(date);
        if (d == null)
            return null;
        if (d instanceof Date)
            return (Date) d;
        return DateUtil.parseDate(d.toString());
    }

    @Override
    public URI getLocation() {
        Object uri = headers.getFirst(HttpHeaders.LOCATION);
        if (uri == null) {
            return null;
        }
        if (uri instanceof URI) {
            return (URI) uri;
        }
        String str = null;
        if (uri instanceof String) {
            str = (String) uri;
        } else {
            str = headerToString(uri);
        }
        return URI.create(str);
    }

    private LinkHeaders getLinkHeaders() {
        return new LinkHeaders(headers);
    }

    @Override
    public Set<Link> getLinks() {
        return new HashSet<>(getLinkHeaders().getLinks());
    }

    @Override
    public boolean hasLink(String relation) {
        return getLinkHeaders().getLinkByRelationship(relation) != null;
    }

    @Override
    public Link getLink(String relation) {
        return getLinkHeaders().getLinkByRelationship(relation);
    }

    @Override
    public Builder getLinkBuilder(String relation) {
        Link link = getLinkHeaders().getLinkByRelationship(relation);
        if (link == null) {
            return null;
        }
        return Link.fromLink(link);
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return headers;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        // FIXME: is this mutable?
        if (stringHeaders == null) {
            stringHeaders = new MultivaluedHashMap<>();
            for (Entry<String, List<Object>> entry : headers.entrySet()) {
                List<String> stringValues = new ArrayList<>(entry.getValue().size());
                for (Object value : entry.getValue()) {
                    stringValues.add(headerToString(value));
                }
                stringHeaders.put(entry.getKey(), stringValues);
            }
        }

        return stringHeaders;
    }

    @SuppressWarnings("unchecked")
    private static String headerToString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else {
            // TODO: we probably want a more direct way to get the delegate instead of going through all the indirection
            return RuntimeDelegate.getInstance().createHeaderDelegate((Class<Object>) obj.getClass()).toString(obj);
        }
    }

    @Override
    public String getHeaderString(String name) {
        if (!getStringHeaders().containsKey(name)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : getStringHeaders().get(name)) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public Annotation[] getEntityAnnotations() {
        return entityAnnotations;
    }

    private static class LinkHeaders {
        private final Map<String, Link> linksByRelationship = new HashMap<>();
        private final List<Link> links = new ArrayList<>();

        private LinkHeaders(MultivaluedMap<String, Object> headers) {
            List<Object> values = headers.get("Link");
            if (values == null) {
                return;
            }

            for (Object val : values) {
                if (val instanceof Link) {
                    addLink((Link) val);
                } else if (val instanceof String) {
                    for (String link : ((String) val).split(",")) {
                        addLink(Link.valueOf(link));
                    }
                } else {
                    String str = QuarkusRestResponse.headerToString(val);
                    addLink(Link.valueOf(str));
                }
            }
        }

        private void addLink(final Link link) {
            links.add(link);
            for (String rel : link.getRels()) {
                linksByRelationship.put(rel, link);
            }
        }

        public Link getLinkByRelationship(String rel) {
            return linksByRelationship.get(rel);
        }

        public List<Link> getLinks() {
            return links;
        }

    }
}
