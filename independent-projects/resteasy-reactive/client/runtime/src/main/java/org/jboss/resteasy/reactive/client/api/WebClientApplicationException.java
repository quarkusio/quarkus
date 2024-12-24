package org.jboss.resteasy.reactive.client.api;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ResteasyReactiveClientProblem;
import org.jboss.resteasy.reactive.common.jaxrs.StatusTypeImpl;

/**
 * Subclass of {@link WebApplicationException} for use by clients.
 */
@SuppressWarnings("serial")
public class WebClientApplicationException extends WebApplicationException implements ResteasyReactiveClientProblem {

    public WebClientApplicationException(int responseStatus) {
        this(responseStatus, (String) null);
    }

    public WebClientApplicationException(int responseStatus, String responseReasonPhrase) {
        super("Server response is: " + responseStatus, null, new DummyResponse(responseStatus, responseReasonPhrase));
    }

    public WebClientApplicationException(int responseStatus, Response response) {
        super("Server response is: " + responseStatus, response);
    }

    public WebClientApplicationException(int responseStatus, Throwable cause, Response response) {
        super("Server response is: " + responseStatus, cause, response);
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
