package io.quarkus.it.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.enterprise.inject.Instance;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class EndUserDisabledTest extends AbstractEndUserTest {

    public EndUserDisabledTest() {
        super(Instance::isUnsatisfied);
    }

    @Override
    protected void evaluateAttributes(Attributes attributes) {
        assertNull(attributes.get(SemanticAttributes.ENDUSER_ID));
        assertNull(attributes.get(SemanticAttributes.ENDUSER_ROLE));
    }
}
