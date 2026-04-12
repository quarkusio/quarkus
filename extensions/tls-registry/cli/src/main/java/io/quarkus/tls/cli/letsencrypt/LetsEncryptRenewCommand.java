package io.quarkus.tls.cli.letsencrypt;

import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.CERT_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.DOT_ENV_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.KEY_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.LETS_ENCRYPT_DIR;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptHelpers.*;

import java.util.concurrent.Callable;

import org.jboss.logging.Logger;

import picocli.CommandLine;

@CommandLine.Command(name = "renew-certificate", mixinStandardHelpOptions = true, description = "Renew a Let's Encrypt. This command re-runs the HTTP 01 challenge of let's encrypt to retrieve a new certificate. "
        + "Make sure the application is running before running this command.")
public class LetsEncryptRenewCommand implements Callable<Integer> {

    static Logger LOGGER = Logger.getLogger(LetsEncryptRenewCommand.class);

    @CommandLine.Option(names = { "-d",
            "--domain" }, description = "The domain for which the certificate will be generated", required = true)
    String domain;

    @CommandLine.Option(names = { "-n",
            "--tls-configuration-name" }, description = "The name of the TLS configuration to be used, if not set, the default configuration is used")
    String tlsConfigurationName;

    @CommandLine.Option(names = {
            "--management-url" }, description = "The URL of the management endpoint to use for the ACME challenge", required = true)
    String managementUrl;

    @CommandLine.Option(names = {
            "--management-user" }, description = "The username to use for the management endpoint")
    String managementUser;

    @CommandLine.Option(names = {
            "--management-password" }, description = "The password to use for the management endpoint")
    String managementPassword;

    @CommandLine.Option(names = {
            "--staging" }, description = "Whether to use the staging environment of Let's Encrypt", defaultValue = "false")
    boolean staging;

    @CommandLine.Option(names = {
            "--insecure" }, description = "Disable SSL certificate validation for the management endpoint (INSECURE - development/testing only)", defaultValue = "false")
    boolean insecure;

    @CommandLine.Option(names = {
            "--acme-server-url" }, description = "Custom ACME production server URL (default: Let's Encrypt production)")
    String acmeServerUrl;

    @CommandLine.Option(names = {
            "--acme-staging-server-url" }, description = "Custom ACME staging server URL (default: Let's Encrypt staging)")
    String acmeStagingServerUrl;

    @Override
    public Integer call() throws Exception {
        if (insecure) {
            AUDIT.error("SSL certificate validation DISABLED via --insecure flag for management endpoint: " + managementUrl);
            AUDIT.error("This configuration is INSECURE and must not be used in production");
        }

        AcmeClient client = new AcmeClient(managementUrl, managementUser, managementPassword, tlsConfigurationName, insecure);

        // Step 0 - Verification
        // - Make sure the .letsencrypt directory exists
        if (!LETS_ENCRYPT_DIR.exists()) {
            LOGGER.error(
                    "The .letsencrypt directory does not exist, please run the `quarkus tls letsencrypt prepare` command first");
            return 1;
        }
        // - Make sure the cert and key files exist
        if (!CERT_FILE.isFile() || !KEY_FILE.isFile()) {
            LOGGER.error(
                    "The certificate and key files do not exist, please run the `quarkus tls letsencrypt prepare` command first");
            return 1;
        }
        // - Make sure the .env file exists
        if (!DOT_ENV_FILE.isFile()) {
            LOGGER.error("The .env file does not exist, please run the `quarkus tls letsencrypt prepare` command first");
            return 1;
        }
        // - Make sure application is running
        if (!client.checkReadiness()) {
            return 1;
        }

        // Step 1 - Renew
        renewCertificate(client, LETS_ENCRYPT_DIR, staging, domain, CERT_FILE, KEY_FILE, acmeServerUrl,
                acmeStagingServerUrl);
        adjustPermissions(CERT_FILE, KEY_FILE);

        // Step 2 - Reload certificate
        client.certificateChainAndKeyAreReady();

        LOGGER.infof("✅ Successfully renewed certificate for %s", domain);

        return 0;
    }
}
