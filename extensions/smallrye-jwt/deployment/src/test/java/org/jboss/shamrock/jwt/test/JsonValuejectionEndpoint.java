package org.jboss.shamrock.jwt.test;


import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@Path("/endp")
@RolesAllowed({"Echoer", "Tester"})
@RequestScoped
public class JsonValuejectionEndpoint {
    @Inject
    @Claim("raw_token")
    JsonString rawToken;
    @Inject
    @Claim("iss")
    JsonString issuer;
    @Inject
    @Claim("jti")
    JsonString jti;
    @Inject
    @Claim("aud")
    JsonArray aud;
    @Inject
    @Claim("roles")
    JsonArray roles;
    @Inject
    @Claim("iat")
    JsonNumber issuedAt;
    @Inject
    @Claim("auth_time")
    JsonNumber authTime;
    @Inject
    @Claim("customString")
    JsonString customString;
    @Inject
    @Claim("customInteger")
    JsonNumber customInteger;
    @Inject
    @Claim("customDouble")
    JsonNumber customDouble;
    @Inject
    @Claim("customObject")
    JsonObject customObject;
    @Inject
    @Claim("customStringArray")
    JsonArray customStringArray;
    @Inject
    @Claim("customIntegerArray")
    JsonArray customIntegerArray;
    @Inject
    @Claim("customDoubleArray")
    JsonArray customDoubleArray;

    @GET
    @Path("/verifyInjectedIssuer")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedIssuer(@QueryParam("iss") String iss) {
        boolean pass = false;
        String msg;
        String issValue = issuer.getString();
        if(issValue == null || issValue.length() == 0) {
            msg = Claims.iss.name()+"value is null or empty, FAIL";
        }
        else if(issValue.equals(iss)) {
            msg = Claims.iss.name()+" PASS";
            pass = true;
        }
        else {
            msg = String.format("%s: %s != %s", Claims.iss.name(), issValue, iss);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }
    @GET
    @Path("/verifyInjectedRawToken")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedRawToken(@QueryParam("raw_token") String rt) {
        boolean pass = false;
        String msg;
        // raw_token
        String rawTokenValue = rawToken.getString();
        if(rawTokenValue == null || rawTokenValue.length() == 0) {
            msg = Claims.raw_token.name()+"value is null or empty, FAIL";
        }
        else if(rawTokenValue.equals(rt)) {
            msg = Claims.raw_token.name()+" PASS";
            pass = true;
        }
        else {
            msg = String.format("%s: %s != %s", Claims.raw_token.name(), rawTokenValue, rt);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }
    @GET
    @Path("/verifyInjectedJTI")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedJTI(@QueryParam("jti") String jwtID) {
        boolean pass = false;
        String msg;
        // jti
        String jtiValue = jti.getString();
        if(jtiValue == null || jtiValue.length() == 0) {
            msg = Claims.jti.name()+"value is null or empty, FAIL";
        }
        else if(jtiValue.equals(jwtID)) {
            msg = Claims.jti.name()+" PASS";
            pass = true;
        }
        else {
            msg = String.format("%s: %s != %s", Claims.jti.name(), jtiValue, jwtID);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }
    @GET
    @Path("/verifyInjectedAudience")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedAudience(@QueryParam("aud") String audience) {
        boolean pass = false;
        String msg;
        // aud
        List<JsonString> audValue = aud.getValuesAs(JsonString.class);
        if(audValue == null || audValue.size() == 0) {
            msg = Claims.aud.name()+"value is null or empty, FAIL";
        }
        else if(audValue.get(0).getString().equals(audience)) {
            msg = Claims.aud.name()+" PASS";
            pass = true;
        }
        else {
            msg = String.format("%s: %s != %s", Claims.aud.name(), audValue, audience);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }
    @GET
    @Path("/verifyInjectedIssuedAt")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedIssuedAt(@QueryParam("iat") Long iat) {
        boolean pass = false;
        String msg;
        // iat
        Long iatValue = issuedAt.longValue();
        if(iatValue == null || iatValue.intValue() == 0) {
            msg = Claims.iat.name()+"value is null or empty, FAIL";
        }
        else if(iatValue.equals(iat)) {
            msg = Claims.iat.name()+" PASS";
            pass = true;
        }
        else {
            msg = String.format("%s: %s != %s", Claims.iat.name(), iatValue, iat);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }
    @GET
    @Path("/verifyInjectedAuthTime")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedAuthTime(@QueryParam("auth_time") Long authTime) {
        boolean pass = false;
        String msg;
        // auth_time
        Long authTimeValue = this.authTime.longValue();
        if(authTimeValue == null) {
            msg = Claims.auth_time.name()+" value is null or missing, FAIL";
        }
        else if(authTimeValue.equals(authTime)) {
            msg = Claims.auth_time.name()+" PASS";
            pass = true;
        }
        else {
            msg = String.format("%s: %s != %s", Claims.auth_time.name(), authTimeValue, authTime);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomString")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedCustomString(@QueryParam("value") String value) {
        boolean pass = false;
        String msg;
        // iat
        String customValue = customString.getString();
        if(customValue == null || customValue.length() == 0) {
            msg = "customString value is null or empty, FAIL";
        }
        else if(customValue.equals(value)) {
            msg = "customString PASS";
            pass = true;
        }
        else {
            msg = String.format("customString: %s != %s", customValue, value);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomInteger")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedCustomInteger(@QueryParam("value") Long value) {
        boolean pass = false;
        String msg;
        // iat
        Long customValue = customInteger.longValue();
        if(customValue.equals(value)) {
            msg = "customInteger PASS";
            pass = true;
        }
        else {
            msg = String.format("customInteger: %d != %d", customValue, value);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomDouble")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedCustomDouble(@QueryParam("value") Double value) {
        boolean pass = false;
        String msg;
        // iat
        Double customValue = customDouble.doubleValue();
        if(Math.abs(customValue.doubleValue() - value.doubleValue()) < 0.0000001) {
            msg = "customDouble PASS";
            pass = true;
        }
        else {
            msg = String.format("customDouble: %s != %.8f", customValue, value);
        }
        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg)
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomStringArray")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedCustomStringArray(@QueryParam("value") List<String> array) {
        boolean pass = false;
        HashSet<String> expected = new HashSet<>();
        for(String value : array) {
            expected.add(value);
        }
        StringBuilder msg = new StringBuilder();

        if(customStringArray == null || customStringArray.size() == 0) {
            msg.append("customStringArray value is null or empty, FAIL");
        }
        else if(customStringArray.size() != array.size()) {
            msg.append(String.format("customStringArray.size(%d) != expected.size(%d)", customStringArray.size(), array.size()));
        }
        else {
            for(int n = 0; n < customStringArray.size(); n ++) {
                JsonString js = customStringArray.getJsonString(n);
                if(!expected.remove(js.getString())) {
                    msg.append(String.format("%s not found in expected", js.getString()));
                }
            }
            pass = expected.size() == 0;
        }

        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg.toString())
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomIntegerArray")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedCustomIntegerArray(@QueryParam("value") List<Long> array) {
        boolean pass = false;
        HashSet<Long> expected = new HashSet<>();
        for(Long value : array) {
            expected.add(value);
        }
        StringBuilder msg = new StringBuilder();

        if(customIntegerArray == null || customIntegerArray.size() == 0) {
            msg.append("customStringArray value is null or empty, FAIL");
        }
        else if(customIntegerArray.size() != array.size()) {
            msg.append(String.format("customStringArray.size(%d) != expected.size(%d)", customIntegerArray.size(), array.size()));
        }
        else {
            for(int n = 0; n < customIntegerArray.size(); n ++) {
                Long value = customIntegerArray.getJsonNumber(n).longValue();
                if(!expected.remove(value)) {
                    msg.append(String.format("%s not found in expected", value));
                }
            }
            pass = expected.size() == 0;
        }

        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg.toString())
                .build();
        return result;
    }

    @GET
    @Path("/verifyInjectedCustomDoubleArray")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("Tester")
    public JsonObject verifyInjectedCustomDoubleArray(@QueryParam("value") List<Double> array) {
        boolean pass = false;
        HashSet<BigDecimal> expected = new HashSet<>();
        for(Double value : array) {
            expected.add(BigDecimal.valueOf(value));
        }
        StringBuilder msg = new StringBuilder();

        if(customDoubleArray == null || customDoubleArray.size() == 0) {
            msg.append("customStringArray value is null or empty, FAIL");
        }
        else if(customDoubleArray.size() != array.size()) {
            msg.append(String.format("customStringArray.size(%d) != expected.size(%d)", customDoubleArray.size(), array.size()));
        }
        else {
            for(int n = 0; n < customDoubleArray.size(); n ++) {
                BigDecimal value = customDoubleArray.getJsonNumber(n).bigDecimalValue();
                if(!expected.remove(value)) {
                    msg.append(String.format("%s not found in expected", value));
                }
            }
            pass = expected.size() == 0;
        }

        JsonObject result = Json.createObjectBuilder()
                .add("pass", pass)
                .add("msg", msg.toString())
                .build();
        return result;
    }
}
