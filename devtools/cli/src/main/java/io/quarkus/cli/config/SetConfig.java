package io.quarkus.cli.config;

import static io.quarkus.devtools.messagewriter.MessageIcons.SUCCESS_ICON;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "set", header = "Sets a configuration in application.properties")
public class SetConfig extends BaseConfigCommand implements Callable<Integer> {
    @Parameters(index = "0", arity = "1", paramLabel = "NAME", description = "Configuration name")
    String name;
    @Parameters(index = "1", arity = "0..1", paramLabel = "VALUE", description = "Configuration value")
    String value;
    @Option(names = { "-k", "--encrypt" }, description = "Encrypt the configuration value")
    boolean encrypt;

    @Override
    public Integer call() throws Exception {
        Path properties = projectRoot().resolve("src/main/resources/application.properties");
        if (!properties.toFile().exists()) {
            output.warn("Could not find an application.properties file, creating one now!");
            Path resources = projectRoot().resolve("src/main/resources");
            Files.createDirectories(resources);
            Files.createFile(resources.resolve("application.properties"));
            output.info(SUCCESS_ICON + " @|bold application.properties|@ file created in @|bold src/main/resources|@");
        }

        List<String> lines = Files.readAllLines(properties);

        if (encrypt) {
            Encrypt encrypt = new Encrypt();
            List<String> args = new ArrayList<>();
            args.add("-q");
            if (value == null) {
                value = findKey(lines, name).getValue();
            }
            args.add(value);
            if (value == null || value.length() == 0) {
                output.error("Cannot encrypt an empty value");
                return -1;
            }

            ConfigValue encryptionKey = findKey(lines, "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key");
            if (encryptionKey.getValue() != null) {
                args.add("--key=" + encryptionKey.getValue());
            }
            ConfigValue encryptionDecode = findKey(lines,
                    "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key-decode");
            if (encryptionDecode.getValue() == null
                    || !Converters.getImplicitConverter(Boolean.class).convert(encryptionDecode.getValue())) {
                args.add("--format=plain");
            }

            int execute = new CommandLine(encrypt).execute(args.toArray(new String[] {}));
            if (execute < 0) {
                System.exit(execute);
            }
            value = "${aes-gcm-nopadding::" + encrypt.getEncryptedSecret() + "}";
            if (encryptionKey.getValue() == null) {
                lines.add(encryptionKey.getName() + "=" + encrypt.getEncryptionKey());
            }
        }

        ConfigValue configValue = findKey(lines, name);
        String actualValue = value != null ? value : "empty value";
        if (configValue.getLineNumber() != -1) {
            output.info(SUCCESS_ICON + " Setting configuration @|bold " + name + "|@ to value @|bold " + actualValue + "|@");
            lines.set(configValue.getLineNumber(), name + "=" + (value != null ? value : ""));
        } else {
            output.info(SUCCESS_ICON + " Adding configuration @|bold " + name + "|@ with value @|bold " + actualValue + "|@");
            lines.add(name + "=" + (value != null ? value : ""));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(properties)) {
            for (String i : lines) {
                writer.write(i);
                writer.newLine();
            }
        }

        return CommandLine.ExitCode.OK;
    }
}
