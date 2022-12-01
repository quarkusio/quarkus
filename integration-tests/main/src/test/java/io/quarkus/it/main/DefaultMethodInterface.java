package io.quarkus.it.main;

import org.junit.jupiter.api.Test;

public interface DefaultMethodInterface {

    @Test
    default void doTest() {
    }
}
