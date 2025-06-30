package io.quarkus.resteasy.runtime;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationRedirectException;

@Provider
public class AuthenticationRedirectExceptionMapper implements ExceptionMapper<AuthenticationRedirectException> {
    private static final Logger log = Logger.getLogger(AuthenticationRedirectExceptionMapper.class);

    @Override
    public Response toResponse(AuthenticationRedirectException ex) {

        ResponseBuilder builder = Response.status(ex.getCode())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache");
        if (ex.getCode() == 200) {
            // The target URL is embedded in the auto-submitted form post payload
            log.debugf("Form post redirect to %s", ex.getRedirectUri());
            builder.entity(ex.getRedirectUri())
                    .type("text/html; charset=UTF-8");
        } else {
            log.debugf("Redirect to %s ", ex.getRedirectUri());
            builder.header(HttpHeaders.LOCATION, ex.getRedirectUri());
        }
        return builder.build();
    }

}
