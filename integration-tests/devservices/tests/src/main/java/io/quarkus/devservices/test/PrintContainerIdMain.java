package io.quarkus.devservices.test;

import static io.quarkus.tests.simpleextension.Constants.SIMPLE_EXTENSION_CONTAINER_ID;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/*
 * Not the default main class, so that it is only run by the tests that explicitly select it.
 */
@QuarkusMain(name = "print-container-id")
public class PrintContainerIdMain implements QuarkusApplication {

    public static final String PREFIX = "container-id=";

    @Override
    public int run(String... args) {
        System.out.println(PREFIX + ConfigProvider.getConfig().getValue(SIMPLE_EXTENSION_CONTAINER_ID, String.class));
        return 0;
    }
}
