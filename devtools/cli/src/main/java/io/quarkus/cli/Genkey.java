package io.quarkus.cli;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@CommandLine.Command(name = "genkey", sortOptions = false, header = "Generate RSA or EC public/private keys.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "Options:%n")
public class Genkey extends BaseBuildCommand implements Callable<Integer> {

    @Option(names = { "-f", "--force" }, description = "Overwrite existing private/public keys")
    boolean force;

    @Option(names = { "-s", "--size" }, description = "Key size (Defaults to 2048 for RSA, 256 for EC)")
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
        Path privateKey = resourcesFolder.resolve("private-key.pem");
        Path publicKey = resourcesFolder.resolve("public-key.pem");
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
            output.info("Public and private keys created in %s and %s\nYou can use this configuration for JWT:\n", publicKey,
                    privateKey);
            output.info("mp.jwt.verify.publickey.location=public-key.pem\n"
                    + "mp.jwt.decrypt.key.location=private-key.pem\n"
                    + "smallrye.jwt.sign.key.location=private-key.pem\n"
                    + "smallrye.jwt.encrypt.key.location=public-key.pem\n"
                    + " \n"
                    + "quarkus.native.resources.includes=public-key.pem\n"
                    + "quarkus.native.resources.includes=private-key.pem");
        } else {
            output.info("Public and private keys already exist in %s and %s (use --force to overwrite them).",
                    publicKey, privateKey);
        }

        return CommandLine.ExitCode.OK;
    }

}
