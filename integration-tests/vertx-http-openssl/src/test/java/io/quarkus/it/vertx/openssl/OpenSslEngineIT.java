package io.quarkus.it.vertx.openssl;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Runs {@link OpenSslEngineTest} against the native executable. The executable is built with netty-tcnative on the
 * classpath; the machine running it needs {@code libssl.so.3} (3.5+) and {@code libapr-1}.
 */
@QuarkusIntegrationTest
public class OpenSslEngineIT extends OpenSslEngineTest {
}
