package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(name = "goodbye")
public class GoodbyeCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Goodbye was requested!");
    }
}
