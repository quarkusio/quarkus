package org.jboss.resteasy.reactive.client.impl;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractClientRequestHeaders {
    protected CaseInsensitiveMap<Object> headers = new CaseInsensitiveMap<Object>();

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

    protected abstract StringBuilder buildAcceptString(String accept, Object[] items);

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

    public abstract void allow(String... methods);

    public abstract void allow(Set<String> methods);

    public void cacheControl(CacheControl cacheControl) {
        headers.putSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
    }

    public abstract void header(String name, Object value);

    public Date getDate() {
        return HeaderUtil.getDate(headers);
    }

    public abstract String getHeader(String name);

    public abstract MultivaluedMap<String, String> asMap();

    public abstract Locale getLanguage();

    public abstract int getLength();

    public abstract MediaType getMediaType();

    public abstract List<MediaType> getAcceptableMediaTypes();

    public abstract List<Locale> getAcceptableLanguages();

    public abstract Map<String, Cookie> getCookies();
}
