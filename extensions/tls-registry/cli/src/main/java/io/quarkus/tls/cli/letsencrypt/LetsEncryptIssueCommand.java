package io.quarkus.tls.cli.letsencrypt;

import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.CERT_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.DOT_ENV_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.KEY_FILE;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.LETS_ENCRYPT_DIR;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptHelpers.adjustPermissions;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptHelpers.createAccount;
import static io.quarkus.tls.cli.letsencrypt.LetsEncryptHelpers.issueCertificate;
import static java.lang.System.Logger.Level.INFO;

import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "issue-certificate", mixinStandardHelpOptions = true, description = "Issue a certificate from let's encrypt. This command runs the HTTP 01 challenge of let's encrypt. "
        +
        "Make sure the application is running before running this command.")
public class LetsEncryptIssueCommand implements Callable<Integer> {

    static System.Logger LOGGER = System.getLogger("lets-encrypt-issue");

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

    @Override
    public Integer call() throws Exception {
        AcmeClient client = new AcmeClient(managementUrl, managementUser, managementPassword, tlsConfigurationName);

        // Step 0 - Verification
        // - Make sure the .letsencrypt directory exists
        if (!LETS_ENCRYPT_DIR.exists()) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "The .letsencrypt directory does not exist, please run the `quarkus tls letsencrypt prepare` command first");
            return 1;
        }
        // - Make sure the cert and key files exist
        if (!CERT_FILE.isFile() || !KEY_FILE.isFile()) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "The certificate and key files do not exist, please run the `quarkus tls letsencrypt prepare` command first");
            return 1;
        }
        // - Make sure the .env file exists
        if (!DOT_ENV_FILE.isFile()) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "The .env file does not exist, please run the `quarkus tls letsencrypt prepare` command first");
            return 1;
        }
        // - Make sure application is running
        if (!client.checkReadiness()) {
            return 1;
        }

        // Step 1 - Account
        createAccount(client, LETS_ENCRYPT_DIR.getAbsolutePath(), staging, email);

        // Step 2 - run the challenge to obtain first certificate
        LOGGER.log(INFO, "\uD83D\uDD35 Requesting initial certificate from {0} Let's Encrypt", (staging ? "staging" : ""));
        issueCertificate(client, LETS_ENCRYPT_DIR, staging, domain, CERT_FILE, KEY_FILE);
        adjustPermissions(CERT_FILE, KEY_FILE);

        // Step 3 - Reload certificate
        client.certificateChainAndKeyAreReady();

        LOGGER.log(INFO, "✅ Successfully obtained certificate for {0}", domain);

        return 0;
    }
}
