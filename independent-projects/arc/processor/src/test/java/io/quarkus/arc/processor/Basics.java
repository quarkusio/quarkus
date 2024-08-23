package io.quarkus.arc.processor;

import org.jboss.jandex.DotName;

public final class Basics {

    public static DotName name(Class<?> clazz) {
        return DotName.createSimple(clazz.getName());
    }

}
