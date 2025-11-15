package io.quarkus.it.mockbean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigMappingTest {

    @InjectMock
    DummyMapping dummyMapping;

    @Test
    public void testGreet() {
        Assertions.assertNotNull(dummyMapping);
    }
}
