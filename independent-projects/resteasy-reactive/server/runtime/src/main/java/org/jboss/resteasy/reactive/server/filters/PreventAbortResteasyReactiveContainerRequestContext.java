package org.jboss.resteasy.reactive.server.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class is used to prevent calls to 'abortWith' which could lead to unexpected results in generated
 * {@code ContainerRequestFilter} and {@code ContainerResponseFilter}.
 */
public final class PreventAbortResteasyReactiveContainerRequestContext implements ContainerRequestContext {

    private final ContainerRequestContext delegate;

    public PreventAbortResteasyReactiveContainerRequestContext(ContainerRequestContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getProperty(String name) {
        return delegate.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object object) {
        delegate.setProperty(name, object);
    }

    @Override
    public void removeProperty(String name) {
        delegate.removeProperty(name);
    }

    @Override
    public UriInfo getUriInfo() {
        return delegate.getUriInfo();
    }

    @Override
    public void setRequestUri(URI requestUri) {
        delegate.setRequestUri(requestUri);
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) {
        delegate.setRequestUri(baseUri, requestUri);
    }

    @Override
    public Request getRequest() {
        return delegate.getRequest();
    }

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public void setMethod(String method) {
        delegate.setMethod(method);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return delegate.getHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return delegate.getHeaderString(name);
    }

    @Override
    public Date getDate() {
        return delegate.getDate();
    }

    @Override
    public Locale getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public int getLength() {
        return delegate.getLength();
    }

    @Override
    public MediaType getMediaType() {
        return delegate.getMediaType();
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return delegate.getAcceptableMediaTypes();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return delegate.getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public boolean hasEntity() {
        return delegate.hasEntity();
    }

    @Override
    public InputStream getEntityStream() {
        return delegate.getEntityStream();
    }

    @Override
    public void setEntityStream(InputStream input) {
        delegate.setEntityStream(input);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return delegate.getSecurityContext();
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        delegate.setSecurityContext(context);
    }

    @Override
    public void abortWith(Response response) {
        throw new IllegalStateException(
                "Calling 'abortWith' is not permitted when using @ServerRequestFilter or @ServerResponseFilter. If you need to abort processing, consider returning 'Response' or 'Uni<Response>'");
    }
}
