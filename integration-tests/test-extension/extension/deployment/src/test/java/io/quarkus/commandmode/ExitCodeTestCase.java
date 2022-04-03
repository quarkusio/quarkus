package io.quarkus.commandmode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Application;
import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.test.QuarkusUnitTest;

public class ExitCodeTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    public void testReturnedExitCode() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        ApplicationLifecycleManager.run(Application.currentApplication(), ExitCodeApplication.class,
                new BiConsumer<Integer, Throwable>() {
                    @Override
                    public void accept(Integer integer, Throwable cause) {
                        future.complete(integer);
                    }
                }, "5");
        Assertions.assertEquals(5, future.get());
    }

    @Test
    public void testWaitToExitWithCode() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ApplicationLifecycleManager.run(Application.currentApplication(), WaitToExitApplication.class,
                        new BiConsumer<Integer, Throwable>() {
                            @Override
                            public void accept(Integer integer, Throwable cause) {
                                future.complete(integer);
                            }
                        });
            }
        }).start();
        Thread.sleep(500);
        Assertions.assertFalse(future.isDone());
        Quarkus.asyncExit(10);
        Assertions.assertEquals(10, future.get());
    }

    @Test
    public void testWaitToExitWithNoCode() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ApplicationLifecycleManager.run(Application.currentApplication(), WaitToExitApplication.class,
                        new BiConsumer<Integer, Throwable>() {
                            @Override
                            public void accept(Integer integer, Throwable cause) {
                                future.complete(integer);
                            }
                        });
            }
        }).start();
        Thread.sleep(500);
        Assertions.assertFalse(future.isDone());
        Quarkus.asyncExit();
        Assertions.assertEquals(1, future.get());
    }

    public static class ExitCodeApplication implements QuarkusApplication {

        @Override
        public int run(String... args) throws Exception {
            return Integer.parseInt(args[0]);
        }
    }

    public static class WaitToExitApplication implements QuarkusApplication {

        @Override
        public int run(String... args) throws Exception {
            Quarkus.waitForExit();
            return 1;
        }
    }

}
