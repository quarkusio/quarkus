package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.Authenticated;

@Path("/code-flow-user-info")
@Authenticated
public class CodeFlowUserInfoResource {

    @Inject
    UserInfo userInfo;

    @GET
    public String access() {
        return userInfo.getString("preferred_username");
    }
}
