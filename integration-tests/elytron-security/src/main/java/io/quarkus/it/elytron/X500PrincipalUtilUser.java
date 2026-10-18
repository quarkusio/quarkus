package io.quarkus.it.elytron;

import java.security.Principal;

import javax.security.auth.x500.X500Principal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.wildfly.security.x500.util.X500PrincipalUtil;

/**
 * Simple {@linkplain X500PrincipalUtil} user to validate it works in native image
 */
@ApplicationScoped
public class X500PrincipalUtilUser {
    @Produces
    X500Principal dummyX500Principal() {
        final Principal dummy = new Principal() {
            @Override
            public String getName() {
                return "O=Fake X500Principal";
            }
        };
        System.out.printf("@Produces X500Principal called%n");
        X500Principal principal = X500PrincipalUtil.asX500Principal(dummy, true);
        System.out.printf("@Produces X500Principal created: %s%n", principal);
        return principal;
    }
}
