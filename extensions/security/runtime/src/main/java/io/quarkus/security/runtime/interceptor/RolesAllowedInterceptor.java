package io.quarkus.security.runtime.interceptor;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Interceptor
@RolesAllowed("")
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class RolesAllowedInterceptor {

    @Inject
    SecurityHandler handler;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        return handler.handle(ic);
    }
}
