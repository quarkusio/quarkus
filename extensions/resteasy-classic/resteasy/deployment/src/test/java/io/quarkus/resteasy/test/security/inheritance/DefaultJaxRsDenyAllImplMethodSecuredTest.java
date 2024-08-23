package io.quarkus.resteasy.test.security.inheritance;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DefaultJaxRsDenyAllImplMethodSecuredTest extends AbstractImplMethodSecuredTest {

    @RegisterExtension
    static QuarkusUnitTest runner = getRunner("quarkus.security.jaxrs.deny-unannotated-endpoints=true");

    @Override
    protected boolean denyAllUnannotated() {
        return true;
    }
}
