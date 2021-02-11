package io.quarkus.vertx.http.runtime.security;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class ChallengeData {
    public final int status;
    public final CharSequence headerName;
    public final String headerContent;

    public ChallengeData(int status, CharSequence headerName, String headerContent) {
        this.status = status;
        this.headerName = headerName;
        this.headerContent = headerContent;
    }
}
