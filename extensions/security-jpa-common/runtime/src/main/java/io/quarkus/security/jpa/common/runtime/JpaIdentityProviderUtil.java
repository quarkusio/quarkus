package io.quarkus.security.jpa.common.runtime;

import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.UUID;

import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.password.util.ModularCrypt;
import org.wildfly.security.provider.util.ProviderUtil;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.PasswordType;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

public class JpaIdentityProviderUtil {

    private JpaIdentityProviderUtil() {
        // utility class used by generated classes
    }

    public static QuarkusSecurityIdentity.Builder checkPassword(Password storedPassword,
            UsernamePasswordAuthenticationRequest request) {
        PasswordGuessEvidence sentPasswordEvidence = new PasswordGuessEvidence(request.getPassword().getPassword());
        PasswordCredential storedPasswordCredential = new PasswordCredential(storedPassword);
        if (!storedPasswordCredential.verify(ProviderUtil.INSTALLED_PROVIDERS, sentPasswordEvidence)) {
            throw new AuthenticationFailedException();
        }
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(request.getUsername()));
        builder.addCredential(request.getPassword());
        return builder;
    }

    public static QuarkusSecurityIdentity.Builder trusted(TrustedAuthenticationRequest request) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(request.getPrincipal()));
        return builder;
    }

    public static void addRoles(QuarkusSecurityIdentity.Builder builder, String roles) {
        if (roles.indexOf(',') != -1) {
            for (String role : roles.split(",")) {
                builder.addRole(role.trim());
            }
        } else {
            builder.addRole(roles.trim());
        }
    }

    public static <T> T getSingleUser(List<T> results) {
        if (results.isEmpty())
            return null;
        if (results.size() > 1)
            throw new AuthenticationFailedException();
        return results.get(0);
    }

    public static Password getClearPassword(String pass) {
        return ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, pass.toCharArray());
    }

    public static Password getMcfPassword(String pass) {
        try {
            return ModularCrypt.decode(pass);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static void passwordAction(PasswordType type) {
        String uuid = UUID.randomUUID().toString();
        if (type == PasswordType.CLEAR) {
            ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, uuid.toCharArray());
        } else {
            BcryptUtil.bcryptHash(uuid);
        }
    }
}
