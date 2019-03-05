package org.acme;

import io.quarkus.clrunner.CommandLineRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;

import javax.inject.Inject;



public class ExampleRunner implements CommandLineRunner {

    @Inject
    Capitalizer capitalizer;

    @Override
    public void run(String[] args) {
        final List<String> lines = new ArrayList<>();
        for (int i=1; i<args.length; i++) {
            lines.add(capitalizer.perform(args[i]));
        }

        final String fileToWrite = args[0];

        final Path out = Paths.get(fileToWrite);

        try {
            Files.write(out,lines, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
