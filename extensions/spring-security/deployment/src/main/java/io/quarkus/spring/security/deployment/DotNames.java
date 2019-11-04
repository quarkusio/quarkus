package io.quarkus.spring.security.deployment;

import org.jboss.jandex.DotName;
import org.springframework.security.access.annotation.Secured;

public final class DotNames {

    static final DotName SPRING_SECURED = DotName.createSimple(Secured.class.getName());

    private DotNames() {
    }
}
