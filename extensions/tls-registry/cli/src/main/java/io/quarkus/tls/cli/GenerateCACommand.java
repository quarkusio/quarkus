package io.quarkus.tls.cli;

import static io.quarkus.tls.cli.Constants.CA_FILE;
import static io.quarkus.tls.cli.Constants.KEYSTORE_FILE;
import static io.quarkus.tls.cli.Constants.PK_FILE;
import static java.lang.System.Logger.Level.INFO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import io.smallrye.certs.ca.CaGenerator;
import picocli.CommandLine;

@CommandLine.Command(name = "generate-quarkus-ca", mixinStandardHelpOptions = true, description = "Generate Quarkus Dev CA certificate and private key.")
public class GenerateCACommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-i",
            "--install" }, description = "Install the generated CA into the system keychain.", defaultValue = "false")
    boolean install;

    @CommandLine.Option(names = { "-t",
            "--truststore" }, description = "Generate a PKCS12 (`.p12`) truststore containing the generated CA.", defaultValue = "false")
    boolean truststore;

    @CommandLine.Option(names = { "-r",
            "--renew" }, description = "Update certificate if already created.", defaultValue = "false")
    boolean renew;

    static System.Logger LOGGER = System.getLogger("generate-quarkus-ca");

    @Override
    public Integer call() throws Exception {
        LOGGER.log(INFO, "🔥 Generating Quarkus Dev CA certificate...");
        if (!Constants.BASE_DIR.exists()) {
            Constants.BASE_DIR.mkdirs();
        }

        if (CA_FILE.exists() && !renew) {
            if (!hasExpired()) {
                LOGGER.log(INFO,
                        "✅ Quarkus Dev CA certificate already exists and has not yet expired. Use --renew to update.");
                return 0;
            }
        }

        String username = System.getProperty("user.name", "");
        CaGenerator generator = new CaGenerator(CA_FILE, PK_FILE, KEYSTORE_FILE, "quarkus");
        generator
                .generate("quarkus-dev-root-ca", "Quarkus Development (" + username + ")", "Quarkus Development",
                        "home", "world", "universe");
        if (install) {
            LOGGER.log(INFO, "🔥 Installing the CA certificate in the system truststore...");
            generator.installToSystem();
        }

        if (truststore) {
            LOGGER.log(INFO, "🔥 Generating p12 truststore...");
            File ts = new File("quarkus-ca-truststore.p12");
            generator.generateTrustStore(ts);
            LOGGER.log(INFO, "✅ Truststore generated successfully.");
        }

        LOGGER.log(INFO, "✅ Quarkus Dev CA certificate generated and installed");

        return 0;
    }

    private boolean hasExpired() throws Exception {
        var cert = getCertificateFromPKCS12();
        try {
            cert.checkValidity();
        } catch (Exception e) {
            LOGGER.log(INFO, "🔥 Certificate has expired. Renewing...");
            return true;
        }
        return false;
    }

    private static X509Certificate getCertificateFromPKCS12()
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        try (FileInputStream fis = new FileInputStream(KEYSTORE_FILE)) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, "quarkus".toCharArray());
            Certificate cert = keystore.getCertificate(CaGenerator.KEYSTORE_CERT_ENTRY);
            if (cert == null) {
                throw new KeyStoreException("No certificate found with alias: " + CaGenerator.KEYSTORE_CERT_ENTRY);
            }
            return (X509Certificate) cert;
        }
    }

}
