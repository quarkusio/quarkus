package io.quarkus.arc.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ReactiveTypeTest {

    @Test
    public void testUni() throws Exception {
        Method uniMethod = ReactiveInterface.class.getDeclaredMethod("uniCall");
        assertEquals(ReactiveType.UNI, ReactiveType.valueOf(uniMethod));
        assertTrue(ReactiveType.UNI.isReactive());
    }

    @Test
    public void testMulti() throws Exception {
        Method uniMethod = ReactiveInterface.class.getDeclaredMethod("multiCall");
        assertEquals(ReactiveType.MULTI, ReactiveType.valueOf(uniMethod));
        assertTrue(ReactiveType.MULTI.isReactive());
    }

    @Test
    public void testStage() throws Exception {
        Method uniMethod = ReactiveInterface.class.getDeclaredMethod("stageCall");
        assertEquals(ReactiveType.STAGE, ReactiveType.valueOf(uniMethod));
        assertTrue(ReactiveType.STAGE.isReactive());
    }

    @Test
    public void testFuture() throws Exception {
        Method uniMethod = ReactiveInterface.class.getDeclaredMethod("futureCall");
        assertEquals(ReactiveType.STAGE, ReactiveType.valueOf(uniMethod));
        assertTrue(ReactiveType.STAGE.isReactive());
    }

    @Test
    public void testObject() throws Exception {
        Method uniMethod = ReactiveInterface.class.getDeclaredMethod("objectCall");
        assertEquals(ReactiveType.NON_REACTIVE, ReactiveType.valueOf(uniMethod));
        assertFalse(ReactiveType.NON_REACTIVE.isReactive());
    }

    @Test
    public void testVoid() throws Exception {
        Method uniMethod = ReactiveInterface.class.getDeclaredMethod("voidCall");
        assertEquals(ReactiveType.NON_REACTIVE, ReactiveType.valueOf(uniMethod));
        assertFalse(ReactiveType.NON_REACTIVE.isReactive());
    }

    private static interface ReactiveInterface {
        Uni<?> uniCall();

        Multi<?> multiCall();

        CompletionStage<?> stageCall();

        CompletableFuture<?> futureCall();

        Object objectCall();

        void voidCall();
    }
}
