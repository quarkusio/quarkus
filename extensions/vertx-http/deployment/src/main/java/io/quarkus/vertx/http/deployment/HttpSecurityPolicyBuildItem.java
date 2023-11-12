package io.quarkus.vertx.http.deployment;

import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;

/**
 * @deprecated Define {@link io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy} CDI bean with {@link #name}
 *             set as the {@link HttpSecurityPolicy#name()}.
 */
@Deprecated
public final class HttpSecurityPolicyBuildItem extends MultiBuildItem {

    final String name;
    final Supplier<HttpSecurityPolicy> policySupplier;

    public HttpSecurityPolicyBuildItem(String name, Supplier<HttpSecurityPolicy> policySupplier) {
        this.name = name;
        this.policySupplier = policySupplier;
    }

    public String getName() {
        return name;
    }

    public Supplier<HttpSecurityPolicy> getPolicySupplier() {
        return policySupplier;
    }
}
