package io.quarkus.deployment.dev.testing;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

public final class DotNames {

    private DotNames() {
    }

    public static final DotName EXTEND_WITH = DotName.createSimple(ExtendWith.class.getName());
    public static final DotName REGISTER_EXTENSION = DotName.createSimple(RegisterExtension.class.getName());
    // TODO this leaks knowledge of the junit5 module into this module
    public static final DotName QUARKUS_TEST_EXTENSION = DotName.createSimple("io.quarkus.test.junit.QuarkusTestExtension");
}
