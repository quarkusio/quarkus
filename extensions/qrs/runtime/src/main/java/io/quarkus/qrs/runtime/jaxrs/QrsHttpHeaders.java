package io.quarkus.qrs.runtime.jaxrs;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.vertx.core.MultiMap;

public class QrsHttpHeaders implements HttpHeaders {

    private MultivaluedMap<String, String> headers;

    public QrsHttpHeaders(MultiMap vertxHeaders) {
        headers = new MultivaluedHashMap<>();
        for (Entry<String, String> entry : vertxHeaders.entries()) {
            headers.add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public List<String> getRequestHeader(String name) {
        return headers.get(name);
    }

    @Override
    public String getHeaderString(String name) {
        return headers.getFirst(name);
    }

    @Override
    public MultivaluedMap<String, String> getRequestHeaders() {
        // FIXME: must be immutable
        return headers;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        // TODO Auto-generated method stub
        return null;
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
    public Map<String, Cookie> getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLength() {
        // TODO Auto-generated method stub
        return 0;
    }

}
