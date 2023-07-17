package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;

public class Beans {

    private Beans() {
    }

    public static String toString(InjectableBean<?> bean) {
        return new StringBuilder()
                .append(bean.getKind())
                .append(" bean [class=")
                .append(bean.getBeanClass().getName())
                .append(", id=")
                .append(bean.getIdentifier())
                .append("]")
                .toString();
    }

}
