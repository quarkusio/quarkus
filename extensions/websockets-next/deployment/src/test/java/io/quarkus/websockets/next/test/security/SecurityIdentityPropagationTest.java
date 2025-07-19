package io.quarkus.websockets.next.test.security;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SecurityIdentityPropagationTest extends AbstractSecurityIdentityPropagationTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = getQuarkusUnitTest("");

}
