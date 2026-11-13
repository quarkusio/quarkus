package org.acme.model.api;

import org.acme.model.closed.PublicModel;

public final class Models {
    public static Class<?> publicType() {
        return PublicModel.class;
    }

    public static Class<?> hiddenType() throws ClassNotFoundException {
        return Class.forName("org.acme.model.closed.HiddenModel");
    }
}
