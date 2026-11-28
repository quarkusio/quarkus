package io.quarkus.security.jpa.common.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.spec.InvalidKeySpecException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

public class JpaIdentityProviderUtilTest {

    @Test
    public void nullClearPasswordIsIllegalState() {
        assertThatThrownBy(() -> JpaIdentityProviderUtil.getClearPassword(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stored password cannot be checked");
    }

    @Test
    public void nullMcfPasswordIsIllegalState() {
        assertThatThrownBy(() -> JpaIdentityProviderUtil.getMcfPassword(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stored password cannot be checked");
    }

    @Test
    public void undecodableMcfPasswordIsIllegalState() {
        assertThatThrownBy(() -> JpaIdentityProviderUtil.getMcfPassword("not-a-hash"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stored password cannot be checked")
                .hasCauseInstanceOf(InvalidKeySpecException.class);
    }

    @Test
    public void validPasswordsStillDecode() {
        assertThat(JpaIdentityProviderUtil.getClearPassword("secret")).isNotNull();
        assertThat(JpaIdentityProviderUtil.getMcfPassword(BcryptUtil.bcryptHash("secret".toCharArray()))).isNotNull();
    }

    @Test
    public void moreThanOneUserIsAuthenticationFailure() {
        assertThatThrownBy(() -> JpaIdentityProviderUtil.getSingleUser(List.of("a", "b")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("More than one user matched");
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
