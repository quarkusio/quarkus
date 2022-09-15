package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientRequestHeaders {
    protected CaseInsensitiveMap<Object> headers = new CaseInsensitiveMap<Object>();
    protected ConfigurationImpl configuration;

    public ClientRequestHeaders(final ConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public CaseInsensitiveMap<Object> getHeaders() {
        return headers;
    }

    public void setHeaders(MultivaluedMap<String, Object> newHeaders) {
        headers.clear();
        if (newHeaders == null)
            return;
        headers.putAll(newHeaders);
    }

    public void setLanguage(Locale language) {
        //if this already set by HeaderParamProcessor
        if (this.getHeader(HttpHeaders.CONTENT_LANGUAGE) != null) {
            return;
        }
        if (language == null) {
            headers.remove(HttpHeaders.CONTENT_LANGUAGE);
            return;
        }
        headers.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
    }

    public void setLanguage(String language) {
        setLanguage(new Locale(language));
    }

    public void setMediaType(MediaType mediaType) {
        if (mediaType == null) {
            headers.remove(HttpHeaders.CONTENT_TYPE);
            return;
        }
        headers.putSingle(HttpHeaders.CONTENT_TYPE, mediaType);
    }

    public void acceptLanguage(Locale... locales) {
        String accept = (String) headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE);
        StringBuilder builder = buildAcceptString(accept, locales);
        headers.putSingle(HttpHeaders.ACCEPT_LANGUAGE, builder.toString());
    }

    public void acceptLanguage(String... locales) {
        String accept = (String) headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE);
        StringBuilder builder = buildAcceptString(accept, locales);
        headers.putSingle(HttpHeaders.ACCEPT_LANGUAGE, builder.toString());
    }

    private StringBuilder buildAcceptString(String accept, Object[] items) {
        StringBuilder builder = new StringBuilder();
        if (accept != null)
            builder.append(accept).append(", ");

        boolean isFirst = true;
        for (Object l : items) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(", ");
            }
            builder.append(HeaderUtil.headerToString(l));
        }
        return builder;
    }

    public void acceptEncoding(String... encodings) {
        String accept = (String) headers.getFirst(HttpHeaders.ACCEPT_ENCODING);
        StringBuilder builder = buildAcceptString(accept, encodings);
        headers.putSingle(HttpHeaders.ACCEPT_ENCODING, builder.toString());
    }

    public void accept(String... types) {
        String accept = (String) headers.getFirst(HttpHeaders.ACCEPT);
        StringBuilder builder = buildAcceptString(accept, types);
        headers.putSingle(HttpHeaders.ACCEPT, builder.toString());
    }

    public void accept(MediaType... types) {
        String accept = (String) headers.getFirst(HttpHeaders.ACCEPT);
        StringBuilder builder = buildAcceptString(accept, types);
        headers.putSingle(HttpHeaders.ACCEPT, builder.toString());
    }

    public void cookie(Cookie cookie) {
        if (!(Cookie.class.equals(cookie.getClass()))) {
            cookie = new Cookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), cookie.getVersion());
        }
        headers.add(HttpHeaders.COOKIE, cookie);
    }

    public void allow(String... methods) {
        HeaderUtil.setAllow(this.headers, methods);
    }

    public void allow(Set<String> methods) {
        HeaderUtil.setAllow(headers, methods);
    }

    public void cacheControl(CacheControl cacheControl) {
        headers.putSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
    }

    public void header(String name, Object value) {
        if (value == null) {
            headers.remove(name);
            return;
        }
        if (name.equalsIgnoreCase(HttpHeaders.ACCEPT))
            accept(HeaderUtil.headerToString(value));
        else if (name.equalsIgnoreCase(HttpHeaders.ACCEPT_ENCODING))
            acceptEncoding(HeaderUtil.headerToString(value));
        else if (name.equalsIgnoreCase(HttpHeaders.ACCEPT_LANGUAGE))
            acceptLanguage(HeaderUtil.headerToString(value));
        else
            headers.add(name, value);
    }

    public Date getDate() {
        return HeaderUtil.getDate(headers);
    }

    public String getHeader(String name) {
        return HeaderUtil.getHeaderString(headers, name);
    }

    public MultivaluedMap<String, String> asMap() {
        CaseInsensitiveMap<String> map = new CaseInsensitiveMap<String>();
        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
            for (Object obj : entry.getValue()) {
                map.add(entry.getKey(), HeaderUtil.headerToString(obj));
            }
        }
        return map;
    }

    public Locale getLanguage() {
        return HeaderUtil.getLanguage(headers);
    }

    public int getLength() {
        return HeaderUtil.getLength(headers);
    }

    public MediaType getMediaType() {
        return HeaderUtil.getMediaType(headers);
    }

    public List<MediaType> getAcceptableMediaTypes() {
        return HeaderUtil.getAcceptableMediaTypes(headers);
    }

    public List<Locale> getAcceptableLanguages() {
        return HeaderUtil.getAcceptableLanguages(headers);
    }

    public Map<String, Cookie> getCookies() {
        return HeaderUtil.getCookies(headers);
    }
}
