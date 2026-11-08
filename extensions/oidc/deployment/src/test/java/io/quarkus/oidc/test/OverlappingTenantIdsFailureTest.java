package io.quarkus.oidc.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The web-app tenants `tenant-a` and `tenant-a_1` can not coexist because the cookies of the tenant `tenant-a`
 * with a cookie suffix `1` would be resolved to the tenant `tenant-a_1`.
 */
public class OverlappingTenantIdsFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.oidc.tenant-a.auth-server-url=http://localhost/oidc\n"
                                    + "quarkus.oidc.tenant-a.discovery-enabled=false\n"
                                    + "quarkus.oidc.tenant-a.authorization-path=/authorize\n"
                                    + "quarkus.oidc.tenant-a.token-path=/token\n"
                                    + "quarkus.oidc.tenant-a.application-type=web-app\n"
                                    + "quarkus.oidc.tenant-a.client-id=my-client\n"
                                    + "quarkus.oidc.tenant-a_1.auth-server-url=http://localhost/oidc\n"
                                    + "quarkus.oidc.tenant-a_1.discovery-enabled=false\n"
                                    + "quarkus.oidc.tenant-a_1.authorization-path=/authorize\n"
                                    + "quarkus.oidc.tenant-a_1.token-path=/token\n"
                                    + "quarkus.oidc.tenant-a_1.application-type=web-app\n"
                                    + "quarkus.oidc.tenant-a_1.client-id=my-client\n"),
                            "application.properties"))
            .assertException(t -> {
                Throwable e = t;
                while (e != null && !ConfigurationException.class.getName().equals(e.getClass().getName())) {
                    e = e.getCause();
                }
                assertNotNull(e);
                assertTrue(e.getMessage().contains(
                        "Tenant id 'tenant-a_1' starts with the id of the tenant 'tenant-a' followed by '_'"), e.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

}
