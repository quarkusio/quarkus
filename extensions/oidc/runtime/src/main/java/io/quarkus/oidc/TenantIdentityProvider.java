package io.quarkus.oidc;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Tenant-specific {@link SecurityIdentity} provider. Associated tenant configuration needs to be selected
 * with the {@link Tenant} qualifier. When injection point is not annotated with the {@link Tenant}
 * qualifier, default tenant is selected.
 */
public interface TenantIdentityProvider {

    Uni<SecurityIdentity> authenticate(AccessTokenCredential token);

}
