package io.quarkus.spring.cloud.config.client.runtime;

import java.net.URI;

public record ConfigServerUrl(URI baseURI, int port, String host, String completeURLString) {

}
