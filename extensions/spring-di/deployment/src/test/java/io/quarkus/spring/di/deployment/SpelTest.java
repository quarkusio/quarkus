package io.quarkus.spring.di.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.quarkus.test.QuarkusUnitTest;

public class SpelTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SomeService.class))
            .assertException(e -> {
                assertEquals(IllegalArgumentException.class, e.getClass());
                assertTrue(e.getMessage().contains("#{'${values.list}'.split(',')}"));
            });

    @Test
    public void shouldNotBeInvoked() {
        // This method should not be invoked
        fail();
    }

    @Service
    public static class SomeService {

        @Value("#{'${values.list}'.split(',')}")
        List<String> fieldUsingList; // does not work
    }
}
