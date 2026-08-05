package io.quarkus.vertx.http.cors;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that application startup fails with a {@link ConfigurationException} when
 * {@code quarkus.http.cors.return-exact-origins=false} is combined with
 * {@code quarkus.http.cors.access-control-allow-credentials=true}.
 * The CORS specification does not allow credentials with a wildcard origin.
 */
public class CORSWildcardExactOriginCredentialsFailTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.cors.enabled=true\n" +
            "quarkus.http.cors.origins=*\n" +
            "quarkus.http.cors.return-exact-origins=false\n" +
            "quarkus.http.cors.access-control-allow-credentials=true\n";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(BeanRegisteringRoute.class)
                            .addAsResource(new StringAsset(APP_PROPS), "application.properties");
                }
            })
            .assertException(t -> {
                Assertions.assertTrue(t instanceof ConfigurationException,
                        "Expected ConfigurationException but got: " + t.getClass().getName());
                Assertions.assertTrue(t.getMessage().contains("return-exact-origins"),
                        "Exception message should mention return-exact-origins, got: " + t.getMessage());
                Assertions.assertTrue(t.getMessage().contains("credentials"),
                        "Exception message should mention credentials, got: " + t.getMessage());
            });

    @Test
    public void shouldFailAtStartup() {
        Assertions.fail("Application should not have started");
    }
}
