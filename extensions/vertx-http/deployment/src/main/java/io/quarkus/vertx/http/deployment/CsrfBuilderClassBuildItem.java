package io.quarkus.vertx.http.deployment;

import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.vertx.http.security.CSRF;

/**
 * At most one extension can register a CSRF builder used by the HTTP Security fluent API.
 * The builder class must have public no-args constructor.
 */
public final class CsrfBuilderClassBuildItem extends SimpleBuildItem {

    final Class<? extends CSRF.Builder> csrfBuilderClass;

    public CsrfBuilderClassBuildItem(Class<? extends CSRF.Builder> csrfBuilderClass) {
        this.csrfBuilderClass = Objects.requireNonNull(csrfBuilderClass);
    }
}
