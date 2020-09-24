package io.quarkus.rest.runtime.core.parameters;

import java.util.Collections;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.vertx.core.http.Cookie;

public class CookieParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;

    public CookieParamExtractor(String name, boolean single) {
        this.name = name;
        this.single = single;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        Cookie cookie = context.getContext().cookieMap().get(name);
        if (cookie == null) {
            return null;
        }
        if (single) {
            return cookie.getValue();
        } else {
            return Collections.singletonList(cookie.getValue());
        }
    }
}
