package io.quarkus.it.resteasy.reactive.elytron;

import java.util.Map;

final class Users {

    private Users() {
    }

    private static final Map<String, String> CREDENTIALS = Map.of("john", "john", "mary", "mary", "poul",
            "poul");

    static String password(String user) {
        String password = CREDENTIALS.get(user);
        if (password == null) {
            throw new IllegalArgumentException("Unknown user: '" + user + "'");
        }
        return password;
    }
}
