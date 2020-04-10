package io.quarkus.security.jpa.runtime;

import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.FlushMode;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.password.util.ModularCrypt;

import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity.Builder;
import io.smallrye.mutiny.Uni;

public abstract class JpaIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    private static Logger log = Logger.getLogger(JpaIdentityProvider.class);

    @Inject
    JPAConfig jpaConfig;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return context.runBlocking(new Supplier<SecurityIdentity>() {
            @Override
            public SecurityIdentity get() {
                // FIXME: unit name
                EntityManager em = jpaConfig.getEntityManagerFactory(null).createEntityManager();
                ((org.hibernate.Session) em).setHibernateFlushMode(FlushMode.MANUAL);
                ((org.hibernate.Session) em).setDefaultReadOnly(true);
                try {
                    return authenticate(em, request);
                } catch (SecurityException e) {
                    log.debug("Authentication failed", e);
                    throw new AuthenticationFailedException();
                } finally {
                    em.close();
                }
            }
        });
    }

    public abstract SecurityIdentity authenticate(EntityManager em,
            UsernamePasswordAuthenticationRequest request);

    protected Builder checkPassword(Password storedPassword, UsernamePasswordAuthenticationRequest request) {
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

    protected void addRoles(Builder builder, String roles) {
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
        return ClearPassword.createRaw("clear", pass.toCharArray());
    }

    protected Password getMcfPassword(String pass) {
        try {
            return ModularCrypt.decode(pass);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
