package io.quarkus.jwt.test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonObject;

import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.security.authz.Attributes;

import io.quarkus.smallrye.jwt.runtime.auth.ClaimAttributes;

public class ClaimAttributesUnitTest {
    @Test
    public void testGroups() throws Exception {
        InputStream is = getClass().getResourceAsStream("/Token1.json");
        JsonObject content = Json.createReader(is).readObject();
        JwtClaims jwtClaims = JwtClaims.parse(content.toString());
        ClaimAttributes claimAttributes = new ClaimAttributes(jwtClaims);
        Attributes.Entry groups = claimAttributes.get("groups");
        Assertions.assertNotNull(groups);
        Assertions.assertEquals(4, groups.size());
        HashSet<String> groupsSet = new HashSet<>(groups);
        String[] expectedGroups = { "Echoer",
                "Tester",
                "group1",
                "group2" };
        Assertions.assertEquals(new HashSet<String>(Arrays.asList(expectedGroups)), groupsSet);
    }

    @Test
    public void testAll() throws Exception {
        InputStream is = getClass().getResourceAsStream("/Token1.json");
        JsonObject content = Json.createReader(is).readObject();
        JwtClaims jwtClaims = JwtClaims.parse(content.toString());
        ClaimAttributes claimAttributes = new ClaimAttributes(jwtClaims);
        Attributes.Entry groups = claimAttributes.get("groups");
        Assertions.assertNotNull(groups);
        Assertions.assertEquals(4, groups.size());
        HashSet<String> groupsSet = new HashSet<>(groups);
        String[] expectedGroups = { "Echoer",
                "Tester",
                "group1",
                "group2" };
        Assertions.assertEquals(new HashSet<String>(Arrays.asList(expectedGroups)), groupsSet);

        String upn = claimAttributes.getFirst("upn");
        Assertions.assertEquals("jdoe@example.com", upn);
        String sub = claimAttributes.getLast("sub");
        Assertions.assertEquals("24400320", sub);
        String preferred_username = claimAttributes.getFirst("preferred_username");
        Assertions.assertEquals("jdoe", preferred_username);

        String iss = claimAttributes.getFirst("iss");
        Assertions.assertEquals("https://server.example.com", iss);
        String jti = claimAttributes.getFirst("jti");
        Assertions.assertEquals("a-123", jti);
        String aud = claimAttributes.getFirst("aud");
        Assertions.assertEquals("s6BhdRkqt3", aud);
        String exp = claimAttributes.getFirst("exp");
        Assertions.assertEquals("1311281970", exp);
        String iat = claimAttributes.getFirst("iat");
        Assertions.assertEquals("1311280970", iat);
        String auth_time = claimAttributes.getFirst("auth_time");
        Assertions.assertEquals("1311280969", auth_time);

        String customString = claimAttributes.getFirst("customString");
        Assertions.assertEquals("customStringValue", customString);
        String customInteger = claimAttributes.getFirst("customInteger");
        Assertions.assertEquals("123456789", customInteger);
        String customDouble = claimAttributes.getFirst("customDouble");
        Assertions.assertEquals("3.141592653589793", customDouble);

        /*
         * "customDoubleArray": [0.1, 1.1, 2.2, 3.3, 4.4],
         */
        Assertions.assertEquals(5, claimAttributes.size("customDoubleArray"));
        Attributes.Entry customDoubleArray = claimAttributes.get("customDoubleArray");
        Assertions.assertEquals(5, customDoubleArray.size());
        Assertions.assertEquals("0.1", customDoubleArray.get(0));
        Assertions.assertEquals("0.1", claimAttributes.getFirst("customDoubleArray"));
        Assertions.assertEquals("1.1", customDoubleArray.get(1));
        Assertions.assertEquals("2.2", customDoubleArray.get(2));
        Assertions.assertEquals("3.3", customDoubleArray.get(3));
        Assertions.assertEquals("4.4", customDoubleArray.get(4));
        Assertions.assertEquals("4.4", claimAttributes.getLast("customDoubleArray"));

        Attributes.Entry customStringArray = claimAttributes.get("customStringArray");
        Assertions.assertEquals(3, customStringArray.size());
        Assertions.assertEquals("value0", customStringArray.get(0));
        Assertions.assertEquals("value1", customStringArray.get(1));
        Assertions.assertEquals("value2", customStringArray.get(2));
        /* "customIntegerArray": [0,1,2,3] */
        Attributes.Entry customIntegerArray = claimAttributes.get("customIntegerArray");
        Assertions.assertEquals(4, customIntegerArray.size());
        Assertions.assertEquals("0", customIntegerArray.get(0));
        Assertions.assertEquals("1", customIntegerArray.get(1));
        Assertions.assertEquals("2", customIntegerArray.get(2));
        Assertions.assertEquals("3", customIntegerArray.get(3));

    }
}
