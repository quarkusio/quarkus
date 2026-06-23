package io.quarkus.quickcli.deployment;

import io.quarkus.quickcli.annotations.Command;

@Command(name = "goodbye", description = { "A goodbye command" })
public class GoodbyeCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Goodbye was requested!");
    }
}
