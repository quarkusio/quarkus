package org.jboss.resteasy.reactive.server.jaxrs;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class ContainerRequestContextImpl implements ResteasyReactiveContainerRequestContext {

    private final ResteasyReactiveRequestContext quarkusRestContext;
    private boolean aborted;
    private boolean preMatch;
    private boolean response;

    public ContainerRequestContextImpl(ResteasyReactiveRequestContext requestContext) {
        this.quarkusRestContext = requestContext;
    }

    @Override
    public Object getProperty(String name) {
        return quarkusRestContext.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return quarkusRestContext.getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object object) {
        quarkusRestContext.setProperty(name, object);
    }

    @Override
    public void removeProperty(String name) {
        quarkusRestContext.removeProperty(name);
    }

    @Override
    public UriInfo getUriInfo() {
        return quarkusRestContext.getUriInfo();
    }

    @Override
    public void setRequestUri(URI requestUri) {
        setRequestUri(getUriInfo().getBaseUri(), requestUri);
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) {
        assertPreMatch();
        quarkusRestContext.setRequestUri(baseUri.resolve(requestUri));
    }

    @Override
    public Request getRequest() {
        return quarkusRestContext.getRequest();
    }

    @Override
    public String getMethod() {
        return quarkusRestContext.getMethod();
    }

    @Override
    public void setMethod(String method) {
        assertPreMatch();
        quarkusRestContext.setMethod(method);
    }

    public void assertPreMatch() {
        if (!isPreMatch()) {
            throw new IllegalStateException("Can only be called from a @PreMatch filter");
        }
    }

    public void assertNotResponse() {
        if (isResponse()) {
            throw new IllegalStateException("Cannot be called from response filter");
        }
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return quarkusRestContext.getHttpHeaders().getMutableHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return quarkusRestContext.getHttpHeaders().getHeaderString(name);
    }

    @Override
    public Date getDate() {
        return quarkusRestContext.getHttpHeaders().getDate();
    }

    @Override
    public Locale getLanguage() {
        return quarkusRestContext.getHttpHeaders().getLanguage();
    }

    @Override
    public int getLength() {
        return quarkusRestContext.getHttpHeaders().getLength();
    }

    @Override
    public MediaType getMediaType() {
        return quarkusRestContext.getHttpHeaders().getMediaType();
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return quarkusRestContext.getHttpHeaders().getAcceptableMediaTypes();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return quarkusRestContext.getHttpHeaders().getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return quarkusRestContext.getHttpHeaders().getCookies();
    }

    @Override
    public boolean hasEntity() {
        // we haven't set the input stream because this runs before the input handlers
        // so we just can't know, but RESTEasy does this, I suspect just to get the TCK to
        // be happy, so that's enough for us too
        return getMediaType() != null;
    }

    @Override
    public InputStream getEntityStream() {
        return quarkusRestContext.getInputStream();
    }

    @Override
    public void setEntityStream(InputStream input) {
        assertNotResponse();
        quarkusRestContext.setInputStream(input);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return quarkusRestContext.getSecurityContext();
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        assertNotResponse();
        this.quarkusRestContext.setSecurityContext(context);
    }

    public boolean isPreMatch() {
        return preMatch;
    }

    public ContainerRequestContextImpl setPreMatch(boolean preMatch) {
        this.preMatch = preMatch;
        return this;
    }

    @Override
    public void abortWith(Response response) {
        assertNotResponse();
        quarkusRestContext.setResult(response);
        quarkusRestContext.setAbortHandlerChainStarted(true);
        quarkusRestContext.restart(quarkusRestContext.getAbortHandlerChain(), true);
        aborted = true;
        // this is a valid action after suspend, in which case we must resume
        if (quarkusRestContext.isSuspended())
            quarkusRestContext.resume();
    }

    public boolean isResponse() {
        return response;
    }

    public ContainerRequestContextImpl setResponse(boolean response) {
        this.response = response;
        return this;
    }

    public boolean isAborted() {
        return aborted;
    }

    @Override
    public ServerRequestContext getServerRequestContext() {
        return quarkusRestContext;
    }

    @Override
    public void suspend() {
        quarkusRestContext.suspend();
    }

    @Override
    public void resume() {
        quarkusRestContext.resume();
    }

    @Override
    public void resume(Throwable t) {
        quarkusRestContext.resume(t);
    }
}
