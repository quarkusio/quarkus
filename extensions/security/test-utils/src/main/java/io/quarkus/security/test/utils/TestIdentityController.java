package io.quarkus.security.test.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TestIdentityController {

    public static final Map<String, TestIdentity> identities = new ConcurrentHashMap<>();

    public static Builder resetRoles() {
        identities.clear();
        return new Builder();
    }

    public static class Builder {
        public Builder add(String username, String password, String... roles) {
            identities.put(username, new TestIdentity(username, password, roles));
            return this;
        }
    }

    public static final class TestIdentity {

        public final String username;
        public final String password;
        public final Set<String> roles;

        private TestIdentity(String username, String password, String... roles) {
            this.username = username;
            this.password = password;
            this.roles = new HashSet<>(Arrays.asList(roles));
        }
    }
}
