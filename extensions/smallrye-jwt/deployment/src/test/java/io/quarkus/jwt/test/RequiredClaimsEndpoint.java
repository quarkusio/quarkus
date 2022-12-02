package io.quarkus.jwt.test;

import java.util.Optional;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/endp")
@RequestScoped
@RolesAllowed("Tester")
public class RequiredClaimsEndpoint {

    @Inject
    JsonWebToken rawTokenJson;

    @GET
    @Path("/verifyIssuer")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyIssuer(@QueryParam("iss") String iss) {
        boolean pass = false;
        String msg;
        String issValue = rawTokenJson.getIssuer();
        if (issValue == null || issValue.length() == 0) {
            msg = Claims.iss.name() + "value is null or empty, FAIL";
        } else if (issValue.equals(iss)) {
            msg = Claims.iss.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.iss.name(), issValue, iss);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyRawToken")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyRawToken(@QueryParam("raw_token") String rt) {
        boolean pass = false;
        String msg;
        // raw_token
        String rawTokenValue = rawTokenJson.getRawToken();
        if (rawTokenValue == null || rawTokenValue.length() == 0) {
            msg = Claims.raw_token.name() + "value is null or empty, FAIL";
        } else if (rawTokenValue.equals(rt)) {
            msg = Claims.raw_token.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.raw_token.name(), rawTokenValue, rt);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyJTI")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyJTI(@QueryParam("jti") String jwtID) {
        boolean pass = false;
        String msg;
        // jti
        String jtiValue = rawTokenJson.getTokenID();
        if (jtiValue == null || jtiValue.length() == 0) {
            msg = Claims.jti.name() + "value is null or empty, FAIL";
        } else if (jtiValue.equals(jwtID)) {
            msg = Claims.jti.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.jti.name(), jtiValue, jwtID);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyUPN")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyUPN(@QueryParam("upn") String upn) {
        boolean pass = false;
        String msg;
        // upn
        String upnValue = rawTokenJson.getName();
        if (upnValue == null || upnValue.length() == 0) {
            msg = Claims.upn.name() + "value is null or empty, FAIL";
        } else if (upnValue.equals(upn)) {
            msg = Claims.upn.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.upn.name(), upnValue, upn);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifySUB")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifySUB(@QueryParam("sub") String sub) {
        boolean pass = false;
        String msg;
        // sub
        String subValue = rawTokenJson.getSubject();
        if (subValue == null || subValue.length() == 0) {
            msg = Claims.sub.name() + "value is null or empty, FAIL";
        } else if (subValue.equals(sub)) {
            msg = Claims.sub.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.sub.name(), subValue, sub);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyAudience")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyAudience(@QueryParam("aud") String audience) {
        boolean pass = false;
        String msg;
        // aud
        final Set<String> audValue = rawTokenJson.getAudience();
        if (audValue != null) {
            msg = Claims.aud.name() + "value is NOT null, FAIL";
        } else {
            msg = Claims.aud.name() + " PASS";
            pass = true;
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyOptionalAudience")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyAudience2(@QueryParam("aud") String audience) {
        boolean pass = false;
        String msg;
        // aud
        final Optional<Object> audValue = rawTokenJson.claim("aud");
        if (audValue.isPresent()) {
            msg = Claims.aud.name() + "value IS present, FAIL";
        } else {
            msg = Claims.aud.name() + " PASS";
            pass = true;
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyIssuedAt")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyIssuedAt(@QueryParam("iat") Long iat) {
        boolean pass = false;
        String msg;
        // iat
        Long iatValue = rawTokenJson.getIssuedAtTime();
        if (iatValue == null || iatValue.intValue() == 0) {
            msg = Claims.iat.name() + "value is null or empty, FAIL";
        } else if (iatValue.equals(iat)) {
            msg = Claims.iat.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.iat.name(), iatValue, iat);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyExpiration")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject verifyExpiration(@QueryParam("exp") Long exp) {
        boolean pass = false;
        String msg;
        // exp
        Long expValue = rawTokenJson.getExpirationTime();
        if (expValue == null || expValue.intValue() == 0) {
            msg = Claims.exp.name() + "value is null or empty, FAIL";
        } else if (expValue.equals(exp)) {
            msg = Claims.exp.name() + " PASS";
            pass = true;
        } else {
            msg = String.format("%s: %s != %s", Claims.exp.name(), expValue, exp);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

}
