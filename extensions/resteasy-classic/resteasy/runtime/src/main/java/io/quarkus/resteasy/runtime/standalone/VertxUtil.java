package io.quarkus.resteasy.runtime.standalone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.util.CookieParser;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.resteasy.util.MediaTypeHelper;

import io.vertx.core.http.HttpServerRequest;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VertxUtil {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    public static ResteasyUriInfo extractUriInfo(HttpServerRequest req, String contextPath) {
        String uri = req.absoluteURI();
        String protocol = req.scheme();

        String uriString;

        // If we appear to have an absolute URL, don't try to recreate it from the host and request line.
        if (uri.startsWith(protocol + "://")) {
            uriString = uri;
        } else {
            String host = req.host();
            if (host == null) {
                host = "unknown";
            }
            uriString = protocol + "://" + host + uri;
        }

        // ResteasyUriInfo expects a context path to start with a "/" character
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        return new ResteasyUriInfo(uriString, contextPath);
    }

    public static ResteasyHttpHeaders extractHttpHeaders(HttpServerRequest request) {

        MultivaluedMap<String, String> requestHeaders = extractRequestHeaders(request);
        ResteasyHttpHeaders headers = new ResteasyHttpHeaders(requestHeaders);

        Map<String, Cookie> cookies = extractCookies(requestHeaders);
        headers.setCookies(cookies);
        return headers;

    }

    static Map<String, Cookie> extractCookies(MultivaluedMap<String, String> headers) {
        Map<String, Cookie> cookies = new HashMap<String, Cookie>();
        List<String> cookieHeaders = headers.get("Cookie");
        if (cookieHeaders == null)
            return cookies;

        for (String cookieHeader : cookieHeaders) {
            for (Cookie cookie : CookieParser.parseCookies(cookieHeader)) {
                cookies.put(cookie.getName(), cookie);
            }
        }
        return cookies;
    }

    public static List<MediaType> extractAccepts(MultivaluedMap<String, String> requestHeaders) {
        List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        List<String> accepts = requestHeaders.get(HttpHeaderNames.ACCEPT);
        if (accepts == null)
            return acceptableMediaTypes;

        for (String accept : accepts) {
            acceptableMediaTypes.addAll(MediaTypeHelper.parseHeader(accept));
        }
        return acceptableMediaTypes;
    }

    public static List<String> extractLanguages(MultivaluedMap<String, String> requestHeaders) {
        List<String> acceptable = new ArrayList<String>();
        List<String> accepts = requestHeaders.get(HttpHeaderNames.ACCEPT_LANGUAGE);
        if (accepts == null)
            return acceptable;

        for (String accept : accepts) {
            String[] splits = COMMA_PATTERN.split(accept);
            for (String split : splits)
                acceptable.add(split.trim());
        }
        return acceptable;
    }

    public static MultivaluedMap<String, String> extractRequestHeaders(HttpServerRequest request) {
        Headers<String> requestHeaders = new Headers<String>();

        for (Map.Entry<String, String> header : request.headers()) {
            requestHeaders.add(header.getKey(), header.getValue());
        }
        return requestHeaders;
    }
}
