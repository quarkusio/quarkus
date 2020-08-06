package io.quarkus.qrs.runtime.jaxrs;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.StatusType;

import io.quarkus.qrs.runtime.core.QrsRequestContext;

public class QrsContainerResponseContext implements ContainerResponseContext {

    private QrsRequestContext context;

    public QrsContainerResponseContext(QrsRequestContext requestContext) {
        this.context = requestContext;
    }

    @Override
    public int getStatus() {
        return context.getResponse().getStatus();
    }

    @Override
    public void setStatus(int code) {
        context.getContext().response().setStatusCode(code);
    }

    @Override
    public StatusType getStatusInfo() {
        return context.getResponse().getStatusInfo();
    }

    @Override
    public void setStatusInfo(StatusType statusInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return context.getResponse().getHeaders();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return context.getResponse().getStringHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return context.getResponse().getHeaderString(name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return context.getResponse().getAllowedMethods();
    }

    @Override
    public Date getDate() {
        return context.getResponse().getDate();
    }

    @Override
    public Locale getLanguage() {
        return context.getResponse().getLanguage();
    }

    @Override
    public int getLength() {
        return context.getResponse().getLength();
    }

    @Override
    public MediaType getMediaType() {
        return context.getResponse().getMediaType();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return context.getResponse().getCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return context.getResponse().getEntityTag();
    }

    @Override
    public Date getLastModified() {
        return context.getResponse().getLastModified();
    }

    @Override
    public URI getLocation() {
        return context.getResponse().getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return context.getResponse().getLinks();
    }

    @Override
    public boolean hasLink(String relation) {
        return context.getResponse().hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return context.getResponse().getLink(relation);
    }

    @Override
    public Builder getLinkBuilder(String relation) {
        return context.getResponse().getLinkBuilder(relation);
    }

    @Override
    public boolean hasEntity() {
        return context.getResponse().hasEntity();
    }

    @Override
    public Object getEntity() {
        return context.getResponse().getEntity();
    }

    @Override
    public Class<?> getEntityClass() {
        // FIXME: perhaps this is the method return type?
        return getEntity().getClass();
    }

    @Override
    public Type getEntityType() {
        // FIXME: perhaps this is the method return type?
        return getEntity().getClass();
    }

    @Override
    public void setEntity(Object entity) {
        context.resetBuildTimeSerialization();
        context.setResult(entity);
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        // TODO Auto-generated method stub

    }

    @Override
    public Annotation[] getEntityAnnotations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OutputStream getEntityStream() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {
        // TODO Auto-generated method stub

    }

}
