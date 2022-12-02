package io.quarkus.deployment.dev;

import java.util.function.Predicate;

public class AlwaysFalsePredicate<T> implements Predicate<T> {

    @Override
    public boolean test(Object o) {
        return false;
    }
}
