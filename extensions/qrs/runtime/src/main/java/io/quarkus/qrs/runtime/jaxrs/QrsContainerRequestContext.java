package io.quarkus.qrs.runtime.jaxrs;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import io.quarkus.qrs.runtime.core.RequestContext;

public class QrsContainerRequestContext implements ContainerRequestContext {

    private RequestContext context;

    public QrsContainerRequestContext(RequestContext requestContext) {
        this.context = requestContext;
    }

    @Override
    public Object getProperty(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getPropertyNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setProperty(String name, Object object) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeProperty(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public UriInfo getUriInfo() {
        return context.getUriInfo();
    }

    @Override
    public void setRequestUri(URI requestUri) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) {
        // TODO Auto-generated method stub

    }

    @Override
    public Request getRequest() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMethod() {
        return context.getContext().request().rawMethod();
    }

    @Override
    public void setMethod(String method) {
        // TODO Auto-generated method stub

    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return context.getHttpHeaders().getRequestHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return context.getHttpHeaders().getHeaderString(name);
    }

    @Override
    public Date getDate() {
        return context.getHttpHeaders().getDate();
    }

    @Override
    public Locale getLanguage() {
        return context.getHttpHeaders().getLanguage();
    }

    @Override
    public int getLength() {
        return context.getHttpHeaders().getLength();
    }

    @Override
    public MediaType getMediaType() {
        return context.getHttpHeaders().getMediaType();
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return context.getHttpHeaders().getAcceptableMediaTypes();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return context.getHttpHeaders().getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return context.getHttpHeaders().getCookies();
    }

    @Override
    public boolean hasEntity() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public InputStream getEntityStream() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setEntityStream(InputStream input) {
        // TODO Auto-generated method stub

    }

    @Override
    public SecurityContext getSecurityContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        // TODO Auto-generated method stub

    }

    @Override
    public void abortWith(Response response) {
        // TODO Auto-generated method stub

    }

}
