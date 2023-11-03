package org.jboss.resteasy.reactive.server.core.parameters;

import jakarta.ws.rs.core.Cookie;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class CookieParamExtractor implements ParameterExtractor {

    private final String name;
    private final String parameterTypeName;

    public CookieParamExtractor(String name, String parameterTypeName) {
        this.name = name;
        this.parameterTypeName = parameterTypeName;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        if (Cookie.class.getName().equals(parameterTypeName)) {
            // we need to make sure we preserve the name because otherwise CookieHeaderDelegate will not be able to convert back to Cookie
            Cookie cookie = context.getHttpHeaders().getCookies().get(name);
            return cookie != null ? cookie.toString() : null;
        }
        return context.getCookieParameter(name);
    }
}
