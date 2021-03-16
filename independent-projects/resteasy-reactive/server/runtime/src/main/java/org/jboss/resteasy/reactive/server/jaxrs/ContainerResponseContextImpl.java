package org.jboss.resteasy.reactive.server.jaxrs;

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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class ContainerResponseContextImpl implements ContainerResponseContext {

    private final ResteasyReactiveRequestContext context;

    public ContainerResponseContextImpl(ResteasyReactiveRequestContext requestContext) {
        this.context = requestContext;
    }

    @Override
    public int getStatus() {
        return response().getStatus();
    }

    protected Response response() {
        return context.getResponse().get();
    }

    @Override
    public void setStatus(int code) {
        context.setResult(Response.fromResponse(response()).status(code).build());
    }

    @Override
    public StatusType getStatusInfo() {
        return response().getStatusInfo();
    }

    @Override
    public void setStatusInfo(StatusType statusInfo) {
        context.setResult(Response.fromResponse(response()).status(statusInfo).build());
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return response().getHeaders();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return response().getStringHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return response().getHeaderString(name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return response().getAllowedMethods();
    }

    @Override
    public Date getDate() {
        return response().getDate();
    }

    @Override
    public Locale getLanguage() {
        return response().getLanguage();
    }

    @Override
    public int getLength() {
        return response().getLength();
    }

    @Override
    public MediaType getMediaType() {
        return response().getMediaType();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return response().getCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return response().getEntityTag();
    }

    @Override
    public Date getLastModified() {
        return response().getLastModified();
    }

    @Override
    public URI getLocation() {
        return response().getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return response().getLinks();
    }

    @Override
    public boolean hasLink(String relation) {
        return response().hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return response().getLink(relation);
    }

    @Override
    public Builder getLinkBuilder(String relation) {
        return response().getLinkBuilder(relation);
    }

    @Override
    public boolean hasEntity() {
        return response().hasEntity();
    }

    @Override
    public Object getEntity() {
        return response().getEntity();
    }

    @Override
    public Class<?> getEntityClass() {
        Object entity = getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getClass();
    }

    @Override
    public Type getEntityType() {
        return context.getGenericReturnType();
    }

    @Override
    public void setEntity(Object entity) {
        context.resetBuildTimeSerialization();
        if (entity instanceof GenericEntity) {
            context.setGenericReturnType(((GenericEntity<?>) entity).getType());
            entity = ((GenericEntity<?>) entity).getEntity();
        }
        Response.ResponseBuilder resp = Response.fromResponse(response()).entity(entity);
        context.setResult(resp.build());
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        context.resetBuildTimeSerialization();
        if (entity instanceof GenericEntity) {
            context.setGenericReturnType(((GenericEntity<?>) entity).getType());
            entity = ((GenericEntity<?>) entity).getEntity();
        }
        context.setResponseContentType(mediaType);
        context.setAllAnnotations(annotations);
        Response.ResponseBuilder resp = Response.fromResponse(response()).entity(entity).type(mediaType);
        context.setResult(resp.build());
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return context.getAllAnnotations();
    }

    @Override
    public OutputStream getEntityStream() {
        OutputStream existing = context.getOutputStream();
        if (existing != null) {
            return existing;
        }
        return context.getOrCreateOutputStream();
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {
        context.setOutputStream(outputStream);
    }

}
