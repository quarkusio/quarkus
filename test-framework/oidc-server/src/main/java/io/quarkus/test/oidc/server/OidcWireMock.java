package io.quarkus.test.oidc.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Used to specify that the field should be injected with the {@link WireMockServer}
 * server that provides mock HTTP API for OIDC clients.
 * <p>
 * Note: for this injection to work the test must use {@link OidcWiremockTestResource}.
 * </p>
 * <p>
 * The main purpose of injecting the {@link WireMockServer} is for tests to be able
 * to mock extra URLs not covered by {@link OidcWiremockTestResource}.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OidcWireMock {
}
