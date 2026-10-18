package io.quarkus.spring.cloud.config.client.runtime;

import java.net.URI;

public interface ConfigServerBaseUrlProvider {
    URI get();
}
