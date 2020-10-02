package io.quarkus.rest.runtime.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import io.quarkus.rest.runtime.headers.HeaderUtil;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.util.CaseInsensitiveMap;
import io.quarkus.rest.runtime.util.DateUtil;
import io.quarkus.rest.runtime.util.MediaTypeHelper;
import io.quarkus.rest.runtime.util.WeightedLanguage;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientRequestHeaders {
    protected CaseInsensitiveMap<Object> headers = new CaseInsensitiveMap<Object>();
    protected QuarkusRestConfiguration configuration;

    public ClientRequestHeaders(final QuarkusRestConfiguration configuration) {
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
            builder.append(configuration.toHeaderString(l));
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
            accept(configuration.toHeaderString(value));
        else if (name.equalsIgnoreCase(HttpHeaders.ACCEPT_ENCODING))
            acceptEncoding(configuration.toHeaderString(value));
        else if (name.equalsIgnoreCase(HttpHeaders.ACCEPT_LANGUAGE))
            acceptLanguage(configuration.toHeaderString(value));
        else
            headers.add(name, value);
    }

    public Date getDate() {
        Object d = headers.getFirst(HttpHeaders.DATE);
        if (d == null)
            return null;
        if (d instanceof Date)
            return (Date) d;
        return DateUtil.parseDate(d.toString());
    }

    public String getHeader(String name) {
        List vals = headers.get(name);
        if (vals == null)
            return null;
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Object val : vals) {
            if (first)
                first = false;
            else
                builder.append(",");
            builder.append(configuration.toHeaderString(val));
        }
        return builder.toString();
    }

    public MultivaluedMap<String, String> asMap() {
        CaseInsensitiveMap<String> map = new CaseInsensitiveMap<String>();
        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
            for (Object obj : entry.getValue()) {
                map.add(entry.getKey(), configuration.toHeaderString(obj));
            }
        }
        return map;
    }

    public Locale getLanguage() {
        Object obj = headers.getFirst(HttpHeaders.CONTENT_LANGUAGE);
        if (obj == null)
            return null;
        if (obj instanceof Locale)
            return (Locale) obj;
        return new Locale(obj.toString());
    }

    public int getLength() {
        return -1;
    }

    public MediaType getMediaType() {
        Object obj = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (obj == null)
            return null;
        if (obj instanceof MediaType)
            return (MediaType) obj;
        return MediaType.valueOf(configuration.toHeaderString(obj));
    }

    public List<MediaType> getAcceptableMediaTypes() {
        List<MediaType> list = new ArrayList<MediaType>();
        List accepts = headers.get(HttpHeaders.ACCEPT);
        if (accepts == null)
            return list;
        for (Object obj : accepts) {
            if (obj instanceof MediaType) {
                list.add((MediaType) obj);
                continue;
            }
            String accept = null;
            if (obj instanceof String) {
                accept = (String) obj;
            } else {
                accept = configuration.toHeaderString(obj);

            }
            StringTokenizer tokenizer = new StringTokenizer(accept, ",");
            while (tokenizer.hasMoreElements()) {
                String item = tokenizer.nextToken().trim();
                list.add(MediaType.valueOf(item));
            }
        }
        MediaTypeHelper.sortByWeight(list);
        return list;
    }

    public List<Locale> getAcceptableLanguages() {
        List<Locale> list = new ArrayList<Locale>();
        List accepts = headers.get(HttpHeaders.ACCEPT_LANGUAGE);
        if (accepts == null)
            return list;
        List<WeightedLanguage> languages = new ArrayList<WeightedLanguage>();
        for (Object obj : accepts) {
            if (obj instanceof Locale) {
                languages.add(new WeightedLanguage((Locale) obj, 1.0F));
                continue;
            }
            String accept = configuration.toHeaderString(obj);
            StringTokenizer tokenizer = new StringTokenizer(accept, ",");
            while (tokenizer.hasMoreElements()) {
                String item = tokenizer.nextToken().trim();
                languages.add(WeightedLanguage.parse(item));
            }
        }
        Collections.sort(languages);
        for (WeightedLanguage language : languages)
            list.add(language.getLocale());
        return list;
    }

    public Map<String, Cookie> getCookies() {
        Map<String, Cookie> cookies = new HashMap<String, Cookie>();
        List list = headers.get(HttpHeaders.COOKIE);
        if (list == null)
            return cookies;
        for (Object obj : list) {
            if (obj instanceof Cookie) {
                Cookie cookie = (Cookie) obj;
                cookies.put(cookie.getName(), cookie);
            } else {
                String str = configuration.toHeaderString(obj);
                Cookie cookie = Cookie.valueOf(str);
                cookies.put(cookie.getName(), cookie);
            }
        }
        return cookies;
    }
}
