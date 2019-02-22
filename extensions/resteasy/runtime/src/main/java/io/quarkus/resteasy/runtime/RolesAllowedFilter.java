package io.quarkus.resteasy.runtime;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

/**
 * Supports role based access to an endpoint
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/12/18
 */
@Priority(Priorities.AUTHORIZATION)
public class RolesAllowedFilter implements ContainerRequestFilter {

    private final Set<String> allowedRoles;
    private final boolean allRolesAllowed;

    public RolesAllowedFilter(String[] allowedRoles) {
        this.allowedRoles = new HashSet<>(asList(allowedRoles));
        this.allRolesAllowed = this.allowedRoles.stream().anyMatch("*"::equals);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        SecurityContext securityContext = requestContext.getSecurityContext();
        boolean isForbidden;
        if (allRolesAllowed) {
            isForbidden = securityContext.getUserPrincipal() == null;
        } else {
            isForbidden = allowedRoles.stream().noneMatch(securityContext::isUserInRole);
        }
        if (isForbidden) {
            RequestFailer.fail(requestContext);
        }
    }
}
