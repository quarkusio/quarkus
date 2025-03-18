package io.quarkus.it.kafka;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class KafkaContextPropagationIT extends KafkaContextPropagationTest {

    @Override
    protected Matcher<String> assertBodyRequestScopedContextWasNotActive() {
        return Matchers.not(Matchers.blankOrNullString());
    }
}
