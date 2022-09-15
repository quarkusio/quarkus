package io.quarkus.it.main;

import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MockitoTest {

    @Test
    public void testMockHttpServletRequest() {
        assertNoExceptionThrown(HttpServletRequest.class);
    }

    @Test
    public void testMockApplicationClass() {
        assertNoExceptionThrown(Whatever.class);
    }

    private void assertNoExceptionThrown(Class<?> classToMock) {
        Assertions.assertThatCode(() -> mock(classToMock)).doesNotThrowAnyException();
    }

    public static class Whatever {

    }
}
