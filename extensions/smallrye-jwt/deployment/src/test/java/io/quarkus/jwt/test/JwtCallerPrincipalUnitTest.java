package io.quarkus.jwt.test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.smallrye.jwt.runtime.auth.ElytronJwtCallerPrincipal;

public class JwtCallerPrincipalUnitTest {
    @Test
    public void testAllClaims() throws InvalidJwtException {
        InputStream is = getClass().getResourceAsStream("/Token1.json");
        JsonObject content = Json.createReader(is).readObject();
        JwtClaims jwtClaims = JwtClaims.parse(content.toString());
        ElytronJwtCallerPrincipal principal = new ElytronJwtCallerPrincipal("testAllClaims", jwtClaims);

        String iss = principal.getIssuer();
        Assertions.assertEquals("https://server.example.com", iss);
        String jti = principal.getTokenID();
        Assertions.assertEquals("a-123", jti);
        String name = principal.getName();
        Assertions.assertEquals("testAllClaims", name);
        String upn = principal.getClaim(Claims.upn.name());
        Assertions.assertEquals("jdoe@example.com", upn);
        Set<String> aud = principal.getAudience();
        Assertions.assertEquals(new HashSet<>(Arrays.asList("s6BhdRkqt3")), aud);
        Long exp = principal.getExpirationTime();
        Assertions.assertEquals(1311281970l, exp.longValue());
        Long iat = principal.getIssuedAtTime();
        Assertions.assertEquals(1311280970l, iat.longValue());
        String sub = principal.getSubject();
        Assertions.assertEquals("24400320", sub);
        Set<String> groups = principal.getGroups();
        String[] expectedGroups = { "Echoer",
                "Tester",
                "group1",
                "group2" };
        Assertions.assertEquals(new HashSet<String>(Arrays.asList(expectedGroups)), groups);

        /*
         * "customDoubleArray": [0.1, 1.1, 2.2, 3.3, 4.4],
         */
        JsonArray customDoubleArray = principal.getClaim("customDoubleArray");
        Assertions.assertEquals(5, customDoubleArray.size());
        Assertions.assertEquals(Json.createValue(0.1), customDoubleArray.getJsonNumber(0));
        Assertions.assertEquals(Json.createValue(1.1), customDoubleArray.getJsonNumber(1));
        Assertions.assertEquals(Json.createValue(2.2), customDoubleArray.getJsonNumber(2));
        Assertions.assertEquals(Json.createValue(3.3), customDoubleArray.getJsonNumber(3));
        Assertions.assertEquals(Json.createValue(4.4), customDoubleArray.getJsonNumber(4));

        // "customString": "customStringValue",
        JsonString customString = principal.getClaim("customString");
        Assertions.assertEquals(Json.createValue("customStringValue"), customString);
        // "customInteger": 123456789,
        JsonNumber customInteger = principal.getClaim("customInteger");
        Assertions.assertEquals(Json.createValue(123456789), customInteger);
        // "customDouble": 3.141592653589793,
        JsonNumber customDouble = principal.getClaim("customDouble");
        Assertions.assertEquals(Json.createValue(3.141592653589793), customDouble);

        /*
         * "customStringArray": ["value0", "value1", "value2" ],
         */
        JsonArray customStringArray = principal.getClaim("customStringArray");
        Assertions.assertEquals(3, customStringArray.size());
        Assertions.assertEquals(Json.createValue("value0"), customStringArray.getJsonString(0));
        Assertions.assertEquals(Json.createValue("value1"), customStringArray.getJsonString(1));
        Assertions.assertEquals(Json.createValue("value2"), customStringArray.getJsonString(2));
        /* "customIntegerArray": [0,1,2,3] */
        JsonArray customIntegerArray = principal.getClaim("customIntegerArray");
        Assertions.assertEquals(4, customIntegerArray.size());
        Assertions.assertEquals(Json.createValue(0), customIntegerArray.getJsonNumber(0));
        Assertions.assertEquals(Json.createValue(1), customIntegerArray.getJsonNumber(1));
        Assertions.assertEquals(Json.createValue(2), customIntegerArray.getJsonNumber(2));
        Assertions.assertEquals(Json.createValue(3), customIntegerArray.getJsonNumber(3));

        /*
         * "customObject": {
         * "my-service": {
         * "groups": [
         * "group1",
         * "group2"
         * ],
         * "roles": [
         * "role-in-my-service"
         * ]
         * },
         * "service-B": {
         * "roles": [
         * "role-in-B"
         * ]
         * },
         * "service-C": {
         * "groups": [
         * "groupC",
         * "web-tier"
         * ]
         * }
         * }
         */
        JsonObject customObject = principal.getClaim("customObject");
        String[] keys = { "my-service", "service-B", "service-C" };
        Assertions.assertEquals(new HashSet<>(Arrays.asList(keys)), customObject.keySet());
    }
}
