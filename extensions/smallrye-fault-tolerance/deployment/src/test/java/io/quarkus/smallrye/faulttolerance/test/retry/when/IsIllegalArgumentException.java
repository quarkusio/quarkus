package io.quarkus.smallrye.faulttolerance.test.retry.when;

import java.util.function.Predicate;

public class IsIllegalArgumentException implements Predicate<Throwable> {
    @Override
    public boolean test(Throwable throwable) {
        return throwable instanceof IllegalArgumentException;
    }
}
