package io.quarkus.reactivemessaging.http.runtime.config;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 27/09/2019
 */
public class WebsocketStreamConfig {
    public final String path;

    public WebsocketStreamConfig(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
