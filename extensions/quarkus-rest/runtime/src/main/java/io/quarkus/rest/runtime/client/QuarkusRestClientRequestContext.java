package io.quarkus.rest.runtime.client;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class QuarkusRestClientRequestContext implements ClientRequestContext {

    private final Map<String, Object> properties = new HashMap<>();
    private final Configuration configuration;
    private final Client client;
    private OutputStream entityStream;
    private URI uri;
    private String method;

    public QuarkusRestClientRequestContext(Configuration configuration, Client client) {
        this.configuration = configuration;
        this.client = client;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.keySet();
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
        return uri;
    }

    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
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

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public MediaType getMediaType() {
        return null;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return null;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return null;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public Object getEntity() {
        return null;
    }

    @Override
    public Class<?> getEntityClass() {
        return null;
    }

    @Override
    public Type getEntityType() {
        return null;
    }

    @Override
    public void setEntity(Object entity) {

    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {

    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return new Annotation[0];
    }

    @Override
    public OutputStream getEntityStream() {
        return null;
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {

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

    }
}
