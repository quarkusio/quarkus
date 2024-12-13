package io.quarkus.restclient.config.simple.one;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "one")
public interface SimpleNameRestClient {
}
