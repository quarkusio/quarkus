package io.quarkus.spring.security.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

public final class DotNames {

    static final DotName STRING = DotName.createSimple(String.class.getName());
    static final DotName PRIMITIVE_BOOLEAN = DotName.createSimple(boolean.class.getName());

    static final DotName SPRING_SECURED = DotName.createSimple(Secured.class.getName());
    static final DotName SPRING_PRE_AUTHORIZE = DotName.createSimple(PreAuthorize.class.getName());

    static final Set<DotName> SUPPORTED_SPRING_SECURITY_ANNOTATIONS = new HashSet<>(
            Arrays.asList(SPRING_SECURED, SPRING_PRE_AUTHORIZE));

    private DotNames() {
    }
}
