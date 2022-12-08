package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Authenticated
@Path("/bearer-user-info-github-service")
public class OpaqueGithubResource {

    @Inject
    UserInfo userInfo;

    @Inject
    SecurityIdentity identity;

    @Inject
    AccessTokenCredential accessTokenCredential;

    @GET
    public String access() {
        return String.format("%s:%s:%s", identity.getPrincipal().getName(), userInfo.getString("preferred_username"),
                accessTokenCredential.getToken());
    }

}
