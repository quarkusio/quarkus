package io.quarkus.tls.cli.letsencrypt;

import static io.quarkus.tls.cli.DotEnvHelper.addOrReplaceProperty;
import static io.quarkus.tls.cli.DotEnvHelper.deleteQuietly;
import static io.quarkus.tls.cli.DotEnvHelper.readDotEnvFile;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.CA_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.CERT_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.DOT_ENV_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.KEY_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.LETS_ENCRYPT_DIR;

import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import picocli.CommandLine;

@CommandLine.Command(name = "prepare", mixinStandardHelpOptions = true, description = "Prepare the environment to receive Let's Encrypt certificates."
        + " Make sure to restart the application after having run this command.")
public class LetsEncryptPrepareCommand implements Callable<Integer> {

    static System.Logger LOGGER = System.getLogger("lets-encrypt-prepare");

    @CommandLine.Option(names = { "-d",
            "--domain" }, description = "The domain for which the certificate will be generated", required = true)
    String domain;

    @CommandLine.Option(names = { "-n",
            "--tls-configuration-name" }, description = "The name of the TLS configuration to be used, if not set, the default configuration is used")
    String tlsConfigurationName;

    @Override
    public Integer call() throws Exception {
        // Step 1 - Create .letsencrypt directory
        if (!LETS_ENCRYPT_DIR.exists()) {
            if (LETS_ENCRYPT_DIR.mkdir()) {
                LOGGER.log(System.Logger.Level.INFO, "✅ Created .letsencrypt directory: {0}",
                        LETS_ENCRYPT_DIR.getAbsolutePath());
            }
        }

        // Step 2 - Generate self-signed certificate

        boolean certExistingAndStillValid = false;
        if (CERT_FILE.isFile() && KEY_FILE.isFile()) {
            // The cert and key are already present, check if they are expired
            var existing = LetsEncryptHelpers.loadCertificateFromPEM(CERT_FILE.getAbsolutePath());
            try {
                existing.checkValidity();
                certExistingAndStillValid = true;
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.INFO, "⚠\uFE0F The existing certificate is expired, regenerating it...");
            }
        }

        if (!certExistingAndStillValid) {
            // Generate a self-signed certificate (for the challenge
            CertificateGenerator generator = new CertificateGenerator(LETS_ENCRYPT_DIR.toPath(), true);
            CertificateRequest request = new CertificateRequest()
                    .withCN(domain)
                    .withSubjectAlternativeName("DNS:" + domain)
                    .withDuration(Duration.ofDays(30)) // Should be plenty to run the challenge
                    .withFormat(Format.PEM)
                    .withName("lets-encrypt");
            generator.generate(request);
        } else {
            LOGGER.log(System.Logger.Level.INFO, "✅ Certificate already exists and is still valid: {0}",
                    CERT_FILE.getAbsolutePath());
        }

        // Delete the CA file, we do not use it.
        deleteQuietly(CA_FILE);

        // Step 3 - Create .env file or append if exists
        List<String> dotEnvContent = readDotEnvFile();

        String prefix = "quarkus.tls";
        if (tlsConfigurationName != null) {
            prefix += "." + tlsConfigurationName;
        }

        // We cannot set quarkus.management.enabled and quarkus.tls.lets-encrypt.enabled as they are build time properties.
        addOrReplaceProperty(dotEnvContent, prefix + ".key-store.pem.acme.cert", CERT_FILE.getAbsolutePath());
        addOrReplaceProperty(dotEnvContent, prefix + ".key-store.pem.acme.key", KEY_FILE.getAbsolutePath());

        Files.write(DOT_ENV_FILE.toPath(), dotEnvContent);
        LOGGER.log(System.Logger.Level.INFO, "✅ .env file configured for Let's Encrypt: {0}", DOT_ENV_FILE.getAbsolutePath());
        LOGGER.log(System.Logger.Level.INFO,
                "➡\uFE0F Start the application and run `quarkus tls lets-encrypt issue-certificate --domain={0}{1}` to complete the challenge",
                domain,
                tlsConfigurationName != null ? " -tls-configuration-name=" + tlsConfigurationName : "");
        return 0;
    }

}
