package io.quarkus.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.util.FormatterProfileReader;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;

@CommandLine.Command(name = "format", aliases = "fmt", header = "Format your source code", description = "%n"
        + "This command will format your sources using the same formatting rules used in the Quarkus Core project", footer = {
                "%n"
                        + "For example, the following command will format the current directory:" + "%n"
                        + "quarkus format" + "%n" })
public class Format implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        Properties formatterProperties;
        try (InputStream eclipseFormat = getClass().getClassLoader().getResourceAsStream("eclipse-format.xml")) {
            FormatterProfileReader reader = FormatterProfileReader.fromEclipseXml(eclipseFormat);
            formatterProperties = reader.getPropertiesFor("Quarkus");
        }
        Path root = Paths.get(".");
        // Resolve the sources directory
        Path src = root.resolve("src");
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    String source = Files.readString(file);
                    String formattedSource = Roaster.format(formatterProperties, source);
                    if (!formattedSource.equals(source)) {
                        output.info(root.resolve(file).toString());
                        Files.writeString(file, formattedSource);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return 0;
    }
}
