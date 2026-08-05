package io.quarkus.resteasy.reactive.server.test.security.inheritance;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DefaultJaxRsDenyAllImplMethodSecuredTest extends AbstractImplMethodSecuredTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = getRunner("quarkus.security.jaxrs.deny-unannotated-endpoints=true");

    @Override
    protected boolean denyAllUnannotated() {
        return true;
    }
}
