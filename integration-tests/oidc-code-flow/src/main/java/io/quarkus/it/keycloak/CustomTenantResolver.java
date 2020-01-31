package io.quarkus.it.keycloak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        List<String> tenantidList = context.queryParam("tenantid");
        if (tenantidList.isEmpty()) {
            Cookie cookie = context.getCookie("tenantid");
            return cookie != null ? cookie.getValue() : null;
        }
        context.response().addCookie(Cookie.cookie("tenantid", tenantidList.get(0)));
        return tenantidList.get(0);
    }
}
