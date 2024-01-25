package io.quarkus.cli.config;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.smallrye.config.ConfigValue;
import picocli.CommandLine;

@CommandLine.Command(name = "set")
public class SetConfig extends BaseConfigCommand implements Callable<Integer> {
    @CommandLine.Option(required = true, names = { "-n", "--name" }, description = "Configuration name")
    String name;
    @CommandLine.Option(names = { "-a", "--value" }, description = "Configuration value")
    String value;
    @CommandLine.Option(names = { "-k", "--encrypt" }, description = "Encrypt value")
    boolean encrypt;

    @Override
    public Integer call() throws Exception {
        Path properties = projectRoot().resolve("src/main/resources/application.properties");
        if (!properties.toFile().exists()) {
            System.out.println("Could not find an application.properties file");
            return 0;
        }

        List<String> lines = Files.readAllLines(properties);

        if (encrypt) {
            Encrypt encrypt = new Encrypt();
            List<String> args = new ArrayList<>();
            args.add("-q");
            if (value == null) {
                value = findKey(lines, name).getValue();
            }
            args.add("--secret=" + value);
            if (value == null || value.length() == 0) {
                System.out.println("Cannot encrypt an empty value");
                return -1;
            }

            ConfigValue encryptionKey = findKey(lines, "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key");
            if (encryptionKey.getValue() != null) {
                args.add("--key=" + encryptionKey.getValue());
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

        int nameLineNumber = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(name + "=")) {
                nameLineNumber = i;
                break;
            }
        }

        if (nameLineNumber != -1) {
            if (value != null) {
                System.out.println("Setting " + name + " to " + value);
                lines.set(nameLineNumber, name + "=" + value);
            } else {
                System.out.println("Removing " + name);
                lines.remove(nameLineNumber);
            }
        } else {
            System.out.println("Adding " + name + " with " + value);
            lines.add(name + "=" + (value != null ? value : ""));
        }

        try (BufferedWriter writer = Files.newBufferedWriter(properties)) {
            for (String i : lines) {
                writer.write(i);
                writer.newLine();
            }
        }

        return 0;
    }

    public static ConfigValue findKey(List<String> lines, String name) {
        ConfigValue configValue = ConfigValue.builder().withName(name).build();
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (line.startsWith(configValue.getName() + "=")) {
                return configValue.withValue(line.substring(name.length() + 1)).withLineNumber(i);
            }
        }
        return configValue;
    }
}
