package org.acme;

/**
 * Deliberately has no implementation, so that any attempt to build the application fails during augmentation
 * with an unsatisfied dependency. That is how the tests in this project tell whether Quarkus tried to build an
 * application for a test class.
 */
public interface Missing {
}
