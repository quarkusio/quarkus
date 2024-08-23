package io.quarkus.tls.cli;

import static io.quarkus.tls.cli.Constants.CA_FILE;
import static io.quarkus.tls.cli.Constants.PK_FILE;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.Callable;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import io.smallrye.common.os.OS;
import picocli.CommandLine;

@CommandLine.Command(name = "generate-certificate", mixinStandardHelpOptions = true, description = "Generate a TLS certificate with the Quarkus Dev CA if available.")
public class GenerateCertificateCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-n",
            "--name" }, description = "Name of the certificate. It will be used as file name and alias in the keystore", required = true)
    String name;

    @CommandLine.Option(names = { "-p",
            "--password" }, description = "The password of the keystore. Default is 'password'", defaultValue = "password", required = false)
    String password;

    @CommandLine.Option(names = { "-c",
            "--cn" }, description = "The common name of the certificate. Default is 'localhost'", defaultValue = "localhost", required = false)
    String cn;

    @CommandLine.Option(names = { "-d",
            "--directory" }, description = "The directory in which the certificates will be created. Default is `.certs`", defaultValue = ".certs")
    String directory;

    @CommandLine.Option(names = { "-r",
            "--renew" }, description = "Whether existing certificates will need to be replaced", defaultValue = "false")
    boolean renew;

    @CommandLine.Option(names = {
            "--self-signed" }, description = "Generate a self-signed certificate", defaultValue = "false", hidden = true)
    boolean selfSigned;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    static System.Logger LOGGER = System.getLogger("generate-quarkus-ca");

    @Override
    public Integer call() throws Exception {
        LOGGER.log(INFO, "\uD83D\uDD0E Looking for the Quarkus Dev CA certificate...");

        if (!CA_FILE.exists() || !PK_FILE.exists() || selfSigned) {
            LOGGER.log(INFO, "\uD83C\uDFB2 Quarkus Dev CA certificate not found. Generating a self-signed certificate...");
            generateSelfSignedCertificate();
            return 0;
        }

        LOGGER.log(INFO, "\uD83D\uDCDC Quarkus Dev CA certificate found at {0}", CA_FILE.getAbsolutePath());
        X509Certificate caCert = loadRootCertificate(CA_FILE);
        PrivateKey caPrivateKey = loadPrivateKey();

        createSignedCertificate(caCert, caPrivateKey);

        LOGGER.log(INFO, "✅ Signed Certificate generated successfully and exported into `{0}-keystore.p12`", name);
        printConfig(new File(directory, name + "-keystore.p12").getAbsolutePath(), password);

        return 0;
    }

    private void generateSelfSignedCertificate() throws Exception {
        File out = new File(directory);
        if (!out.exists()) {
            out.mkdirs();
        }
        new CertificateGenerator(out.toPath(), renew).generate(new CertificateRequest()
                .withName(name)
                .withCN(cn)
                .withPassword(password)
                .withDuration(Duration.ofDays(365))
                .withFormat(Format.PKCS12));
        LOGGER.log(INFO, "✅ Self-signed certificate generated successfully and exported into `{0}-keystore.p12`", name);
        printConfig(new File(directory, name + "-keystore.p12").getAbsolutePath(), password);

    }

    private void printConfig(String path, String password) {
        if (OS.WINDOWS.isCurrent()) {
            path = path.replace("\\", "\\\\");
        }

        // .env format
        String env = String.format("""
                _DEV_QUARKUS_TLS_KEY_STORE_P12_PATH=%s
                _DEV_QUARKUS_TLS_KEY_STORE_P12_PASSWORD=%s
                """, path, password);

        var dotEnvFile = new File(".env");
        try (var writer = new FileWriter(dotEnvFile, dotEnvFile.isFile())) {
            writer.write(env);
        } catch (IOException e) {
            LOGGER.log(ERROR, "Failed to write to .env file", e);
        }

        LOGGER.log(INFO, """
                    ✅ Required configuration added to the `.env` file:
                    %dev.quarkus.tls.key-store.p12.path={0}
                    %dev.quarkus.tls.key-store.p12.password={1}
                """, path, password);
    }

    private X509Certificate loadRootCertificate(File ca) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(ca)) {
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(Constants.PK_FILE));
                PEMParser pemParser = new PEMParser(reader)) {
            Object obj = pemParser.readObject();
            if (obj instanceof KeyPair) {
                return ((KeyPair) obj).getPrivate();
            } else if (obj instanceof PrivateKeyInfo) {
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                return converter.getPrivateKey(((PrivateKeyInfo) obj));
            } else {
                throw new IllegalStateException(
                        "The file " + Constants.PK_FILE.getAbsolutePath() + " does not contain a private key "
                                + obj.getClass().getName());
            }
        }
    }

    private void createSignedCertificate(X509Certificate issuerCert,
            PrivateKey issuerPrivateKey) throws Exception {
        File out = new File(directory);
        if (!out.exists()) {
            out.mkdirs();
        }
        new CertificateGenerator(out.toPath(), renew).generate(new CertificateRequest()
                .withName(name)
                .withCN(cn)
                .withPassword(password)
                .withDuration(Duration.ofDays(365))
                .withFormat(Format.PKCS12)
                .signedWith(issuerCert, issuerPrivateKey));

    }
}
