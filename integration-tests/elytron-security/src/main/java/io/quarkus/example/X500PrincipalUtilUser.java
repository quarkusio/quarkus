package io.quarkus.example;

import java.security.Principal;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.security.auth.x500.X500Principal;

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
        X500Principal principal = X500PrincipalUtil.asX500Principal(dummy, true);
        return principal;
    }
}
