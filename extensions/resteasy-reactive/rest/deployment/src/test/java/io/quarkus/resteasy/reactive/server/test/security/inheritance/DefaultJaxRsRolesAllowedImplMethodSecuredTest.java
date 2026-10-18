package io.quarkus.resteasy.reactive.server.test.security.inheritance;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DefaultJaxRsRolesAllowedImplMethodSecuredTest extends AbstractImplMethodSecuredTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = getRunner("quarkus.security.jaxrs.default-roles-allowed=admin");

    @Override
    protected String roleRequiredForUnannotatedEndpoint() {
        return "admin";
    }
}
