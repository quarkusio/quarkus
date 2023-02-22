package io.quarkus.it.resteasy.rest.client.classic;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = Constants.DEFAULT_TEST_URL)
public interface GreetClient extends GreetEndpoint {
}
