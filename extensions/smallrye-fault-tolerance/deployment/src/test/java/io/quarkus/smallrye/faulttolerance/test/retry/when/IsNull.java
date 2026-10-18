package io.quarkus.smallrye.faulttolerance.test.retry.when;

import java.util.function.Predicate;

public class IsNull implements Predicate<Object> {
    @Override
    public boolean test(Object o) {
        return o == null;
    }
}
