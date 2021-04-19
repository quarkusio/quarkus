package org.jboss.resteasy.reactive.client.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.common.NotImplementedYet;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

public class ClientRequestContextImpl implements ResteasyReactiveClientRequestContext {

    private final Client client;
    private final ConfigurationImpl configuration;
    private final RestClientRequestContext restClientRequestContext;
    private final ClientRequestHeadersMap headersMap;
    private OutputStream entityStream;

    public ClientRequestContextImpl(RestClientRequestContext restClientRequestContext, ClientImpl client,
            ConfigurationImpl configuration) {
        this.restClientRequestContext = restClientRequestContext;
        this.client = client;
        this.configuration = configuration;
        this.headersMap = new ClientRequestHeadersMap(); //restClientRequestContext.requestHeaders.getHeaders()
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
        return headersMap;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        CaseInsensitiveMap<String> map = new CaseInsensitiveMap<String>();
        for (Map.Entry<String, List<Object>> entry : headersMap.entrySet()) {
            for (Object obj : entry.getValue()) {
                map.add(entry.getKey(), HeaderUtil.headerToString(obj));
            }
        }
        return map;
    }

    @Override
    public String getHeaderString(String name) {
        return HeaderUtil.getHeaderString(headersMap, name);
    }

    @Override
    public Date getDate() {
        return HeaderUtil.getDate(headersMap);
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
        List<MediaType> acceptableMediaTypes = HeaderUtil.getAcceptableMediaTypes(headersMap);
        if (acceptableMediaTypes.isEmpty()) {
            return singletonList(MediaType.WILDCARD_TYPE);
        }
        return acceptableMediaTypes;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return HeaderUtil.getAcceptableLanguages(headersMap);
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return HeaderUtil.getCookies(headersMap);
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

    public RestClientRequestContext getRestClientRequestContext() {
        return restClientRequestContext;
    }

    public boolean isAborted() {
        return restClientRequestContext.isAborted();
    }

    public Response getAbortedWith() {
        return restClientRequestContext.getAbortedWith();
    }

    @Override
    public void suspend() {
        restClientRequestContext.suspend();
    }

    @Override
    public void resume() {
        restClientRequestContext.resume();
    }

    @Override
    public void resume(Throwable t) {
        restClientRequestContext.resume(t);
    }

    private class ClientRequestHeadersMap implements MultivaluedMap<String, Object> {

        public static final String CONTENT_TYPE = "Content-Type";

        @Override
        public void putSingle(String key, Object value) {
            restClientRequestContext.requestHeaders.getHeaders().putSingle(key, value);
        }

        @Override
        public void add(String key, Object value) {
            restClientRequestContext.requestHeaders.getHeaders().add(key, value);
        }

        @Override
        public Object getFirst(String key) {
            return restClientRequestContext.requestHeaders.getHeaders().getFirst(key);
        }

        @Override
        public void addAll(String key, Object... newValues) {
            restClientRequestContext.requestHeaders.getHeaders().addAll(key, newValues);
        }

        @Override
        public void addAll(String key, List<Object> valueList) {
            restClientRequestContext.requestHeaders.getHeaders().addAll(key, valueList);
        }

        @Override
        public void addFirst(String key, Object value) {
            restClientRequestContext.requestHeaders.getHeaders().addFirst(key, value);
        }

        @Override
        public boolean equalsIgnoreValueOrder(MultivaluedMap<String, Object> otherMap) {
            if (this == otherMap) {
                return true;
            }
            CaseInsensitiveMap<Object> headers = restClientRequestContext.requestHeaders.getHeaders();

            boolean contentTypeMatched = false;
            int checkedKeyCount = 0;
            for (Entry<String, List<Object>> otherEntry : otherMap.entrySet()) {
                checkedKeyCount++;
                if (otherEntry.getKey().equalsIgnoreCase(CONTENT_TYPE)) {
                    contentTypeMatched = true;
                    List<Object> contentTypes = headers.get(CONTENT_TYPE);

                    if (contentTypes == null) {
                        String mediaType = mediaType();
                        if (mediaType != null) {
                            contentTypes = singletonList(mediaType);
                        } else {
                            contentTypes = emptyList();
                        }
                    }
                    for (Object value : otherEntry.getValue()) {
                        if (!contentTypes.contains(value)) {
                            return false;
                        }
                    }
                } else {
                    List<Object> otherValues = otherEntry.getValue();
                    List<Object> values = headers.get(otherEntry.getKey());
                    if (otherValues.size() != values.size()) {
                        return false;
                    }
                    for (Object value : otherValues) {
                        if (!values.contains(value)) {
                            return false;
                        }
                    }
                }
            }

            if (!contentTypeMatched && (headers.containsKey(CONTENT_TYPE) || mediaType() != null)) {
                return false;
            }

            return checkedKeyCount == headers.keySet().size();
        }

        @Override
        public int size() {
            CaseInsensitiveMap<Object> headers = restClientRequestContext.requestHeaders.getHeaders();
            return headers.containsKey(CONTENT_TYPE) ? headers.size() : headers.size() + 1;
        }

        @Override
        public boolean isEmpty() {
            return restClientRequestContext.requestHeaders.getHeaders().isEmpty() &&
                    (restClientRequestContext.entity == null || restClientRequestContext.entity.getMediaType() == null);
        }

        @Override
        public boolean containsKey(Object key) {
            return restClientRequestContext.requestHeaders.getHeaders().containsKey(key) ||
                    (isContentType(key) && restClientRequestContext.entity.getMediaType() != null);
        }

        @Override
        public boolean containsValue(Object value) {
            return restClientRequestContext.requestHeaders.getHeaders().containsValue(value)
                    || (mediaType() != null && mediaType().equals(value));
        }

        @Override
        public List<Object> get(Object key) {
            List<Object> result = restClientRequestContext.requestHeaders.getHeaders().get(key);
            String mediaType = mediaType();
            if (result == null && isContentType(key) && mediaType != null) {
                result = new ArrayList<>(singletonList(mediaType));
            }
            return result;
        }

        @Override
        public List<Object> put(String key, List<Object> value) {
            return restClientRequestContext.requestHeaders.getHeaders().put(key, value);
        }

        @Override
        public List<Object> remove(Object key) {
            return restClientRequestContext.requestHeaders.getHeaders().remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ? extends List<Object>> m) {
            restClientRequestContext.requestHeaders.getHeaders().putAll(m);
        }

        @Override
        public void clear() {
            restClientRequestContext.requestHeaders.getHeaders().clear();
        }

        @Override
        public Set<String> keySet() {
            Set<String> keys = restClientRequestContext.requestHeaders.getHeaders().keySet();
            if (keys.contains(HttpHeaders.CONTENT_TYPE) || mediaType() == null) {
                return keys;
            } else {
                Set<String> keysWithContentType = new TreeSet<>();
                // TODO this is a copy of the set, it should be a set "connected" to the underlying map
                keysWithContentType.add(HttpHeaders.CONTENT_TYPE);
                keysWithContentType.addAll(keys);
                return keysWithContentType;
            }
        }

        @Override
        public Collection<List<Object>> values() {
            CaseInsensitiveMap<Object> headers = restClientRequestContext.requestHeaders.getHeaders();
            Collection<List<Object>> values = headers.values();
            if (headers.containsKey(HttpHeaders.CONTENT_TYPE) || mediaType() == null) {
                return values;
            } else {
                ArrayList<List<Object>> result = new ArrayList<>(values);
                result.add(singletonList(mediaType()));
                return result;
            }
        }

        @Override
        public Set<Entry<String, List<Object>>> entrySet() {
            CaseInsensitiveMap<Object> headers = restClientRequestContext.requestHeaders.getHeaders();
            Set<Entry<String, List<Object>>> entries = headers.entrySet();
            if (headers.containsKey(HttpHeaders.CONTENT_TYPE) || mediaType() == null) {
                return entries;
            } else {
                return new AbstractSet<Entry<String, List<Object>>>() {
                    @Override
                    public Iterator<Entry<String, List<Object>>> iterator() {
                        final AtomicBoolean contentTypeReturned = new AtomicBoolean(false);
                        Iterator<Entry<String, List<Object>>> iterator = entries.iterator();
                        return new Iterator<Entry<String, List<Object>>>() {
                            @Override
                            public boolean hasNext() {
                                return iterator.hasNext() || !contentTypeReturned.get();
                            }

                            @Override
                            public Entry<String, List<Object>> next() {
                                if (iterator.hasNext()) {
                                    return iterator.next();
                                } else if (!contentTypeReturned.get()) {
                                    contentTypeReturned.set(true);
                                    return new AbstractMap.SimpleEntry<>(HttpHeaders.CONTENT_TYPE, singletonList(mediaType()));
                                } else {
                                    throw new NoSuchElementException();
                                }
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return entries.size() + 1;
                    }
                };
            }
        }

        private boolean isContentType(Object key) {
            return key instanceof String && ((String) key).equalsIgnoreCase(HttpHeaders.CONTENT_TYPE);
        }

        private String mediaType() {
            Entity<?> entity = restClientRequestContext.entity;
            return entity == null
                    ? null
                    : entity.getMediaType() == null
                            ? null
                            : entity.getMediaType().toString();
        }
    }
}
