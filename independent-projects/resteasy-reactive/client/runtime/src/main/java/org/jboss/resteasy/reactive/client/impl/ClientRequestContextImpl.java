package org.jboss.resteasy.reactive.client.impl;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.NotImplementedYet;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

public class ClientRequestContextImpl implements ClientRequestContext {

    private final Client client;
    private final ConfigurationImpl configuration;
    private OutputStream entityStream;
    private RestClientRequestContext restClientRequestContext;

    public ClientRequestContextImpl(RestClientRequestContext restClientRequestContext, ClientImpl client,
            ConfigurationImpl configuration) {
        this.restClientRequestContext = restClientRequestContext;
        this.client = client;
        this.configuration = configuration;
    }

    @Override
    public Object getProperty(String name) {
        return restClientRequestContext.properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        // TCK says the property names need to be immutable
        return Collections.unmodifiableSet(restClientRequestContext.properties.keySet());
    }

    @Override
    public void setProperty(String name, Object object) {
        restClientRequestContext.properties.put(name, object);
    }

    @Override
    public void removeProperty(String name) {
        restClientRequestContext.properties.remove(name);
    }

    @Override
    public URI getUri() {
        return restClientRequestContext.uri;
    }

    @Override
    public void setUri(URI uri) {
        restClientRequestContext.uri = uri;
    }

    @Override
    public String getMethod() {
        return restClientRequestContext.httpMethod;
    }

    @Override
    public void setMethod(String method) {
        restClientRequestContext.httpMethod = method;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return restClientRequestContext.requestHeaders.getHeaders();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        // FIXME: this should be mutable, but it's a copy ATM
        return restClientRequestContext.requestHeaders.asMap();
    }

    @Override
    public String getHeaderString(String name) {
        return restClientRequestContext.requestHeaders.getHeader(name);
    }

    @Override
    public Date getDate() {
        return restClientRequestContext.requestHeaders.getDate();
    }

    @Override
    public Locale getLanguage() {
        // those come from the entity
        return restClientRequestContext.entity != null ? restClientRequestContext.entity.getLanguage() : null;
    }

    @Override
    public MediaType getMediaType() {
        // those come from the entity
        return restClientRequestContext.entity != null ? restClientRequestContext.entity.getMediaType() : null;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        List<MediaType> acceptableMediaTypes = restClientRequestContext.requestHeaders.getAcceptableMediaTypes();
        if (acceptableMediaTypes.isEmpty()) {
            return Collections.singletonList(MediaType.WILDCARD_TYPE);
        }
        return acceptableMediaTypes;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return restClientRequestContext.requestHeaders.getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return restClientRequestContext.requestHeaders.getCookies();
    }

    @Override
    public boolean hasEntity() {
        return restClientRequestContext.entity != null;
    }

    @Override
    public Object getEntity() {
        Entity<?> entity = restClientRequestContext.entity;
        if (entity != null) {
            Object ret = entity.getEntity();
            if (ret instanceof GenericEntity) {
                return ((GenericEntity<?>) ret).getEntity();
            }
            return ret;
        }
        return null;
    }

    @Override
    public Class<?> getEntityClass() {
        Entity<?> entity = restClientRequestContext.entity;
        if (entity != null) {
            Object ret = entity.getEntity();
            if (ret instanceof GenericEntity) {
                return ((GenericEntity<?>) ret).getRawType();
            }
            return ret.getClass();
        }
        return null;
    }

    @Override
    public Type getEntityType() {
        Entity<?> entity = restClientRequestContext.entity;
        if (entity != null) {
            Object ret = entity.getEntity();
            if (ret instanceof GenericEntity) {
                return ((GenericEntity<?>) ret).getType();
            }
            return ret.getClass();
        }
        return null;
    }

    @Override
    public void setEntity(Object entity) {
        setEntity(entity, Serialisers.NO_ANNOTATION, null);
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        restClientRequestContext.setEntity(entity, annotations, mediaType);
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return restClientRequestContext.entity != null ? restClientRequestContext.entity.getAnnotations()
                : Serialisers.NO_ANNOTATION;
    }

    @Override
    public OutputStream getEntityStream() {
        throw new NotImplementedYet();
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {
        throw new NotImplementedYet();
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void abortWith(Response response) {
        restClientRequestContext.setAbortedWith(response);
    }

    public Response getAbortedWith() {
        return restClientRequestContext.getAbortedWith();
    }
}
