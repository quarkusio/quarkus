package io.quarkus.test.junit.launcher;

import java.time.Duration;

class DefaultInitContextBase {
    private final int httpPort;
    private final int httpsPort;
    private final Duration waitTime;
    private final String testProfile;

    DefaultInitContextBase(int httpPort, int httpsPort, Duration waitTime, String testProfile) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.waitTime = waitTime;
        this.testProfile = testProfile;
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
}
