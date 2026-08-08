package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusExtensionTest;

public class LargeMessageBundleTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(LargeMessageBundle.class));

    @Inject
    Engine engine;

    @Test
    void resolveMessageFromSecondResolverGroup() {
        assertEquals("message-300", engine.parse("{large:message300}").render());
    }

}
