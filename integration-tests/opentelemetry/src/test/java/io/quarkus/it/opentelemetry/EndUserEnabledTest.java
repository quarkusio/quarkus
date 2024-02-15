package io.quarkus.it.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Instance;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.SemanticAttributes;
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
        assertEquals("testUser", attributes.get(SemanticAttributes.ENDUSER_ID), attributes.toString());
        assertEquals("[admin, user]", attributes.get(SemanticAttributes.ENDUSER_ROLE), attributes.toString());
    }
}
