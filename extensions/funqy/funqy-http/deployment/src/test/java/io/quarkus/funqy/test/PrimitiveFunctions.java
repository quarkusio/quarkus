package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class PrimitiveFunctions {

    public static final String TEST_EXCEPTION_MSG = "TEST_EXCEPTION_MSG";

    @Funq
    public String toLowerCase(String val) {
        return val.toLowerCase();
    }

    @Funq
    public Uni<String> toLowerCaseAsync(String val) {
        return Uni.createFrom().item(() -> val.toLowerCase());
    }

    @Funq
    public int doubleIt(int val) {
        return val * 2;
    }

    @Funq
    public String get() {
        return "get";
    }

    @Funq
    public void noop() {
    }

    @Funq
    public Uni<Void> noopAsync() {
        return Uni.createFrom().item(() -> (Void) null);
    }

    @Funq
    public Uni<Integer> doubleItAsync(int val) {
        return Uni.createFrom().item(() -> val * 2);
    }

    @Funq
    public Uni<Void> voidFunThrowError() {
        return Uni.createFrom().failure(new RuntimeException(TEST_EXCEPTION_MSG));
    }

}
