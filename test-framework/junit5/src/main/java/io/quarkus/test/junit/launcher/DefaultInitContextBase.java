package io.quarkus.test.junit.launcher;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.test.common.ArtifactLauncher;

class DefaultInitContextBase {
    private final int httpPort;
    private final int httpsPort;
    private final Duration waitTime;
    private final String testProfile;
    private final List<String> argLine;

    private final Map<String, String> env;
    private final ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult;

    DefaultInitContextBase(int httpPort, int httpsPort, Duration waitTime, String testProfile, List<String> argLine,
            Map<String, String> env, ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.waitTime = waitTime;
        this.testProfile = testProfile;
        this.argLine = argLine;
        this.env = env;
        this.devServicesLaunchResult = devServicesLaunchResult;
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

    public Map<String, String> env() {
        return env;
    }

    public ArtifactLauncher.InitContext.DevServicesLaunchResult getDevServicesLaunchResult() {
        return devServicesLaunchResult;
    }
}
