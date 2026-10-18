package io.quarkus.oidc.common.runtime;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class OidcClientRedirectException extends RuntimeException {

    private final String location;
    private final List<String> cookies;

    public OidcClientRedirectException(String location, List<String> setCookies) {
        this.location = location;
        this.cookies = getCookies(setCookies);
    }

    private static List<String> getCookies(List<String> setCookies) {
        if (setCookies != null && !setCookies.isEmpty()) {
            List<String> cookies = new ArrayList<>();
            for (String setCookie : setCookies) {
                int index = setCookie.indexOf(";");
                cookies.add(setCookie.substring(0, index));
            }
            return cookies;
        }
        return List.of();
    }

    public String getLocation() {
        return location;
    }

    public List<String> getCookies() {
        return cookies;
    }

}
