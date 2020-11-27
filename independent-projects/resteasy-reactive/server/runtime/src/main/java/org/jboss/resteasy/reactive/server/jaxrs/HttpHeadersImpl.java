package org.jboss.resteasy.reactive.server.jaxrs;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.reactive.common.headers.HeaderUtil;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.common.util.UnmodifiableMultivaluedMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HttpHeadersImpl implements HttpHeaders {

    private final MultivaluedMap<String, String> requestHeaders;
    private final MultivaluedMap<String, String> unmodifiableRequestHeaders;
    private Map<String, Cookie> cookies;

    public HttpHeadersImpl(Iterable<Map.Entry<String, String>> vertxHeaders) {
        requestHeaders = new CaseInsensitiveMap<>();
        for (Map.Entry<String, String> entry : vertxHeaders) {
            requestHeaders.add(entry.getKey(), entry.getValue());
        }
        this.unmodifiableRequestHeaders = new UnmodifiableMultivaluedMap<>(requestHeaders, false);
    }

    @Override
    public MultivaluedMap<String, String> getRequestHeaders() {
        return unmodifiableRequestHeaders;
    }

    public MultivaluedMap<String, String> getMutableHeaders() {
        return requestHeaders;
    }

    @Override
    public List<String> getRequestHeader(String name) {
        List<String> vals = unmodifiableRequestHeaders.get(name);
        return vals == null ? Collections.<String> emptyList() : vals;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        if (cookies == null) {
            cookies = Collections.unmodifiableMap(HeaderUtil.getCookies(requestHeaders));
        }
        return cookies;
    }

    @Override
    public Date getDate() {
        return HeaderUtil.getDate(requestHeaders);
    }

    @Override
    public String getHeaderString(String name) {
        return HeaderUtil.getHeaderString(requestHeaders, name);
    }

    @Override
    public Locale getLanguage() {
        return HeaderUtil.getLanguage(requestHeaders);
    }

    @Override
    public int getLength() {
        return HeaderUtil.getLength(requestHeaders);
    }

    // because header string map is mutable, we only cache the parsed media type
    // and still do hash lookup
    private String cachedMediaTypeString;
    private MediaType cachedMediaType;

    @Override
    public MediaType getMediaType() {
        String obj = requestHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (obj == null)
            return null;
        if (obj == cachedMediaTypeString)
            return cachedMediaType;
        cachedMediaTypeString = obj;
        cachedMediaType = MediaType.valueOf(obj);
        return cachedMediaType;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        List<MediaType> modifiableAcceptableMediaTypes = getModifiableAcceptableMediaTypes();
        if (modifiableAcceptableMediaTypes.size() > 1) {
            return Collections.unmodifiableList(modifiableAcceptableMediaTypes);
        }
        return modifiableAcceptableMediaTypes;
    }

    public List<MediaType> getModifiableAcceptableMediaTypes() {
        return HeaderUtil.getAcceptableMediaTypes(requestHeaders);
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return HeaderUtil.getAcceptableLanguages(requestHeaders);
    }
}
