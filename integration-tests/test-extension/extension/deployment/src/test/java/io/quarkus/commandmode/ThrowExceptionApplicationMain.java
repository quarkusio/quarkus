package io.quarkus.commandmode;

import java.util.function.BiConsumer;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class ThrowExceptionApplicationMain {

    public static void main(String... args) {
        Quarkus.run(ThrowExceptionApplication.class, new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer exitCode, Throwable cause) {
                System.out.println("Exception and exit code [" + exitCode + "] handled by application");
                System.exit(10);
            }
        });
    }
}
