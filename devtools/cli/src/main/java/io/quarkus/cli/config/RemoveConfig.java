package io.quarkus.cli.config;

import static io.quarkus.devtools.messagewriter.MessageIcons.SUCCESS_ICON;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.quickcli.ExitCode;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Parameters;
import io.smallrye.config.ConfigValue;

@Command(name = "remove", header = "Removes a configuration from application.properties")
public class RemoveConfig extends BaseConfigCommand implements Callable<Integer> {
    @Parameters(index = "0", arity = "1", paramLabel = "NAME", description = "Configuration name")
    String name;

    @Override
    public Integer call() throws Exception {
        Path properties = projectRoot().resolve("src/main/resources/application.properties");
        if (!properties.toFile().exists()) {
            output.error("Could not find an application.properties file");
            return -1;
        }

        List<String> lines = Files.readAllLines(properties);

        ConfigValue configValue = findKey(lines, name);
        if (configValue.getLineNumber() != -1) {
            output.info(SUCCESS_ICON + " Removing configuration @|bold " + name + "|@");
            lines.remove(configValue.getLineNumber());
        } else {
            output.error("Could not find configuration " + name);
            return -1;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(properties)) {
            for (String i : lines) {
                writer.write(i);
                writer.newLine();
            }
        }

        return ExitCode.OK;
    }
}
