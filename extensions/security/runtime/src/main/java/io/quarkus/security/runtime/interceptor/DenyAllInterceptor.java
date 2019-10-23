package io.quarkus.security.runtime.interceptor;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Interceptor
@DenyAll
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class DenyAllInterceptor {

    @Inject
    SecurityHandler handler;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        return handler.handle(ic);
    }
}
