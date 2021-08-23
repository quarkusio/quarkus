package io.quarkus.security.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorageBuilder;
import io.quarkus.security.runtime.interceptor.check.AuthenticatedCheck;
import io.quarkus.security.runtime.interceptor.check.DenyAllCheck;
import io.quarkus.security.runtime.interceptor.check.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;

@Recorder
public class SecurityCheckRecorder {

    private static volatile SecurityCheckStorage storage;

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

}
