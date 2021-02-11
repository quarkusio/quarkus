package io.quarkus.it.mockbean;

import javax.enterprise.context.ApplicationScoped;

/**
 * This gets removed removed by Arc since it's not being used in the application,
 * nor is it injected anywhere in tests
 */
@ApplicationScoped
public class OtherUnusedService {
}
