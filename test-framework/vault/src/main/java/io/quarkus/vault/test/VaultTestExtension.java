package io.quarkus.vault.test;

import static io.quarkus.vault.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import javax.sql.DataSource;

import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.VaultManager;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.config.VaultTlsConfig;
import io.quarkus.vault.test.client.TestVaultClient;
import io.quarkus.vault.test.client.dto.VaultInit;
import io.quarkus.vault.test.client.dto.VaultSealStatus;

public class VaultTestExtension {

    private static final Logger log = Logger.getLogger(VaultTestExtension.class.getName());

    static final String DB_NAME = "mydb";
    static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "bar";
    public static final String SECRET_VALUE = "s\u20accr\u20act";
    static final String DEFAULT_VAULT_VERSION = "1.2.2";
    static final int VAULT_PORT = 8200;
    static final int MAPPED_POSTGRESQL_PORT = 6543;
    public static final String VAULT_AUTH_USERPASS_USER = "bob";
    public static final String VAULT_AUTH_USERPASS_PASSWORD = "sinclair";
    public static final String VAULT_AUTH_APPROLE = "myapprole";
    public static final String SECRET_PATH_V1 = "secret";
    public static final String SECRET_PATH_V2 = "secret-v2";
    public static final String VAULT_DBROLE = "mydbrole";
    public static final String APP_SECRET_PATH = "foo";
    static final String APP_CONFIG_PATH = "config";
    static final String VAULT_POLICY = "mypolicy";
    static final String POSTGRESQL_HOST = "mypostgresdb";
    static final String VAULT_URL = (useTls() ? "https" : "http") + "://localhost:" + VAULT_PORT;
    public static final String SECRET_KEY = "secret";
    public static final String ENCRYPTION_KEY_NAME = "my-encryption-key";
    public static final String ENCRYPTION_KEY2_NAME = "my-encryption-key2";
    public static final String ENCRYPTION_DERIVED_KEY_NAME = "my-derivation-encryption-key";
    public static final String SIGN_KEY_NAME = "my-sign-key";
    public static final String SIGN_KEY2_NAME = "my-sign-key2";
    public static final String SIGN_DERIVATION_KEY_NAME = "my-derivation-sign-key";

    public static final String TMP_VAULT_POSTGRES_CREATION_SQL_FILE = "/tmp/vault-postgres-creation.sql";
    public static final String TMP_VAULT_CONFIG_JSON_FILE = "/tmp/vault-config.json";
    public static final String TMP_VAULT_POLICY_FILE = "/tmp/vault.policy";
    public static final String TMP_POSTGRES_INIT_SQL_FILE = "/tmp/postgres-init.sql";
    public static final String TEST_QUERY_STRING = "SELECT 1";

    public GenericContainer vaultContainer;
    public PostgreSQLContainer postgresContainer;
    public String rootToken = null;
    public String appRoleSecretId = null;
    public String appRoleRoleId = null;

    private VaultManager vaultManager = createVaultManager();

    private String db_default_ttl = "1m";
    private String db_max_ttl = "10m";

    public static void testDataSource(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection()) {
            try (Statement stmt = c.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(TEST_QUERY_STRING)) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
        }
    }

    private static VaultManager createVaultManager() {
        VaultRuntimeConfig serverConfig = new VaultRuntimeConfig();
        serverConfig.tls = new VaultTlsConfig();
        serverConfig.url = getVaultUrl();
        serverConfig.tls.skipVerify = true;
        serverConfig.tls.caCert = Optional.empty();
        serverConfig.connectTimeout = Duration.ofSeconds(5);
        serverConfig.readTimeout = Duration.ofSeconds(1);
        return new VaultManager(serverConfig, new TestVaultClient(serverConfig));
    }

    private static Optional<URL> getVaultUrl() {
        try {
            return Optional.of(new URL(VAULT_URL));
        } catch (MalformedURLException e) {
            throw new VaultException(e);
        }
    }

    public void start() throws InterruptedException, IOException {

        log.info("start containers on " + System.getProperty("os.name"));

        Network network = Network.newNetwork();

        postgresContainer = new PostgreSQLContainer<>()
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withNetwork(network)
                .withNetworkAliases(POSTGRESQL_HOST)
                .withExposedPorts(POSTGRESQL_PORT)
                .withClasspathResourceMapping("postgres-init.sql", TMP_POSTGRES_INIT_SQL_FILE, READ_ONLY);

        postgresContainer.setPortBindings(Arrays.asList(MAPPED_POSTGRESQL_PORT + ":" + POSTGRESQL_PORT));

        String configFile = useTls() ? "vault-config-tls.json" : "vault-config.json";

        log.info("starting vault with url=" + VAULT_URL + " and config file=" + configFile);

        vaultContainer = new GenericContainer<>("vault:" + getVaultVersion())
                .withExposedPorts(VAULT_PORT)
                .withEnv("SKIP_SETCAP", "true")
                .withEnv("VAULT_SKIP_VERIFY", "true") // this is internal to the container
                .withEnv("VAULT_ADDR", VAULT_URL)
                .withNetwork(network)
                .withClasspathResourceMapping(configFile, TMP_VAULT_CONFIG_JSON_FILE, READ_ONLY)
                .withClasspathResourceMapping("vault-tls.key", "/tmp/vault-tls.key", READ_ONLY)
                .withClasspathResourceMapping("vault-tls.crt", "/tmp/vault-tls.crt", READ_ONLY)
                .withClasspathResourceMapping("vault.policy", TMP_VAULT_POLICY_FILE, READ_ONLY)
                .withClasspathResourceMapping("vault-postgres-creation.sql", TMP_VAULT_POSTGRES_CREATION_SQL_FILE, READ_ONLY)
                .withCommand("server", "-log-level=debug", "-config=" + TMP_VAULT_CONFIG_JSON_FILE);

        vaultContainer.setPortBindings(Arrays.asList(VAULT_PORT + ":" + VAULT_PORT));

        postgresContainer.start();
        execPostgres(format("psql -U %s -d %s -f %s", DB_USERNAME, DB_NAME, TMP_POSTGRES_INIT_SQL_FILE));

        vaultContainer.start();
        initVault();
        log.info("vault has started with root token: " + rootToken);
    }

    private String getVaultVersion() {
        return System.getProperty("vault.version", DEFAULT_VAULT_VERSION);
    }

    private void initVault() throws InterruptedException, IOException {

        TestVaultClient vaultClient = (TestVaultClient) vaultManager.getVaultClient();
        VaultInit vaultInit = vaultClient.init(1, 1);
        String unsealKey = vaultInit.keys.get(0);
        rootToken = vaultInit.rootToken;

        waitForContainerToStart();

        try {
            vaultClient.getHealth();
        } catch (VaultClientException e) {
            // https://www.vaultproject.io/api/system/health.html
            // 503 = sealed
            assertEquals(503, e.getStatus());
        }

        // unseal
        execVault("vault operator unseal " + unsealKey);

        VaultSealStatus sealStatus = vaultClient.getSealStatus();
        assertFalse(sealStatus.sealed);

        // userpass auth
        execVault("vault auth enable userpass");
        execVault(format("vault write auth/userpass/users/%s password=%s policies=%s",
                VAULT_AUTH_USERPASS_USER, VAULT_AUTH_USERPASS_PASSWORD, VAULT_POLICY));

        // approle auth
        execVault("vault auth enable approle");
        execVault(format("vault write auth/approle/role/%s policies=%s",
                VAULT_AUTH_APPROLE, VAULT_POLICY));
        appRoleSecretId = vaultClient.generateAppRoleSecretId(rootToken, VAULT_AUTH_APPROLE).data.secretId;
        appRoleRoleId = vaultClient.getAppRoleRoleId(rootToken, VAULT_AUTH_APPROLE).data.roleId;
        log.info(
                format("generated role_id=%s secret_id=%s for approle=%s", appRoleRoleId, appRoleSecretId, VAULT_AUTH_APPROLE));

        // policy
        execVault(format("vault policy write %s /tmp/vault.policy", VAULT_POLICY));

        // static secrets kv v1
        execVault(format("vault secrets enable -path=%s kv", SECRET_PATH_V1));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, APP_SECRET_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, APP_CONFIG_PATH, PASSWORD_PROPERTY_NAME, DB_PASSWORD));

        // multi config
        execVault(format("vault kv put %s/multi/default1 color=blue size=XL", SECRET_PATH_V1));
        execVault(format("vault kv put %s/multi/default2 color=red weight=3", SECRET_PATH_V1));
        execVault(format("vault kv put %s/multi/singer1 firstname=paul lastname=shaffer", SECRET_PATH_V1));
        execVault(format("vault kv put %s/multi/singer2 lastname=simon age=78 color=green", SECRET_PATH_V1));

        // static secrets kv v2
        execVault(format("vault secrets enable -path=%s -version=2 kv", SECRET_PATH_V2));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, APP_SECRET_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, APP_CONFIG_PATH, PASSWORD_PROPERTY_NAME, DB_PASSWORD));

        // dynamic secrets

        execVault("vault secrets enable database");

        String vault_write_database_config_mydb = format(
                "vault write database/config/%s " +
                        "plugin_name=postgresql-database-plugin " +
                        "allowed_roles=%s " +
                        "connection_url=postgresql://{{username}}:{{password}}@%s:%s/%s?sslmode=disable " +
                        "username=%s " +
                        "password=%s",
                DB_NAME, VAULT_DBROLE, POSTGRESQL_HOST, POSTGRESQL_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD);
        execVault(vault_write_database_config_mydb);

        String vault_write_database_roles_mydbrole = format(
                "vault write database/roles/%s " +
                        "db_name=%s " +
                        "creation_statements=@%s " +
                        "default_ttl=%s " +
                        "max_ttl=%s " +
                        "revocation_statements=\"ALTER ROLE \\\"{{name}}\\\" NOLOGIN;\" " +
                        "renew_statements=\"ALTER ROLE \\\"{{name}}\\\" VALID UNTIL '{{expiration}}';\"",
                VAULT_DBROLE, DB_NAME, TMP_VAULT_POSTGRES_CREATION_SQL_FILE, db_default_ttl, db_max_ttl);
        execVault(vault_write_database_roles_mydbrole);

        // transit

        execVault("vault secrets enable transit");
        execVault(format("vault write -f transit/keys/%s", ENCRYPTION_KEY_NAME));
        execVault(format("vault write -f transit/keys/%s", ENCRYPTION_KEY2_NAME));
        execVault(format("vault write transit/keys/%s derived=true", ENCRYPTION_DERIVED_KEY_NAME));
        execVault(format("vault write transit/keys/%s type=ecdsa-p256", SIGN_KEY_NAME));
        execVault(format("vault write transit/keys/%s type=ecdsa-p256", SIGN_KEY2_NAME));
        execVault(format("vault write transit/keys/%s type=ed25519 derived=true", SIGN_DERIVATION_KEY_NAME));

        execVault("vault write transit/keys/jws type=ecdsa-p256");
    }

    public static boolean useTls() {
        return System.getProperty("vault-test-extension.use-tls", TRUE.toString()).equals(TRUE.toString());
    }

    private void waitForContainerToStart() throws InterruptedException, IOException {
        Instant started = Instant.now();
        while (Instant.now().isBefore(started.plusSeconds(20))) {
            Container.ExecResult vault_status = vaultContainer.execInContainer(createVaultCommand("vault status"));
            if (vault_status.getExitCode() == 2) { // 2 => sealed
                return;
            }
        }
        fail("vault failed to start");
    }

    private Container.ExecResult execPostgres(String command) throws IOException, InterruptedException {
        return exec(postgresContainer, new String[] { "/bin/sh", "-c", command });
    }

    private Container.ExecResult execVault(String command) throws IOException, InterruptedException {
        String[] cmd = createVaultCommand(command);
        return exec(vaultContainer, cmd);
    }

    private static Container.ExecResult exec(GenericContainer container, String[] cmd)
            throws IOException, InterruptedException {

        Container.ExecResult execResult = container.execInContainer(cmd);

        if (execResult.getExitCode() != 0) {
            throw new RuntimeException(
                    "command " + Arrays.asList(cmd) + " failed with exit code " + execResult.getExitCode() + "\n"
                            + execResult.getStderr());
        } else {
            log.info("> " + Arrays.asList(cmd) + "\n" + execResult.getStdout());
        }
        return execResult;
    }

    @NotNull
    private String[] createVaultCommand(String command) {
        String cmd = (rootToken != null ? "export VAULT_TOKEN=" + rootToken + " && " : "") + command;
        return new String[] { "/bin/sh", "-c", cmd };
    }

    public void close() {

        log.info("stop containers");

        if (vaultContainer != null && vaultContainer.isRunning()) {
            vaultContainer.stop();
        }
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
        }

        VaultManager.getInstance().reset();
    }

}
