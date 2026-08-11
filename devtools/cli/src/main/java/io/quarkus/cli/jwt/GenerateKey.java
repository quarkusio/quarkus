package io.quarkus.cli.jwt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.quarkus.cli.BaseBuildCommand;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(name = "generate-key", sortOptions = false, header = "Generate RSA or EC public/private key pair for JWT.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "Options:%n")
public class GenerateKey extends BaseBuildCommand implements Callable<Integer> {

    @Option(names = { "-f", "--force" }, description = "Overwrite existing private/public keys")
    boolean force;

    @Option(names = { "-s",
            "--size" }, description = "Key size (Defaults to 2048 for RSA, 256 for EC. EC supports 256, 384, or 521)")
    int size;

    @Option(names = { "-a", "--algo" }, description = "Key algorithm: RSA or EC (Defaults to RSA)")
    String algo;

    @Override
    public Integer call() throws Exception {
        if (algo != null) {
            if (algo.equalsIgnoreCase("RSA")) {
                algo = "RSA";
            } else if (algo.equalsIgnoreCase("EC")) {
                algo = "EC";
            } else {
                throw new RuntimeException("Algorithm not supported: " + algo);
            }
        } else {
            algo = "RSA";
        }
        if (algo.equals("RSA")) {
            if (size == 0) {
                size = 2048;
            }
        } else {
            if (size == 0) {
                size = 256;
            }
        }

        Path resourcesFolder = projectRoot().resolve("src/main/resources");
        if (!Files.exists(resourcesFolder))
            Files.createDirectories(resourcesFolder);
        Path privateKey = resourcesFolder.resolve("privateKey.pem");
        Path publicKey = resourcesFolder.resolve("publicKey.pem");

        if (!force) {
            Path properties = projectRoot().resolve("src/main/resources/application.properties");
            if (Files.exists(properties)) {
                List<String> existingLines = Files.readAllLines(properties);
                Map<String, String> expectedValues = Map.of(
                        "mp.jwt.verify.publickey.location", "publicKey.pem",
                        "mp.jwt.decrypt.key.location", "privateKey.pem",
                        "smallrye.jwt.sign.key.location", "privateKey.pem",
                        "smallrye.jwt.encrypt.key.location", "publicKey.pem");
                for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
                    String existing = getProperty(existingLines, entry.getKey());
                    if (existing != null && !existing.equals(entry.getValue())) {
                        output.error(
                                "Configuration %s already points to %s (expected %s). Use --force to overwrite.",
                                entry.getKey(), existing, entry.getValue());
                        return CommandLine.ExitCode.USAGE;
                    }
                }
            }
        }

        if (force || (!Files.exists(privateKey) && !Files.exists(publicKey))) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algo);
            kpg.initialize(size);
            KeyPair kp = kpg.generateKeyPair();

            try (FileWriter fw = new FileWriter(privateKey.toFile())) {
                fw.append("-----BEGIN PRIVATE KEY-----\n");
                fw.append(Base64.getMimeEncoder().encodeToString(kp.getPrivate().getEncoded()));
                fw.append("\n");
                fw.append("-----END PRIVATE KEY-----\n");
            }
            try (FileWriter fw = new FileWriter(publicKey.toFile())) {
                fw.append("-----BEGIN PUBLIC KEY-----\n");
                fw.append(Base64.getMimeEncoder().encodeToString(kp.getPublic().getEncoded()));
                fw.append("\n");
                fw.append("-----END PUBLIC KEY-----\n");
            }
            output.info("Public and private keys created in %s and %s", publicKey, privateKey);

            Map<String, String> config = new LinkedHashMap<>();
            config.put("mp.jwt.verify.publickey.location", "publicKey.pem");
            config.put("mp.jwt.decrypt.key.location", "privateKey.pem");
            config.put("smallrye.jwt.sign.key.location", "privateKey.pem");
            config.put("smallrye.jwt.encrypt.key.location", "publicKey.pem");

            Path properties = projectRoot().resolve("src/main/resources/application.properties");
            if (!Files.exists(properties)) {
                Files.createFile(properties);
            }
            List<String> lines = new ArrayList<>(Files.readAllLines(properties));
            for (Map.Entry<String, String> entry : config.entrySet()) {
                setProperty(lines, entry.getKey(), entry.getValue());
            }
            appendToProperty(lines, "quarkus.native.resources.includes", "publicKey.pem", "privateKey.pem");
            try (BufferedWriter writer = Files.newBufferedWriter(properties)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            output.info("Configuration added to application.properties");
        } else {
            output.info("Public and private keys already exist in %s and %s (use --force to overwrite them).",
                    publicKey, privateKey);
        }

        return CommandLine.ExitCode.OK;
    }

    private String getProperty(List<String> lines, String name) {
        String prefix = name + "=";
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length());
            }
        }
        return null;
    }

    private void setProperty(List<String> lines, String name, String value) {
        String prefix = name + "=";
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                lines.set(i, prefix + value);
                return;
            }
        }
        lines.add(prefix + value);
    }

    private void appendToProperty(List<String> lines, String name, String... values) {
        String prefix = name + "=";
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                String existing = lines.get(i).substring(prefix.length());
                List<String> entries = new ArrayList<>(List.of(existing.split(",")));
                for (String v : values) {
                    if (!entries.contains(v)) {
                        entries.add(v);
                    }
                }
                lines.set(i, prefix + String.join(",", entries));
                return;
            }
        }
        lines.add(prefix + String.join(",", values));
    }

}
