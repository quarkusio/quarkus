package io.quarkus.reactivemessaging.http.runtime.config;

import io.vertx.core.http.HttpMethod;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 27/09/2019
 */
public class HttpStreamConfig {
    public final HttpMethod method;
    public final String path;

    public HttpStreamConfig(String path, String method, String name) {
        this.path = path;
        this.method = toHttpMethod(method, name);
    }

    public String path() {
        return path;
    }

    private HttpMethod toHttpMethod(String method, String connectorName) {
        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid http method '" + method + "' defined for connector " + connectorName);
        }
    }
}
