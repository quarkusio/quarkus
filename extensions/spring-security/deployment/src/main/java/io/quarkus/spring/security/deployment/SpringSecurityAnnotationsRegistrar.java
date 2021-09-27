package io.quarkus.spring.security.deployment;

import java.util.Collections;
import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

import io.quarkus.arc.processor.InterceptorBindingRegistrar;

public class SpringSecurityAnnotationsRegistrar implements InterceptorBindingRegistrar {

    private static final List<InterceptorBinding> SECURITY_BINDINGS = List.of(
            InterceptorBinding.of(Secured.class, Collections.singleton("value")),
            InterceptorBinding.of(PreAuthorize.class, Collections.singleton("value")));

    @Override
    public List<InterceptorBinding> getAdditionalBindings() {
        return SECURITY_BINDINGS;
    }
}
