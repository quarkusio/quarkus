package io.quarkus.it.security.webauthn;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.EXECUTED;
import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.SECURITY_HANDLER;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;

@Priority(Interceptor.Priority.PLATFORM_BEFORE + 190)
@CustomInterceptorBinding
@Interceptor
public class CustomInterceptor {

    public static volatile boolean sessionNotStarted = false;
    public static volatile boolean securityCheckRun = false;

    @AroundInvoke
    public Object aroundInvoke(ArcInvocationContext context) throws Exception {
        var currentSession = SessionOperations.getCurrentSession(DEFAULT_PERSISTENCE_UNIT_NAME);
        sessionNotStarted = currentSession == null;
        securityCheckRun = EXECUTED.equals(context.getContextData().get(SECURITY_HANDLER));
        return context.proceed();
    }

}
