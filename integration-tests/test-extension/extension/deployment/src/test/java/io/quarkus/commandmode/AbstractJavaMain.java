package io.quarkus.commandmode;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public abstract class AbstractJavaMain {

    public static void main(String... args) {
        Quarkus.run(HelloWorldNonDefault.class, args);
    }
}
