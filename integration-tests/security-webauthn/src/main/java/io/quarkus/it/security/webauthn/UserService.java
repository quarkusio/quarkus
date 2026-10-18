package io.quarkus.it.security.webauthn;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
class UserService {

    @CustomInterceptorBinding
    @WithTransaction
    @Authenticated
    Uni<Long> findUserIdByName(String username) {
        return User.findByUsername(username).map(u -> u.id);
    }

}
