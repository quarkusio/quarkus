package io.quarkus.security.jpa.common.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

public class JpaIdentityProviderUtilTest {

    @Test
    public void nullClearPasswordIsAuthenticationFailure() {
        assertThatThrownBy(() -> JpaIdentityProviderUtil.getClearPassword(null))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    public void nullMcfPasswordIsAuthenticationFailure() {
        assertThatThrownBy(() -> JpaIdentityProviderUtil.getMcfPassword(null))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    public void undecodableMcfPasswordIsAuthenticationFailure() {
        assertThatThrownBy(() -> JpaIdentityProviderUtil.getMcfPassword("not-a-hash"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    public void validPasswordsStillDecode() {
        assertThat(JpaIdentityProviderUtil.getClearPassword("secret")).isNotNull();
        assertThat(JpaIdentityProviderUtil.getMcfPassword(BcryptUtil.bcryptHash("secret".toCharArray()))).isNotNull();
    }

    @Test
    public void nullRolesAddNothing() {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal("alice"));
        JpaIdentityProviderUtil.addRoles(builder, null);
        assertThat(builder.build().getRoles()).isEmpty();
    }

    @Test
    public void commaSeparatedRolesAreTrimmed() {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal("alice"));
        JpaIdentityProviderUtil.addRoles(builder, "admin, user");
        assertThat(builder.build().getRoles()).containsExactlyInAnyOrder("admin", "user");
    }
}
