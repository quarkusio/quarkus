package io.quarkus.runtime.test;

import java.util.function.Predicate;

/**
 * This predicate can be used to distinguish application classes in the test mode.
 */
public interface TestApplicationClassPredicate extends Predicate<String> {

}
