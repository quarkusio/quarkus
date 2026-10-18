package io.quarkus.oidc.db.token.state.manager;

import java.time.Instant;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/token-state-manager-generator")
public class OidcDbTokenStateManagerResource {

    private static final long EXPIRED_EXTRA_GRACE = 30;

    @Inject
    EntityManager em;

    @Transactional
    @POST
    public void create(Long numOfTokens) {
        long expiresIn5Sec = Instant.now().getEpochSecond() + 5 - EXPIRED_EXTRA_GRACE;
        for (int i = 0; i < numOfTokens; i++) {
            var token = new OidcDbTokenStateManagerEntity();
            token.idToken = "ID TOKEN " + i;
            token.accessToken = "ACCESS TOKEN " + i;
            token.refreshToken = "REFRESH TOKEN " + i;
            token.accessTokenExpiresIn = 10L + i;
            token.expiresIn = expiresIn5Sec;
            token.id = UUID.randomUUID().toString() + Instant.now().getEpochSecond();
            em.persist(token);
        }
    }

}
