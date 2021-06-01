package io.quarkus.vault.test;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.vault.runtime.VaultAuthManager.USERPASS_WRAPPING_TOKEN_PASSWORD_KEY;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.MULTILINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.jboss.logging.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.VaultVersions;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitResponse;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultSealStatusResult;
import io.quarkus.vault.runtime.config.VaultAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.quarkus.vault.runtime.config.VaultEnterpriseConfig;
import io.quarkus.vault.runtime.config.VaultKubernetesAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultTlsConfig;
import io.quarkus.vault.test.client.TestVaultClient;

public class VaultTestExtension {

    private static final Logger log = Logger.getLogger(VaultTestExtension.class.getName());

    static final String DB_NAME = "mydb";
    static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "bar";
    public static final String SECRET_VALUE = "s\u20accr\u20act";
    static final int VAULT_PORT = 8200;
    static final int MAPPED_POSTGRESQL_PORT = 6543;
    public static final String VAULT_AUTH_USERPASS_USER = "bob";
    public static final String VAULT_AUTH_USERPASS_PASSWORD = "sinclair";
    public static final String VAULT_AUTH_APPROLE = "myapprole";
    public static final String SECRET_PATH_V1 = "secret-v1";
    public static final String SECRET_PATH_V2 = "secret";
    public static final String LIST_PATH = "hello";
    public static final String LIST_SUB_PATH = "world";
    public static final String EXPECTED_SUB_PATHS = "[" + LIST_SUB_PATH + "]";
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
    public static final String TMP_POSTGRES_INIT_SQL_FILE = "/tmp/postgres-init.sql";
    public static final String TEST_QUERY_STRING = "SELECT 1";
    public static final String CONTAINER_TMP_CMD = "/tmp/cmd";
    public static final String HOST_VAULT_TMP_CMD = "target/vault_cmd";
    public static final String HOST_POSTGRES_TMP_CMD = "target/postgres_cmd";
    public static final String OUT_FILE = "/out";
    public static final String WRAPPING_TEST_PATH = "wrapping-test";

    private static String CRUD_PATH = "crud";

    public GenericContainer vaultContainer;
    public PostgreSQLContainer postgresContainer;
    public String rootToken = null;
    public String appRoleSecretId = null;
    public String appRoleRoleId = null;
    public String appRoleSecretIdWrappingToken = null;
    public String clientTokenWrappingToken = null;
    public String passwordKvv1WrappingToken = null;
    public String passwordKvv2WrappingToken = null;
    public String anotherPasswordKvv2WrappingToken = null;

    private TestVaultClient vaultClient;

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

    public static void assertCrudSecret(VaultKVSecretEngine kvSecretEngine) {

        assertEquals(EXPECTED_SUB_PATHS, kvSecretEngine.listSecrets(LIST_PATH).toString());

        assertDeleteSecret(kvSecretEngine);

        assertDeleteSecret(kvSecretEngine);

        Map<String, String> newsecrets = new HashMap<>();
        newsecrets.put("first", "one");
        newsecrets.put("second", "two");
        kvSecretEngine.writeSecret(CRUD_PATH, newsecrets);
        assertEquals("{first=one, second=two}", readSecretAsString(kvSecretEngine, CRUD_PATH));

        newsecrets.put("first", "un");
        newsecrets.put("third", "tres");
        kvSecretEngine.writeSecret(CRUD_PATH, newsecrets);
        assertEquals("{first=un, second=two, third=tres}", readSecretAsString(kvSecretEngine, CRUD_PATH));

        assertDeleteSecret(kvSecretEngine);
    }

    private static void assertDeleteSecret(VaultKVSecretEngine kvSecretEngine) {
        kvSecretEngine.deleteSecret(CRUD_PATH);
        try {
            readSecretAsString(kvSecretEngine, CRUD_PATH);
        } catch (VaultClientException e) {
            assertEquals(404, e.getStatus());
        }
    }

    private static String readSecretAsString(VaultKVSecretEngine kvSecretEngine, String path) {
        Map<String, String> secret = kvSecretEngine.readSecret(path);
        return new TreeMap<>(secret).toString();
    }

    private TestVaultClient createVaultClient() {
        VaultBootstrapConfig vaultBootstrapConfig = new VaultBootstrapConfig();
        vaultBootstrapConfig.tls = new VaultTlsConfig();
        vaultBootstrapConfig.url = getVaultUrl();
        vaultBootstrapConfig.enterprise = new VaultEnterpriseConfig();
        vaultBootstrapConfig.enterprise.namespace = Optional.empty();
        vaultBootstrapConfig.tls.skipVerify = Optional.of(true);
        vaultBootstrapConfig.tls.caCert = Optional.empty();
        vaultBootstrapConfig.connectTimeout = Duration.ofSeconds(5);
        vaultBootstrapConfig.readTimeout = Duration.ofSeconds(1);
        vaultBootstrapConfig.nonProxyHosts = Optional.empty();
        vaultBootstrapConfig.authentication = new VaultAuthenticationConfig();
        vaultBootstrapConfig.authentication.kubernetes = new VaultKubernetesAuthenticationConfig();
        return new TestVaultClient(new VaultConfigHolder().setVaultBootstrapConfig(vaultBootstrapConfig));
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

        new File(HOST_POSTGRES_TMP_CMD).mkdirs();

        Network network = Network.newNetwork();

        postgresContainer = new PostgreSQLContainer<>()
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withNetwork(network)
                .withFileSystemBind(HOST_POSTGRES_TMP_CMD, CONTAINER_TMP_CMD)
                .withNetworkAliases(POSTGRESQL_HOST)
                .withExposedPorts(POSTGRESQL_PORT)
                .withClasspathResourceMapping("postgres-init.sql", TMP_POSTGRES_INIT_SQL_FILE, READ_ONLY);

        postgresContainer.setPortBindings(Arrays.asList(MAPPED_POSTGRESQL_PORT + ":" + POSTGRESQL_PORT));

        String configFile = useTls() ? "vault-config-tls.json" : "vault-config.json";

        String vaultImage = getVaultImage();
        log.info("starting " + vaultImage + " with url=" + VAULT_URL + " and config file=" + configFile);

        new File(HOST_VAULT_TMP_CMD).mkdirs();

        vaultContainer = new GenericContainer<>(vaultImage)
                .withExposedPorts(VAULT_PORT)
                .withEnv("SKIP_SETCAP", "true")
                .withEnv("VAULT_SKIP_VERIFY", "true") // this is internal to the container
                .withEnv("VAULT_ADDR", VAULT_URL)
                .withNetwork(network)
                .withFileSystemBind(HOST_VAULT_TMP_CMD, CONTAINER_TMP_CMD)
                .withClasspathResourceMapping(configFile, TMP_VAULT_CONFIG_JSON_FILE, READ_ONLY)
                .withClasspathResourceMapping("vault-tls.key", "/tmp/vault-tls.key", READ_ONLY)
                .withClasspathResourceMapping("vault-tls.crt", "/tmp/vault-tls.crt", READ_ONLY)
                .withClasspathResourceMapping("vault-postgres-creation.sql", TMP_VAULT_POSTGRES_CREATION_SQL_FILE, READ_ONLY)
                .withCommand("server", "-log-level=debug", "-config=" + TMP_VAULT_CONFIG_JSON_FILE);

        vaultContainer.setPortBindings(Arrays.asList(VAULT_PORT + ":" + VAULT_PORT));

        postgresContainer.start();
        execPostgres(format("psql -U %s -d %s -f %s", DB_USERNAME, DB_NAME, TMP_POSTGRES_INIT_SQL_FILE));

        Consumer<OutputFrame> consumer = outputFrame -> System.out.print("VAULT >> " + outputFrame.getUtf8String());
        vaultContainer.setLogConsumers(Arrays.asList(consumer));
        vaultContainer.start();

        initVault();
        log.info("vault has started with root token: " + rootToken);
    }

    private String getVaultImage() {
        return "vault:" + VaultVersions.VAULT_TEST_VERSION;
    }

    private void initVault() throws InterruptedException, IOException {

        vaultClient = createVaultClient();
        VaultInternalSystemBackend vaultInternalSystemBackend = new VaultInternalSystemBackend();
        vaultInternalSystemBackend.setVaultClient(vaultClient);

        VaultInitResponse vaultInit = vaultInternalSystemBackend.init(1, 1);
        String unsealKey = vaultInit.keys.get(0);
        rootToken = vaultInit.rootToken;

        waitForContainerToStart();

        try {
            vaultInternalSystemBackend.systemHealthStatus(false, false);
        } catch (VaultClientException e) {
            // https://www.vaultproject.io/api/system/health.html
            // 503 = sealed
            assertEquals(503, e.getStatus());
        }

        // unseal
        execVault("vault operator unseal " + unsealKey);

        VaultSealStatusResult sealStatus = vaultInternalSystemBackend.systemSealStatus();
        assertFalse(sealStatus.sealed);

        // userpass auth
        execVault("vault auth enable userpass");
        execVault(format("vault write auth/userpass/users/%s password=%s policies=%s",
                VAULT_AUTH_USERPASS_USER, VAULT_AUTH_USERPASS_PASSWORD, VAULT_POLICY));

        // k8s auth
        execVault("vault auth enable kubernetes");

        // approle auth
        execVault("vault auth enable approle");
        execVault(format("vault write auth/approle/role/%s policies=%s",
                VAULT_AUTH_APPROLE, VAULT_POLICY));
        appRoleSecretId = vaultClient.generateAppRoleSecretId(rootToken, VAULT_AUTH_APPROLE).data.secretId;
        appRoleRoleId = vaultClient.getAppRoleRoleId(rootToken, VAULT_AUTH_APPROLE).data.roleId;
        log.info(
                format("generated role_id=%s secret_id=%s for approle=%s", appRoleRoleId, appRoleSecretId, VAULT_AUTH_APPROLE));

        // policy
        String policyContent = readResourceContent("vault.policy");
        vaultInternalSystemBackend.createUpdatePolicy(rootToken, VAULT_POLICY, new VaultPolicyBody(policyContent));

        // static secrets kv v1
        execVault(format("vault secrets enable -path=%s kv", SECRET_PATH_V1));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, APP_SECRET_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(
                format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, LIST_PATH + "/" + LIST_SUB_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, APP_CONFIG_PATH, PASSWORD_PROPERTY_NAME, DB_PASSWORD));

        // static secrets kv v2
        execVault(format("vault secrets enable -path=%s -version=2 kv", SECRET_PATH_V2));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, APP_SECRET_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(
                format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, LIST_PATH + "/" + LIST_SUB_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, APP_CONFIG_PATH, PASSWORD_PROPERTY_NAME, DB_PASSWORD));

        // multi config
        execVault(format("vault kv put %s/multi/default1 color=blue size=XL", SECRET_PATH_V2));
        execVault(format("vault kv put %s/multi/default2 color=red weight=3", SECRET_PATH_V2));
        execVault(format("vault kv put %s/multi/singer1 firstname=paul lastname=shaffer", SECRET_PATH_V2));
        execVault(format("vault kv put %s/multi/singer2 lastname=simon age=78 color=green", SECRET_PATH_V2));

        // wrapped

        appRoleSecretIdWrappingToken = fetchWrappingToken(
                execVault(format("vault write -wrap-ttl=120s -f auth/approle/role/%s/secret-id", VAULT_AUTH_APPROLE)));
        log.info("appRoleSecretIdWrappingToken=" + appRoleSecretIdWrappingToken);

        clientTokenWrappingToken = fetchWrappingToken(
                execVault(format("vault token create -wrap-ttl=120s -ttl=10m -policy=%s", VAULT_POLICY)));
        log.info("clientTokenWrappingToken=" + clientTokenWrappingToken);

        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, WRAPPING_TEST_PATH, USERPASS_WRAPPING_TOKEN_PASSWORD_KEY,
                VAULT_AUTH_USERPASS_PASSWORD));
        passwordKvv1WrappingToken = fetchWrappingToken(
                execVault(format("vault kv get -wrap-ttl=120s %s/%s", SECRET_PATH_V1, WRAPPING_TEST_PATH)));
        log.info("passwordKvv1WrappingToken=" + passwordKvv1WrappingToken);

        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, WRAPPING_TEST_PATH, USERPASS_WRAPPING_TOKEN_PASSWORD_KEY,
                VAULT_AUTH_USERPASS_PASSWORD));
        passwordKvv2WrappingToken = fetchWrappingToken(
                execVault(format("vault kv get -wrap-ttl=120s %s/%s", SECRET_PATH_V2, WRAPPING_TEST_PATH)));
        log.info("passwordKvv2WrappingToken=" + passwordKvv2WrappingToken);
        anotherPasswordKvv2WrappingToken = fetchWrappingToken(
                execVault(format("vault kv get -wrap-ttl=120s %s/%s", SECRET_PATH_V2, WRAPPING_TEST_PATH)));
        log.info("anotherPasswordKvv2WrappingToken=" + anotherPasswordKvv2WrappingToken);

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
        execVault(format("vault write transit/keys/%s type=rsa-2048", SIGN_KEY2_NAME));
        execVault(format("vault write transit/keys/%s type=ed25519 derived=true", SIGN_DERIVATION_KEY_NAME));

        execVault("vault write transit/keys/jws type=ecdsa-p256");

        // TOTP

        execVault("vault secrets enable totp");
    }

    private String readResourceContent(String path) throws IOException {
        int count;
        byte[] buf = new byte[512];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        try {
            while ((count = in.read(buf)) > 0) {
                baos.write(buf, 0, count);
            }
        } finally {
            in.close();
        }
        return new String(baos.toByteArray());
    }

    private String fetchWrappingToken(String out) {
        Pattern wrappingTokenPattern = Pattern.compile("^wrapping_token:\\s+([^\\s]+)$", MULTILINE);
        Matcher matcher = wrappingTokenPattern.matcher(out);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new RuntimeException("unable to find wrapping_token in " + out);
        }
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

    private String execPostgres(String command) throws IOException, InterruptedException {
        String[] cmd = { "/bin/sh", "-c", command + " > " + CONTAINER_TMP_CMD + OUT_FILE };
        return exec(postgresContainer, command, cmd, HOST_POSTGRES_TMP_CMD + OUT_FILE);
    }

    private String execVault(String command) throws IOException, InterruptedException {
        String[] cmd = createVaultCommand(command + " > " + CONTAINER_TMP_CMD + OUT_FILE);
        return exec(vaultContainer, command, cmd, HOST_VAULT_TMP_CMD + OUT_FILE);
    }

    private String exec(GenericContainer container, String command, String[] cmd, String outFile)
            throws IOException, InterruptedException {
        exec(container, cmd);
        String out = new String(Files.readAllBytes(Paths.get(outFile)), UTF_8);
        log.info("> " + command + "\n" + out);
        return out;
    }

    private static Container.ExecResult exec(GenericContainer container, String[] cmd)
            throws IOException, InterruptedException {

        Container.ExecResult execResult = container.execInContainer(cmd);

        if (execResult.getExitCode() != 0) {
            throw new RuntimeException(
                    "command " + Arrays.asList(cmd) + " failed with exit code " + execResult.getExitCode() + "\n"
                            + execResult.getStderr());
        }
        return execResult;
    }

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

        // VaultManager.getInstance().reset();
    }
}
