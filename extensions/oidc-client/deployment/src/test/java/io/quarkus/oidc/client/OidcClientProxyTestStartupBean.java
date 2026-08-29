package io.quarkus.oidc.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.oidc.client.runtime.TokensProducer;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class OidcClientProxyTestStartupBean {
    // Injecting TokensProducer forces its @PostConstruct init() -> initTokens() to run at startup.
    // With early token acquisition enabled and the OIDC server unavailable on startup, this would fail
    // the application startup unless early token acquisition is skipped for a DeferredOidcClient.
    void startup(@Observes StartupEvent event, TokensProducer tokensProducer) {
        tokensProducer.toString();
    }
}
