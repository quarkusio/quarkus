package io.quarkus.resteasy.reactive.server.test.security;

import java.io.IOException;
import java.security.Principal;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class SecurityOverrideFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String user = requestContext.getHeaders().getFirst("User");
        String role = requestContext.getHeaders().getFirst("Role");
        if (user != null && role != null) {
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return new Principal() {
                        @Override
                        public String getName() {
                            return user;
                        }
                    };
                }

                @Override
                public boolean isUserInRole(String r) {
                    return role.equals(r);
                }

                @Override
                public boolean isSecure() {
                    return false;
                }

                @Override
                public String getAuthenticationScheme() {
                    return "basic";
                }
            });
        }

    }
}
