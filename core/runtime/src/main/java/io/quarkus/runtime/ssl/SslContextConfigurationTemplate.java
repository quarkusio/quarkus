package io.quarkus.runtime.ssl;

import io.quarkus.runtime.annotations.Template;

@Template
public class SslContextConfigurationTemplate {

    public void setSslNativeEnabled(boolean sslNativeEnabled) {
        SslContextConfiguration.setSslNativeEnabled(sslNativeEnabled);
    }
}
