package io.quarkus.it.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Instance;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.quarkus.it.opentelemetry.util.EndUserProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(EndUserProfile.class)
public class EndUserEnabledTest extends AbstractEndUserTest {

    public EndUserEnabledTest() {
        super(Instance::isResolvable);
    }

    @Override
    protected void evaluateAttributes(Attributes attributes) {
        assertEquals(attributes.get(SemanticAttributes.ENDUSER_ID), "testUser");
        assertEquals(attributes.get(SemanticAttributes.ENDUSER_ROLE), "[admin, user]");
    }
}
