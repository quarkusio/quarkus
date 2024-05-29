package io.quarkus.deployment.dev.testing;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

final class DotNames {

    private DotNames() {
    }

    static final DotName EXTEND_WITH = DotName.createSimple(ExtendWith.class.getName());
    static final DotName REGISTER_EXTENSION = DotName.createSimple(RegisterExtension.class.getName());
    // TODO leaking of knowledge from JUnit5
    static final DotName QUARKUS_TEST_EXTENSION = DotName.createSimple("io.quarkus.test.junit.QuarkusTextExtension");
}
