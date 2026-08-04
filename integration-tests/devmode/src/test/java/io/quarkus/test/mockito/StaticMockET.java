package io.quarkus.test.mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class StaticMockET {

    @Inject
    StaticMockService service;

    @Test
    void mockStaticMethod() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(date);
            assertEquals("2024-06-01-expected-value", service.value("expected-value"));
        }
    }

    @Test
    void callStaticMethod() {
        assertEquals(LocalDate.now() + "-expected-value", service.value("expected-value"));
    }
}
