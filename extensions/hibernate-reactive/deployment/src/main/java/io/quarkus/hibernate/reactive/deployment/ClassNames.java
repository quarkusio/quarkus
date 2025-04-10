package io.quarkus.hibernate.reactive.deployment;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;

public final class ClassNames {
    static final Set<DotName> CREATED_CONSTANTS = new HashSet<>();

    private ClassNames() {
    }

    private static DotName createConstant(String fqcn) {
        DotName result = DotName.createSimple(fqcn);
        CREATED_CONSTANTS.add(result);
        return result;
    }

    public static final DotName MUTINY_SESSION_FACTORY = createConstant("org.hibernate.reactive.mutiny.Mutiny$SessionFactory");
    public static final DotName IMPLEMENTOR = createConstant("org.hibernate.reactive.common.spi.Implementor");
}
