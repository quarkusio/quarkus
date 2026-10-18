package io.quarkus.tls.cli.letsencrypt;

import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.CERT_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.DOT_ENV_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.KEY_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.LETS_ENCRYPT_DIR;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptHelpers.adjustPermissions;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptHelpers.createAccount;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptHelpers.issueCertificate;

import java.util.concurrent.Callable;

import org.jboss.logging.Logger;

import picocli.CommandLine;

@CommandLine.Command(name = "issue-certificate", mixinStandardHelpOptions = true, description = "Issue a certificate from let's encrypt. This command runs the HTTP 01 challenge of let's encrypt. "
        +
        "Make sure the application is running before running this command.")
public class LetsEncryptIssueCommand implements Callable<Integer> {

    static Logger LOGGER = Logger.getLogger(LetsEncryptIssueCommand.class);

    @CommandLine.Option(names = { "-d",
            "--domain" }, description = "The domain for which the certificate will be generated", required = true)
    String domain;

    @CommandLine.Option(names = { "-n",
            "--tls-configuration-name" }, description = "The name of the TLS configuration to be used, if not set, the default configuration is used")
    String tlsConfigurationName;

    // TODO Check if /lets-encrypt is appended
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
            "--email" }, description = "The email of the account to use for the ACME challenge", required = true)
    String email;

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
    public Integer call() {
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

        // Step 1 - Account
        createAccount(client, LETS_ENCRYPT_DIR.getAbsolutePath(), staging, email, acmeServerUrl, acmeStagingServerUrl);

        // Step 2 - run the challenge to obtain first certificate
        LOGGER.infof("\uD83D\uDD35 Requesting initial certificate from %s ACME server", (staging ? "staging" : "production"));
        issueCertificate(client, LETS_ENCRYPT_DIR, staging, domain, CERT_FILE, KEY_FILE, acmeServerUrl,
                acmeStagingServerUrl);
        adjustPermissions(CERT_FILE, KEY_FILE);

        // Step 3 - Reload certificate
        client.certificateChainAndKeyAreReady();

        LOGGER.infof("✅ Successfully obtained certificate for %s", domain);

        return 0;
    }
}
