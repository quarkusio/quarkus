package io.quarkus.resteasy.runtime;

import java.lang.reflect.Method;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.core.ResteasyContext;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.ext.web.RoutingContext;

/**
 * Helper class for JAXRS request failure responses.
 */
public class RequestFailer {

    //Servlet API may not be present
    private static final Class<?> HTTP_SERVLET_REQUEST;
    private static final Class<?> HTTP_SERVLET_RESPONSE;
    private static final Method AUTHENTICATE;

    static {
        Class<?> httpServletReq = null;
        Class<?> httpServletResp = null;
        Method auth = null;
        try {
            httpServletReq = Class.forName("javax.servlet.http.HttpServletRequest");
            httpServletResp = Class.forName("javax.servlet.http.HttpServletResponse");
            auth = httpServletReq.getMethod("authenticate", httpServletResp);
        } catch (Exception e) {

        }
        AUTHENTICATE = auth;
        HTTP_SERVLET_REQUEST = httpServletReq;
        HTTP_SERVLET_RESPONSE = httpServletResp;
    }

    public static void fail(ContainerRequestContext requestContext) {
        if (requestContext.getSecurityContext().getUserPrincipal() == null) {

            SecurityIdentity identity = CurrentIdentityAssociation.current();
            if (HTTP_SERVLET_REQUEST != null) {
                Object httpServletRequest = ResteasyContext.getContextData(HTTP_SERVLET_REQUEST);
                if (httpServletRequest != null) {
                    Object httpServletResponse = ResteasyContext.getContextData(HTTP_SERVLET_RESPONSE);
                    try {
                        AUTHENTICATE.invoke(httpServletRequest, httpServletResponse);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }
            }
            HttpAuthenticator authenticator = identity.getAttribute(HttpAuthenticator.class.getName());
            RoutingContext context = ResteasyContext.getContextData(RoutingContext.class);
            if (authenticator != null && context != null) {
                authenticator.sendChallenge(context, null);
            } else {
                respond(requestContext, 401, "Not authorized");
            }

        } else {
            respond(requestContext, 403, "Access forbidden: role not allowed");
        }
    }

    private static void respond(ContainerRequestContext context, int status, String message) {
        Response response = Response.status(status)
                .entity(message)
                .type("text/html;charset=UTF-8")
                .build();

        context.abortWith(response);
    }
}
