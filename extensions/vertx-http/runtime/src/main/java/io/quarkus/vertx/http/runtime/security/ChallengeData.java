package io.quarkus.vertx.http.runtime.security;

import java.util.Map;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class ChallengeData {
    public final int status;
    private final Map<CharSequence, String> headers;

    public ChallengeData(int status) {
        this(status, null);
    }

    public ChallengeData(int status, CharSequence headerName, String headerContent) {
        this.status = status;
        this.headers = createHeaders(headerName, headerContent);
    }

    public ChallengeData(int status, Map<CharSequence, String> headers) {
        this.status = status;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public Map<CharSequence, String> getHeaders() {
        return headers;
    }

    private static Map<CharSequence, String> createHeaders(CharSequence headerName, String headerContent) {
        if (headerName == null) {
            return Map.of();
        }
        return Map.of(headerName, headerContent == null ? "" : headerContent);
    }
}
