package io.quarkus.quickcli.deployment;

import jakarta.inject.Inject;

import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Option;

@Command(name = "injected", description = { "Tests CDI injection in commands" })
public class InjectedCommand implements Runnable {

    @Option(names = { "--greeting" }, description = "Greeting text", defaultValue = "Hi")
    String greeting;

    @Inject
    GreetingService greetingService;

    @Override
    public void run() {
        System.out.println(greetingService.greet(greeting));
    }
}
