package io.quarkus.security.deployment;

import java.util.Collections;
import java.util.List;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SecurityAnnotationsRegistrar implements InterceptorBindingRegistrar {

    static final List<InterceptorBinding> SECURITY_BINDINGS = List.of(
            // keep the contents the same as in io.quarkus.resteasy.deployment.SecurityTransformerUtils
            InterceptorBinding.of(RolesAllowed.class, Collections.singleton("value")),
            InterceptorBinding.of(Authenticated.class),
            InterceptorBinding.of(DenyAll.class),
            InterceptorBinding.of(PermitAll.class));

    @Override
    public List<InterceptorBinding> getAdditionalBindings() {
        return SECURITY_BINDINGS;
    }
}
