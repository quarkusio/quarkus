package io.quarkus.elytron.security.deployment;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * A build item for the {@linkplain SecurityRealm} runtime values created for the deployment. These are combined into a
 * single {@linkplain org.wildfly.security.auth.server.SecurityDomain} by the {@linkplain SecurityDeploymentProcessor}.
 */
public final class SecurityRealmBuildItem extends MultiBuildItem {
    private final RuntimeValue<SecurityRealm> realm;
    private final String name;
    private final Runnable runtimeLoadTask;

    public SecurityRealmBuildItem(RuntimeValue<SecurityRealm> realm, String name, Runnable runtimeLoadTask) {
        this.realm = realm;
        this.name = name;
        this.runtimeLoadTask = runtimeLoadTask;
    }

    public RuntimeValue<SecurityRealm> getRealm() {
        return realm;
    }

    public String getName() {
        return name;
    }

    public Runnable getRuntimeLoadTask() {
        return runtimeLoadTask;
    }
}
