package io.quarkus.rest.client.reactive.runtime;

import java.util.Base64;

public class BasicAuthUtil {

    private BasicAuthUtil() {
    }

    public static String headerValue(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
