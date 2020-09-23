package io.quarkus.rest.runtime.core.parameters;

import java.util.Collections;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.vertx.core.http.Cookie;

public class CookieParamExtractor implements ParameterExtractor {

    private final String name;

    public CookieParamExtractor(String name) {
        this.name = name;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        return context.getCookieParameter(name);
    }
}
