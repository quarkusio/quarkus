package io.quarkus.test.mockito;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StaticMockService {

    public String value(String suffix) {
        return LocalDate.now() + "-" + suffix;
    }
}
