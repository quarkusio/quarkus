package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class VoidFunction {

    public static final String TEST_EXCEPTION_MSG = "TEST_EXCEPTION_MSG";

    @Funq("void-function")
    public Uni<Void> voidFunction(boolean willThrow) {
        if (willThrow) {
            return Uni.createFrom().failure(new RuntimeException(TEST_EXCEPTION_MSG));
        } else {
            return Uni.createFrom().item((Void) null);
        }
    }
}
