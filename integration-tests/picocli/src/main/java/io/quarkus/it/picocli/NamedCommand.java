package io.quarkus.it.picocli;

import jakarta.inject.Named;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@Named("PicocliEntry")
@CommandLine.Command
public class NamedCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("NamedCommand called!");
    }
}
