package io.quarkus.security.jpa.reactive;

import static io.quarkus.security.jpa.common.runtime.JpaIdentityProviderUtil.addRoles;
import static io.quarkus.security.jpa.common.runtime.JpaIdentityProviderUtil.checkPassword;
import static io.quarkus.security.jpa.common.runtime.JpaIdentityProviderUtil.getClearPassword;

import jakarta.inject.Singleton;

import org.hibernate.reactive.common.Identifier;
import org.hibernate.reactive.mutiny.Mutiny;
import org.wildfly.security.password.Password;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.reactive.runtime.JpaReactiveIdentityProvider;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@Singleton
public class UserEntityIdentityProvider extends JpaReactiveIdentityProvider {

    @Override
    public Uni<SecurityIdentity> authenticate(Mutiny.Session session, UsernamePasswordAuthenticationRequest request) {
        return session.find(PlainUserEntity.class, Identifier.id("name", request.getUsername())).onItem().ifNotNull()
                .transform(user -> {
                    Password storedPassword = getClearPassword(user.pass);
                    QuarkusSecurityIdentity.Builder builder = checkPassword(storedPassword, request);
                    addRoles(builder, user.role);
                    return builder.build();
                });
    }
}
