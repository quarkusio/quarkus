package io.quarkus.it.mockbean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class StartupService {
    @Inject
    @RestClient
    GreetingRestClient greetingRestClient;

    @PostConstruct
    void onStart() {
        greetingRestClient.greeting();
    }

    public String greeting() {
        return greetingRestClient.greeting();
    }
}
