package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerClassSpyTest {

    @InjectSpy
    IdentityService identityService;

    @Test
    @Order(1)
    void testWithSpy() {
        when(identityService.call(any())).thenReturn("DUMMY");
        assertEquals("DUMMY", identityService.call("foo"));
    }

    @Test
    @Order(2)
    void testWithoutSpy() {
        assertEquals("foo", identityService.call("foo"));
    }

    @ApplicationScoped
    public static class IdentityService {

        public String call(String input) {
            return input;
        }
    }
}
