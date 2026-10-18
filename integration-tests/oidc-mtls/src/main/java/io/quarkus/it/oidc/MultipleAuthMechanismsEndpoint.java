package io.quarkus.it.oidc;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/multiple-auth-mechanisms")
@Authenticated
public class MultipleAuthMechanismsEndpoint {

    @GET
    @Path("mtls-jwt-lax")
    public String getMtlsJwtLax() {
        return "ignored";
    }

    @MtlsJwt
    @GET
    @Path("mtls-jwt")
    public String getMtlsJwt() {
        return "ignored";
    }

    @MtlsBasic
    @GET
    @Path("mtls-basic")
    public String getMtlsBasic() {
        return "ignored";
    }
}
