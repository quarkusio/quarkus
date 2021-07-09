package io.quarkus.test.junit.launcher;

import java.time.Duration;
import java.util.List;

class DefaultInitContextBase {
    private final int httpPort;
    private final int httpsPort;
    private final Duration waitTime;
    private final String testProfile;
    private final List<String> argLine;

    DefaultInitContextBase(int httpPort, int httpsPort, Duration waitTime, String testProfile, List<String> argLine) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.waitTime = waitTime;
        this.testProfile = testProfile;
        this.argLine = argLine;
    }

    public int httpPort() {
        return httpPort;
    }

    public int httpsPort() {
        return httpsPort;
    }

    public Duration waitTime() {
        return waitTime;
    }

    public String testProfile() {
        return testProfile;
    }

    public List<String> argLine() {
        return argLine;
    }
}
