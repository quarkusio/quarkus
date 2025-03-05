package io.quarkus.restclient.config.key;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "shared")
public interface SharedThreeConfigKeyRestClient {
}
