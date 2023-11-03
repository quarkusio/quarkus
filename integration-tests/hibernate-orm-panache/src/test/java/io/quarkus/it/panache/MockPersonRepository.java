package io.quarkus.it.panache;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * An alternate implementation of {@link PersonRepository} to demonstrate that Repository entity type can be retrieved
 * even when the class does not directly implement PanacheRepository interface.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class MockPersonRepository extends PersonRepository {
}
