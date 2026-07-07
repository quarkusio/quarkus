package org.acme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class RunLifecycle {

    void started(@Observes StartupEvent event) {
        write("build/application-child.pid", Long.toString(ProcessHandle.current().pid()));
        System.out.print("RUN-PARTIAL-PROMPT:\u001b[35mready\u001b[0m");
        System.out.flush();
        System.err.print("RUN-STDERR:\u001b[31mready\u001b[0m");
        System.err.flush();
    }

    void stopped(@Observes ShutdownEvent event) {
        write("build/application-stopped.txt", "stopped");
    }

    private static void write(String fileName, String value) {
        try {
            Path file = Path.of(fileName);
            Files.createDirectories(file.getParent());
            Files.writeString(file, value, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
