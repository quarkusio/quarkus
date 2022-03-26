package io.quarkus.commandmode;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class PropertyInjectingMain implements QuarkusApplication {

    @ConfigProperty(name = "test.message")
    String message;

    @Override
    public int run(String... args) {
        System.out.println("test.message = " + message);
        return 0;
    }
}
