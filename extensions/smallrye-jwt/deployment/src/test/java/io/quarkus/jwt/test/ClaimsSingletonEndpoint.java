package io.quarkus.jwt.test;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonString;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;

import io.quarkus.security.Authenticated;

@Path("/endp")
@Authenticated
public class ClaimsSingletonEndpoint {
    @Inject
    @Claim(standard = Claims.upn)
    ClaimValue<JsonString> upn;
    @Inject
    @Claim(standard = Claims.raw_token)
    Instance<String> rawToken;
    @Inject
    @Claim(standard = Claims.groups)
    Provider<Set<String>> groups;

    @GET
    @Path("claims")
    @Produces(MediaType.TEXT_PLAIN)
    public String verifyGroups() {
        return upn.getValue().getString() + ":" + groups.get().stream().collect(Collectors.joining(",")) + ":"
                + rawToken.get();
    }
}
