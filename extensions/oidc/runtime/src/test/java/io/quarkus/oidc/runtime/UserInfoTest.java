package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.UserInfo;

public class UserInfoTest {
    UserInfo userInfo = new UserInfo(
            "{"
                    + "\"name\": \"alice\","
                    + "\"admin\": true"
                    + "}");

    @Test
    public void testGetString() {
        assertEquals("alice", userInfo.getString("name"));
        assertNull(userInfo.getString("names"));
    }

    @Test
    public void testGetBoolean() {
        assertTrue(userInfo.getBoolean("admin"));
        assertNull(userInfo.getBoolean("admins"));
    }
}
