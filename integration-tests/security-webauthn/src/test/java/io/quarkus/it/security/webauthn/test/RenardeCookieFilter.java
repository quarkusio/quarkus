package io.quarkus.it.security.webauthn.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.RFC6265StrictSpec;
import org.apache.http.message.BasicHeader;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

public class RenardeCookieFilter implements Filter {
    private final boolean allowMultipleCookiesWithTheSameName;
    private final CookieSpec cookieSpec;
    private final BasicCookieStore cookieStore;

    /**
     * Create an instance of {@link CookieFilter} that will prevent cookies with the same name to be sent twice.
     *
     * @see CookieFilter#CookieFilter(boolean)
     */
    public RenardeCookieFilter() {
        this(false);
    }

    /**
     * Create an instance of {@link CookieFilter} that allows specifying whether or not it should accept (and thus send)
     * multiple cookies with the same name.
     * Default is <code>false</code>.
     *
     * @param allowMultipleCookiesWithTheSameName Specify whether or not to allow found two cookies with same name eg.
     *        JSESSIONID with different paths.
     */
    public RenardeCookieFilter(boolean allowMultipleCookiesWithTheSameName) {
        this.allowMultipleCookiesWithTheSameName = allowMultipleCookiesWithTheSameName;
        this.cookieSpec = new RFC6265StrictSpec();
        this.cookieStore = new BasicCookieStore();
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
            FilterContext ctx) {

        final CookieOrigin cookieOrigin = cookieOriginFromUri(requestSpec.getURI());
        for (Cookie cookie : cookieStore.getCookies()) {
            if (cookieSpec.match(cookie, cookieOrigin)
                    && allowMultipleCookiesWithTheSameNameOrCookieNotPreviouslyDefined(requestSpec, cookie)) {
                requestSpec.cookie(cookie.getName(), cookie.getValue());
            }
        }

        final Response response = ctx.next(requestSpec, responseSpec);

        List<Cookie> responseCookies = extractResponseCookies(response, cookieOrigin);
        cookieStore.addCookies(responseCookies.toArray(new Cookie[0]));
        return response;
    }

    private boolean allowMultipleCookiesWithTheSameNameOrCookieNotPreviouslyDefined(FilterableRequestSpecification requestSpec,
            Cookie cookie) {
        return allowMultipleCookiesWithTheSameName || !requestSpec.getCookies().hasCookieWithName(cookie.getName());
    }

    private List<Cookie> extractResponseCookies(Response response, CookieOrigin cookieOrigin) {

        List<Cookie> cookies = new ArrayList<Cookie>();
        for (String cookieValue : response.getHeaders().getValues("Set-Cookie")) {
            Header setCookieHeader = new BasicHeader("Set-Cookie", cookieValue);
            try {
                cookies.addAll(cookieSpec.parse(setCookieHeader, cookieOrigin));
            } catch (MalformedCookieException ignored) {
            }
        }
        return cookies;
    }

    private CookieOrigin cookieOriginFromUri(String uri) {

        try {
            URL parsedUrl = new URL(uri);
            int port = parsedUrl.getPort() != -1 ? parsedUrl.getPort() : 80;
            return new CookieOrigin(
                    parsedUrl.getHost(), port, parsedUrl.getPath(), "https".equals(parsedUrl.getProtocol()));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }
}
