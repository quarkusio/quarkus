package io.quarkus.flyway.multitenant.runtime;

import static io.quarkus.flyway.runtime.FlywayCreator.TENANT_ID_DEFAULT;

import java.lang.annotation.Annotation;

public final class FlywayMultiTenantContainerUtil {
    private FlywayMultiTenantContainerUtil() {
    }

    public static Annotation getFlywayContainerQualifier(String persistenceUnitName) {
        return getFlywayContainerQualifier(persistenceUnitName, TENANT_ID_DEFAULT);
    }

    public static Annotation getFlywayContainerQualifier(String persistenceUnitName, String tenantId) {
        return FlywayPersistenceUnit.FlywayPersistenceUnitLiteral.of(persistenceUnitName, tenantId);
    }
}
