package org.jboss.resteasy.reactive.client.api;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.jaxrs.StatusTypeImpl;

/**
 * Subclass of {@link WebApplicationException} for use by clients, which forbids setting a response that
 * would be used by the server.
 * FIXME: I'd rather this be disjoint from WebApplicationException, so we could store the response info
 * for client usage. Perhaps we can store it in an alternate field?
 */
@SuppressWarnings("serial")
public class WebClientApplicationException extends WebApplicationException {

    public WebClientApplicationException(int responseStatus) {
        this(responseStatus, null);
    }

    public WebClientApplicationException(int responseStatus, String responseReasonPhrase) {
        super("Server response is: " + responseStatus, null, new DummyResponse(responseStatus, responseReasonPhrase));
    }

    /**
     * Used in order to avoid depending on the server parts just for generating the exception.
     * The only meaningful information it has is the response code.
     */
    private static class DummyResponse extends Response {

        private final int responseStatus;
        private final StatusType statusType;

        private DummyResponse(int responseStatus, String responseReasonPhrase) {
            this.responseStatus = responseStatus;
            this.statusType = new StatusTypeImpl(responseStatus, responseReasonPhrase);
        }

        @Override
        public int getStatus() {
            return responseStatus;
        }

        @Override
        public StatusType getStatusInfo() {
            return statusType;
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
    }
}
