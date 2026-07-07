package io.quarkus.gradle.application.internal.dev;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Pattern;

final class QuarkusDevUiUrlParser {

    private static final String LISTENING_MARKER = "Listening on:";
    private static final String MANAGEMENT_MARKER = "Management interface listening on";
    // The startup line exposes listener authorities but not resolved HTTP routes. Keep this default-path bridge
    // isolated so a future typed runtime session-information contract can replace it without preserving log parsing.
    private static final String CONTINUOUS_TESTING_PATH = "/q/dev-ui/continuous-testing";
    private static final Pattern LISTENER_URL = Pattern
            .compile("https?://(?:\\[[^\\]\\s]+]|[A-Za-z0-9._~-]+):[0-9]+");

    private QuarkusDevUiUrlParser() {
    }

    static Optional<String> parse(String line) {
        int listeningOffset = line.indexOf(LISTENING_MARKER);
        if (listeningOffset < 0) {
            return Optional.empty();
        }
        String listeners = line.substring(listeningOffset + LISTENING_MARKER.length());
        int managementOffset = listeners.indexOf(MANAGEMENT_MARKER);
        if (managementOffset >= 0) {
            listeners = listeners.substring(0, managementOffset);
        }
        return parseListener(listeners);
    }

    private static Optional<String> parseListener(String listenerText) {
        var matcher = LISTENER_URL.matcher(listenerText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        URI listener;
        try {
            listener = URI.create(matcher.group());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String host = listener.getHost();
        if (host == null || listener.getPort() <= 0 || listener.getPort() > 65535) {
            return Optional.empty();
        }
        if (isWildcardAddress(host)) {
            host = "localhost";
        }
        try {
            return Optional.of(new URI(listener.getScheme(), null, host, listener.getPort(),
                    CONTINUOUS_TESTING_PATH, null, null).toString());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private static boolean isWildcardAddress(String host) {
        return host.equals("0.0.0.0")
                || host.equals("::")
                || host.equals("[::]")
                || host.equals("0:0:0:0:0:0:0:0")
                || host.equals("[0:0:0:0:0:0:0:0]");
    }
}
