package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@ApplicationPath("/")
@SecurityScheme(ref = "oidc_auth")
public class ApplicationContext extends Application {
}
