package io.quarkus.security.jpa.runtime;

import java.security.spec.InvalidKeySpecException;
import java.util.List;

import jakarta.persistence.Query;

import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.password.util.ModularCrypt;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

public abstract class AbstractJpaIdentityProvider {

    protected QuarkusSecurityIdentity.Builder checkPassword(Password storedPassword,
            UsernamePasswordAuthenticationRequest request) {
        PasswordGuessEvidence sentPasswordEvidence = new PasswordGuessEvidence(request.getPassword().getPassword());
        PasswordCredential storedPasswordCredential = new PasswordCredential(storedPassword);
        if (!storedPasswordCredential.verify(sentPasswordEvidence)) {
            throw new AuthenticationFailedException();
        }
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(request.getUsername()));
        builder.addCredential(request.getPassword());
        return builder;
    }

    protected QuarkusSecurityIdentity.Builder trusted(TrustedAuthenticationRequest request) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(request.getPrincipal()));
        return builder;
    }

    protected void addRoles(QuarkusSecurityIdentity.Builder builder, String roles) {
        if (roles.indexOf(',') != -1) {
            for (String role : roles.split(",")) {
                builder.addRole(role.trim());
            }
        } else {
            builder.addRole(roles.trim());
        }
    }

    protected <T> T getSingleUser(Query query) {
        @SuppressWarnings("unchecked")
        List<T> results = (List<T>) query.getResultList();
        if (results.isEmpty())
            return null;
        if (results.size() > 1)
            throw new AuthenticationFailedException();
        return results.get(0);
    }

    protected Password getClearPassword(String pass) {
        return ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, pass.toCharArray());
    }

    protected Password getMcfPassword(String pass) {
        try {
            return ModularCrypt.decode(pass);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
