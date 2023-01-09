package io.quarkus.keycloak.admin.client.common;

import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.BeanDestroyer;

public final class AutoCloseableDestroyer implements BeanDestroyer<AutoCloseable> {

    @Override
    public void destroy(AutoCloseable instance, CreationalContext<AutoCloseable> creationalContext,
            Map<String, Object> params) {
        try {
            instance.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
