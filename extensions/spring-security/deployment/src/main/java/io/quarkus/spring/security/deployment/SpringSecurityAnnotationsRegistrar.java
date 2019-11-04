package io.quarkus.spring.security.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.springframework.security.access.annotation.Secured;

import io.quarkus.arc.processor.InterceptorBindingRegistrar;

public class SpringSecurityAnnotationsRegistrar implements InterceptorBindingRegistrar {

    public static final Map<DotName, Set<String>> SECURITY_BINDINGS = new HashMap<>();

    static {
        SECURITY_BINDINGS.put(DotName.createSimple(Secured.class.getName()), Collections.singleton("value"));
    }

    @Override
    public Map<DotName, Set<String>> registerAdditionalBindings() {
        return SECURITY_BINDINGS;
    }
}
