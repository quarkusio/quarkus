package io.quarkus.security.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SecurityAnnotationsRegistrar implements InterceptorBindingRegistrar {

    public static final Map<DotName, Set<String>> SECURITY_BINDINGS = new HashMap<>();

    static {
        // keep the contents the same as in io.quarkus.resteasy.deployment.SecurityTransformerUtils
        SECURITY_BINDINGS.put(DotName.createSimple(RolesAllowed.class.getName()), Collections.singleton("value"));
        SECURITY_BINDINGS.put(DotName.createSimple(Authenticated.class.getName()), Collections.emptySet());
        SECURITY_BINDINGS.put(DotName.createSimple(DenyAll.class.getName()), Collections.emptySet());
        SECURITY_BINDINGS.put(DotName.createSimple(PermitAll.class.getName()), Collections.emptySet());
    }

    @Override
    public Map<DotName, Set<String>> registerAdditionalBindings() {
        return SECURITY_BINDINGS;
    }
}
