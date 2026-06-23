package io.quarkus.quickcli.deployment;

import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Option;

@Command(name = "hello", description = { "A hello command" })
public class HelloCommand implements Runnable {

    @Option(names = { "--name" }, description = "Name to greet", defaultValue = "World")
    String name;

    @Override
    public void run() {
        System.out.println("Hello " + name + "!");
    }
}
