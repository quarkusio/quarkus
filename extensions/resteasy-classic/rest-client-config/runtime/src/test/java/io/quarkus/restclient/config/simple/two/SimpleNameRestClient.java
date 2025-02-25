package io.quarkus.restclient.config.simple.two;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "two")
public interface SimpleNameRestClient {
}
