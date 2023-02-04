package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * This would normally have been removed by Arc since it's not being used in the application,
 * but the fact that it's used with {@code @InjectMock} in a test prevents that from happening
 */
@ApplicationScoped
public class UnusedService {

    public String doSomething(String input) {
        return null;
    }
}
