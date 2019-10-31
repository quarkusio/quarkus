package io.quarkus.security.runtime.interceptor;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.InvocationContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class SecurityHandler {

    private static final String HANDLER_NAME = SecurityHandler.class.getName();
    private static final String EXECUTED = "executed";

    @Inject
    SecurityConstrainer constrainer;

    public Object handle(InvocationContext ic) throws Exception {
        if (alreadyHandled(ic)) {
            return ic.proceed();
        }
        constrainer.checkRoles(ic.getMethod());
        return ic.proceed();
    }

    private boolean alreadyHandled(InvocationContext ic) {
        return ic.getContextData().put(HANDLER_NAME, EXECUTED) != null;
    }
}
