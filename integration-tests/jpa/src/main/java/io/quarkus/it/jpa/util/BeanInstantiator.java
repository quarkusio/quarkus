package io.quarkus.it.jpa.util;

import java.util.Arrays;
import java.util.Objects;

public enum BeanInstantiator {
    HIBERNATE,
    ARC;

    public static BeanInstantiator fromCaller() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .map(e -> {
                    var className = e.getClassName();
                    if (className.startsWith("io.quarkus.it.")) {
                        // Class from this integration test: ignore.
                        return null;
                    }
                    if (className.startsWith("io.quarkus.")) {
                        return ARC;
                    }
                    if (className.startsWith("org.hibernate.")) {
                        return HIBERNATE;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not determine bean instantiator"));
    }
}
