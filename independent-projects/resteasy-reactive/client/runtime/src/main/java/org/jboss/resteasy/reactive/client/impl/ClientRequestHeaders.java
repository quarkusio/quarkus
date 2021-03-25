package org.jboss.resteasy.reactive.client.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
public class ClientRequestHeaders extends AbstractClientRequestHeaders {

    public ClientRequestHeaders() {
    }

    protected StringBuilder buildAcceptString(String accept, Object[] items) {
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

    public void allow(String... methods) {
        HeaderUtil.setAllow(this.headers, methods);
    }

    public void allow(Set<String> methods) {
        HeaderUtil.setAllow(headers, methods);
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
