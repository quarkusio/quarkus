package org.acme.invokedyn;

import java.util.function.Function;

public final class Identity {

    private Identity() {
    }

    public static <T> T identity(T t) {
        return t;
    }

    public static String check() {
        // The method reference is generic, so InstantiatedType only appears in the lambda's
        // instantiatedMethodType bootstrap argument: (Lorg/acme/invokedyn/InstantiatedType;)Lorg/acme/invokedyn/InstantiatedType;
        Function<InstantiatedType, InstantiatedType> identity = Identity::identity;
        return identity.apply(null) == null ? "instantiated" : "unexpected";
    }
}
