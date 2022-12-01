package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command
public class HelloCommand implements Runnable {

    @CommandLine.Option(names = { "-n", "--name" }, defaultValue = "World", description = "Who we will greet?")
    String name;

    @Override
    public void run() {
        System.out.println("Hello " + name + "!");
    }
}
