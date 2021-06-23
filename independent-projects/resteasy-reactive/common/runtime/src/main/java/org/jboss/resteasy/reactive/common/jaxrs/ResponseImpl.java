package org.jboss.resteasy.reactive.common.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.headers.LinkHeaders;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

/**
 * This is the Response class for user-created responses. The client response
 * object has more deserialising powers in @{link {@link io.quarkus.rest.server.runtime.client.QuarkusRestClientResponse}.
 */
@SuppressWarnings("JavadocReference")
public class ResponseImpl extends Response {

    int status;
    String reasonPhrase;
    protected Object entity;
    MultivaluedTreeMap<String, Object> headers;
    InputStream entityStream;
    private StatusTypeImpl statusType;
    private MultivaluedMap<String, String> stringHeaders;
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
            statusType = new StatusTypeImpl(status, reasonPhrase);
        }
        return statusType;
    }

    /**
     * Internal: this is just cheaper than duplicating the response just to change the status
     */
    public void setStatusInfo(StatusType statusType) {
        this.statusType = StatusTypeImpl.valueOf(statusType);
        status = statusType.getStatusCode();
    }

    @Override
    public Object getEntity() {
        // The spec says that getEntity() can be called after readEntity() to obtain the same entity,
        // but it also sort-of implies that readEntity() calls Response.close(), and the TCK does check
        // that we throw if closed and non-buffered
        checkClosed();
        // this check seems very ugly, but it seems to be needed by the TCK
        // this will likely require a better solution
        if (entity instanceof GenericEntity) {
            GenericEntity<?> genericEntity = (GenericEntity<?>) entity;
            if (genericEntity.getRawType().equals(genericEntity.getType())) {
                return ((GenericEntity<?>) entity).getEntity();
            }
        }
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
        if (entity != null && entityType.isInstance(entity)) {
            // Note that this works if entityType is InputStream where we return it without closing it, as per spec
            return (T) entity;
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
        // we have an entity already read, or still to be read
        return entity != null || entityStream != null;
    }

    @Override
    public boolean bufferEntity() {
        checkClosed();
        // must be idempotent
        if (buffered) {
            return true;
        }
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
        return HeaderUtil.getMediaType(headers);
    }

    @Override
    public Locale getLanguage() {
        return HeaderUtil.getLanguage(headers);
    }

    @Override
    public int getLength() {
        return HeaderUtil.getLength(headers);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return HeaderUtil.getAllowedMethods(headers);
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return HeaderUtil.getNewCookies(headers);
    }

    @Override
    public EntityTag getEntityTag() {
        return HeaderUtil.getEntityTag(headers);
    }

    @Override
    public Date getDate() {
        return HeaderUtil.getDate(headers);
    }

    @Override
    public Date getLastModified() {
        return HeaderUtil.getLastModified(headers);
    }

    @Override
    public URI getLocation() {
        return HeaderUtil.getLocation(headers);
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
        if (stringHeaders == null) {
            // let's keep this map case-insensitive
            stringHeaders = new CaseInsensitiveMap<>();
            headers.forEach(this::populateStringHeaders);
        }
        return stringHeaders;
    }

    public void populateStringHeaders(String headerName, List<Object> values) {
        List<String> stringValues = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            stringValues.add(HeaderUtil.headerToString(values.get(i)));
        }
        stringHeaders.put(headerName, stringValues);
    }

    @Override
    public String getHeaderString(String name) {
        return HeaderUtil.getHeaderString(getStringHeaders(), name);
    }

    public Annotation[] getEntityAnnotations() {
        return entityAnnotations;
    }

}
