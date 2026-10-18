package io.quarkus.hibernate.reactive.panache.common.runtime;

import jakarta.interceptor.InvocationContext;

import io.smallrye.mutiny.Uni;

abstract class AbstractUniInterceptor {

    @SuppressWarnings("unchecked")
    protected <T> Uni<T> proceedUni(InvocationContext context) {
        try {
            return ((Uni<T>) context.proceed());
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
        }
    }

    protected boolean isUniReturnType(InvocationContext context) {
        return context.getMethod().getReturnType().equals(Uni.class);
    }

}
