package io.quarkus.security.runtime.interceptor;

import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.SECURITY_INTERCEPTOR_PRIORITY;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.RunAsUser;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * This is a CDI interceptor used for the {@link RunAsUser} interceptor binding type.
 * The interceptor binding type is registered during the build time via the annotation transformer.
 * Also, the {@link RunAsUser} interceptor binding is currently registered via a build item.
 */
@Interceptor
@Priority(SECURITY_INTERCEPTOR_PRIORITY)
public final class RunAsUserInterceptor {

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @AroundInvoke
    Object intercept(InvocationContext ic) throws Exception {
        Class<?> returnType = ic.getMethod().getReturnType();
        if (Uni.class.isAssignableFrom(returnType)) {
            configureIdentityFromAnnotation(ic);
            try {
                return ((Uni<?>) ic.proceed()).onTermination().invoke(this::cleanIdentity);
            } catch (Throwable throwable) {
                cleanIdentity();
                throw throwable;
            }
        } else if (CompletionStage.class.isAssignableFrom(returnType)) {
            configureIdentityFromAnnotation(ic);
            try {
                return ((CompletionStage<?>) ic.proceed()).whenComplete((item, failure) -> cleanIdentity());
            } catch (Throwable throwable) {
                cleanIdentity();
                throw throwable;
            }
        } else {
            configureIdentityFromAnnotation(ic);
            try {
                return ic.proceed();
            } finally {
                cleanIdentity();
            }
        }
    }

    private void cleanIdentity() {
        try {
            identityAssociation.setIdentity((SecurityIdentity) null);
        } catch (Throwable ignored) {
            // theoretically, this could be for example inactive CDI request
            // make this silent, we only try to "clear" the association to cover every eventuality
            // currently supported scheduled methods should not need it anyway
        }
    }

    private void configureIdentityFromAnnotation(InvocationContext ic) {
        identityAssociation.setIdentity(createIdentity(ic));
    }

    private static SecurityIdentity createIdentity(InvocationContext ic) {
        var runAsUser = ic.getInterceptorBinding(RunAsUser.class);
        var identityBuilder = QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal(runAsUser.user()));
        if (runAsUser.roles() != null && runAsUser.roles().length > 0) {
            identityBuilder.addRoles(Set.of(runAsUser.roles()));
        }
        return identityBuilder.build();
    }
}
