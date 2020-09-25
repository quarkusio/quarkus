package io.quarkus.rest.runtime.client;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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

import io.quarkus.rest.runtime.NotImplementedYet;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;

public class QuarkusRestClientRequestContext implements ClientRequestContext {

    private final Map<String, Object> properties = new HashMap<>();
    private final Client client;
    private final QuarkusRestConfiguration configuration;
    private OutputStream entityStream;
    private InvocationState invocationState;
    Response abortedWith;

    public QuarkusRestClientRequestContext(InvocationState invocationState, QuarkusRestClient client,
            QuarkusRestConfiguration configuration) {
        this.invocationState = invocationState;
        this.client = client;
        this.configuration = configuration;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        // TCK says the property names need to be immutable
        return Collections.unmodifiableSet(properties.keySet());
    }

    @Override
    public void setProperty(String name, Object object) {
        properties.put(name, object);
    }

    @Override
    public void removeProperty(String name) {
        properties.remove(name);
    }

    @Override
    public URI getUri() {
        return invocationState.uri;
    }

    @Override
    public void setUri(URI uri) {
        invocationState.uri = uri;
    }

    @Override
    public String getMethod() {
        return invocationState.httpMethod;
    }

    @Override
    public void setMethod(String method) {
        invocationState.httpMethod = method;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return invocationState.requestHeaders.getHeaders();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        // FIXME: this should be mutable, but it's a copy ATM
        return invocationState.requestHeaders.asMap();
    }

    @Override
    public String getHeaderString(String name) {
        return invocationState.requestHeaders.getHeader(name);
    }

    @Override
    public Date getDate() {
        return invocationState.requestHeaders.getDate();
    }

    @Override
    public Locale getLanguage() {
        // those come from the entity
        return invocationState.entity != null ? invocationState.entity.getLanguage() : null;
    }

    @Override
    public MediaType getMediaType() {
        // those come from the entity
        return invocationState.entity != null ? invocationState.entity.getMediaType() : null;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        List<MediaType> acceptableMediaTypes = invocationState.requestHeaders.getAcceptableMediaTypes();
        if (acceptableMediaTypes.isEmpty()) {
            return Collections.singletonList(MediaType.WILDCARD_TYPE);
        }
        return acceptableMediaTypes;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return invocationState.requestHeaders.getAcceptableLanguages();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return invocationState.requestHeaders.getCookies();
    }

    @Override
    public boolean hasEntity() {
        return invocationState.entity != null;
    }

    @Override
    public Object getEntity() {
        Entity<?> entity = invocationState.entity;
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
        Entity<?> entity = invocationState.entity;
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
        Entity<?> entity = invocationState.entity;
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
        invocationState.setEntity(entity, annotations, mediaType);
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return invocationState.entity != null ? invocationState.entity.getAnnotations() : Serialisers.NO_ANNOTATION;
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
        this.abortedWith = response;
    }
}
