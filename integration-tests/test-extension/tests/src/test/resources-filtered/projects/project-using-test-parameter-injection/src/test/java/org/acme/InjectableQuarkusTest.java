package org.acme;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ExtendWith(MyParameterResolver.class)
public class InjectableQuarkusTest {

    @Test
    public void testParameterIsPassedIn(InjectableParameter param) {
        assertNotNull(param);
    }
}
