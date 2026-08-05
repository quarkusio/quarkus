package io.quarkus.vertx.http.cors;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.security.PathHandler;

/**
 * Verifies that application startup fails with a {@link ConfigurationException} when
 * {@code quarkus.http.cors.return-exact-origins=false} is combined with a wildcard origin
 * and a Quarkus security extension is present.
 */
public class CORSWildcardExactOriginSecurityFailTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.cors.enabled=true\n" +
            "quarkus.http.cors.origins=*\n" +
            "quarkus.http.cors.return-exact-origins=false\n" +
            "quarkus.http.auth.basic=true\n" +
            "quarkus.http.auth.policy.r1.roles-allowed=test\n" +
            "quarkus.http.auth.permission.roles1.paths=/test\n" +
            "quarkus.http.auth.permission.roles1.policy=r1\n";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestIdentityProvider.class, TestIdentityController.class, PathHandler.class)
                            .addAsResource(new StringAsset(APP_PROPS), "application.properties");
                }
            })
            .assertException(t -> {
                Assertions.assertTrue(t instanceof ConfigurationException,
                        "Expected ConfigurationException but got: " + t.getClass().getName());
                Assertions.assertTrue(t.getMessage().contains("return-exact-origins"),
                        "Exception message should mention return-exact-origins, got: " + t.getMessage());
            });

    @Test
    public void shouldFailAtStartup() {
        Assertions.fail("Application should not have started");
    }
}
