package io.quarkus.runtime.ssl;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SslContextConfigurationRecorder {

    public void setSslNativeEnabled(boolean sslNativeEnabled) {
        SslContextConfiguration.setSslNativeEnabled(sslNativeEnabled);
    }
}
