package io.quarkus.restclient.config;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "mp.key")
interface MPConfigKeyRestClient {
}
