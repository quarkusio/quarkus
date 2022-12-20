package io.quarkus.security.runtime;

import static io.quarkus.security.runtime.QuarkusSecurityRolesAllowedConfigBuilder.transformToKey;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorageBuilder;
import io.quarkus.security.runtime.interceptor.check.AuthenticatedCheck;
import io.quarkus.security.runtime.interceptor.check.DenyAllCheck;
import io.quarkus.security.runtime.interceptor.check.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.runtime.interceptor.check.SupplierRolesAllowedCheck;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.smallrye.config.Expressions;

@Recorder
public class SecurityCheckRecorder {

    private static volatile SecurityCheckStorage storage;
    private static final Set<SupplierRolesAllowedCheck> configExpRolesAllowedChecks = ConcurrentHashMap.newKeySet();

    public static SecurityCheckStorage getStorage() {
        return storage;
    }

    public SecurityCheck denyAll() {
        return DenyAllCheck.INSTANCE;
    }

    public SecurityCheck permitAll() {
        return PermitAllCheck.INSTANCE;
    }

    public SecurityCheck rolesAllowed(String... roles) {
        return RolesAllowedCheck.of(roles);
    }

    public SecurityCheck rolesAllowedSupplier(String[] allowedRoles, int[] configExpIndexes, int[] configKeys) {

        // here we add generated keys and values with the property expressions to the config source,
        // the config source will be registered with the Config system,
        // and we get all features available from Config
        for (int i = 0; i < configExpIndexes.length; i++) {
            QuarkusSecurityRolesAllowedConfigBuilder.addProperty(configKeys[i], allowedRoles[configExpIndexes[i]]);
        }

        final var check = new SupplierRolesAllowedCheck(
                resolveRolesAllowedConfigExp(allowedRoles, configExpIndexes, configKeys));
        configExpRolesAllowedChecks.add(check);
        return check;
    }

    private static Supplier<String[]> resolveRolesAllowedConfigExp(String[] allowedRoles, int[] configExpIndexes,
            int[] configKeys) {

        final String[] roles = Arrays.copyOf(allowedRoles, allowedRoles.length);
        return new Supplier<String[]>() {
            @Override
            public String[] get() {
                final var config = ConfigProviderResolver.instance().getConfig(Thread.currentThread().getContextClassLoader());
                if (config.getOptionalValue(Config.PROPERTY_EXPRESSIONS_ENABLED, Boolean.class).orElse(Boolean.TRUE)
                        && Expressions.isEnabled()) {
                    // property expressions are enabled
                    for (int i = 0; i < configExpIndexes.length; i++) {
                        // resolve configuration expressions specified as value of the @RolesAllowed annotation
                        roles[configExpIndexes[i]] = config.getValue(transformToKey(configKeys[i]), String.class);
                    }
                }
                return roles;
            }
        };
    }

    public SecurityCheck authenticated() {
        return AuthenticatedCheck.INSTANCE;
    }

    public RuntimeValue<SecurityCheckStorageBuilder> newBuilder() {
        return new RuntimeValue<>(new SecurityCheckStorageBuilder());
    }

    public void addMethod(RuntimeValue<SecurityCheckStorageBuilder> builder, String className,
            String methodName,
            String[] parameterTypes,
            SecurityCheck securityCheck) {
        builder.getValue().registerCheck(className, methodName, parameterTypes, securityCheck);
    }

    public void create(RuntimeValue<SecurityCheckStorageBuilder> builder) {
        storage = builder.getValue().create();
    }

    public void resolveRolesAllowedConfigExpRoles() {
        if (!configExpRolesAllowedChecks.isEmpty()) {
            for (SupplierRolesAllowedCheck configExpRolesAllowedCheck : configExpRolesAllowedChecks) {
                configExpRolesAllowedCheck.resolveAllowedRoles();
            }
            configExpRolesAllowedChecks.clear();
        }
    }
}
