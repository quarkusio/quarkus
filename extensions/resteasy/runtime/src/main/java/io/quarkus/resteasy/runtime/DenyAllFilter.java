package io.quarkus.resteasy.runtime;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

/**
 * A filter associated with the DenyAll common security annotation
 * 
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 6/12/18
 */
@Priority(Priorities.AUTHORIZATION)
public class DenyAllFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        RequestFailer.fail(requestContext);
    }
}
