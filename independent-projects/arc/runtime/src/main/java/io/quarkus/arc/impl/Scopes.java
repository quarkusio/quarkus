package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;

class Scopes {

    private Scopes() {
    }

    static boolean scopeMatches(InjectableContext context, InjectableBean<?> bean) {
        return context.getScope().getName().equals(bean.getScope().getName());
    }

    static IllegalArgumentException scopeDoesNotMatchException(InjectableContext context, InjectableBean<?> bean) {
        return new IllegalArgumentException(String.format(
                "The scope of the bean [%s] does not match the scope of the context [%s]:\n\t- %s bean with bean class %s and identifier %s \n\t- context %s",
                bean.getScope().getName(), context.getScope().getName(), bean.getKind(), bean.getBeanClass().getName(),
                bean.getIdentifier(),
                context.getClass().getName()));
    }

}
