package io.quarkus.qrs.runtime.core;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

class QrsResponse extends Response {

    int status;
    String reasonPhrase;
    Object entity;
    MultivaluedHashMap<String, Object> headers;
    private QrsStatusType statusType;
    private MultivaluedHashMap<String, String> stringHeaders;

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public StatusType getStatusInfo() {
        if(statusType == null) {
            statusType = new QrsStatusType(status, reasonPhrase);
        }
        return statusType;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasEntity() {
        return entity != null;
    }

    @Override
    public boolean bufferEntity() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Locale getLanguage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Set<String> getAllowedMethods() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EntityTag getEntityTag() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getLastModified() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URI getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Link> getLinks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasLink(String relation) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Link getLink(String relation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Builder getLinkBuilder(String relation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return headers;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        // FIXME: is this mutable?
        if(stringHeaders == null) {
            stringHeaders = new MultivaluedHashMap<>();
            for (Entry<String, List<Object>> entry : headers.entrySet()) {
                List<String> stringValues = new ArrayList<>(entry.getValue().size());
                for (Object value : entry.getValue()) {
                    // FIXME: serialisation support
                    stringValues.add((String) value);
                }
                stringHeaders.put(entry.getKey(), stringValues);
            }
        }
        
        return stringHeaders;
    }

    @Override
    public String getHeaderString(String name) {
        return getStringHeaders().getFirst(name);
    }

}
