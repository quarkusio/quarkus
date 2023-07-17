package io.quarkus.it.picocli;

import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "system-property-command")
public class SystemPropertyCommand implements Callable<Integer> {

    public static final String PROPERTY_NAME = "test.property";

    @Override
    public Integer call() {
        System.out.println(System.getProperty(PROPERTY_NAME));
        return 0;
    }
}
