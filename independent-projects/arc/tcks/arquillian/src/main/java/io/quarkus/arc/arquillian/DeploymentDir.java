package io.quarkus.arc.arquillian;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class DeploymentDir {
    final Path root;

    final Path appClasses;
    final Path appLibraries;

    final Path generatedClasses;
    final Path generatedServices;

    DeploymentDir() throws IOException {
        this.root = Files.createTempDirectory("ArcArquillian");

        this.appClasses = Files.createDirectories(root.resolve("app").resolve("classes"));
        this.appLibraries = Files.createDirectories(root.resolve("app").resolve("libraries"));

        this.generatedClasses = Files.createDirectories(root.resolve("generated").resolve("classes"));
        this.generatedServices = Files.createDirectories(root.resolve("generated").resolve("services"));
    }
}
