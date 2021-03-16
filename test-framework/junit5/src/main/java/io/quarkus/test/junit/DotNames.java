package io.quarkus.test.junit;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

final class DotNames {

    private DotNames() {
    }

    static final DotName EXTEND_WITH = DotName.createSimple(ExtendWith.class.getName());
    static final DotName REGISTER_EXTENSION = DotName.createSimple(RegisterExtension.class.getName());
    static final DotName QUARKUS_TEST_EXTENSION = DotName.createSimple(QuarkusTestExtension.class.getName());
}
