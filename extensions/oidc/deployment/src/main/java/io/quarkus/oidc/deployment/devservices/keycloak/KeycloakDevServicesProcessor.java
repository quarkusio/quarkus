package io.quarkus.oidc.deployment.devservices.keycloak;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.oidc.deployment.OidcBuildStep.IsEnabled;
import io.quarkus.oidc.deployment.devservices.OidcDevServicesBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;

public class KeycloakDevServicesProcessor {
    static volatile DevServicesConfig capturedDevServicesConfiguration;
    static volatile Vertx vertxInstance;

    private static final Logger LOG = Logger.getLogger(KeycloakDevServicesProcessor.class);

    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String APPLICATION_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    private static final String KEYCLOAK_URL_KEY = "keycloak.url";

    private static final int KEYCLOAK_EXPOSED_PORT = 8080;
    private static final String KEYCLOAK_DOCKER_REALM_PATH = "/tmp/realm.json";
    private static final String KEYCLOAK_USER_PROP = "KEYCLOAK_USER";
    private static final String KEYCLOAK_PASSWORD_PROP = "KEYCLOAK_PASSWORD";
    private static final String KEYCLOAK_VENDOR_PROP = "DB_VENDOR";
    private static final String KEYCLOAK_IMPORT_PROP = "KEYCLOAK_IMPORT";
    private static final String KEYCLOAK_ADMIN_USER = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "admin";
    private static final String KEYCLOAK_DB_VENDOR = "H2";
    private static final String OIDC_USERS = "oidc.users";

    private static volatile List<Closeable> closeables;
    private static volatile boolean first = true;
    private static volatile String capturedKeycloakUrl;
    private static volatile FileTime capturedRealmFileLastModifiedDate;
    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = IsEnabled.class)
    public DevServicesConfigBuildItem startKeycloakContainer(LaunchModeBuildItem launchMode,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfiguration,
            BuildProducer<DevServicesNativeConfigResultBuildItem> devServices,
            Optional<OidcDevServicesBuildItem> oidcProviderBuildItem,
            KeycloakBuildTimeConfig config) {

        if (oidcProviderBuildItem.isPresent()) {
            // Dev Services for the alternative OIDC provider are enabled
            return null;
        }

        DevServicesConfig currentDevServicesConfiguration = config.devservices;
        // Figure out if we need to shut down and restart any existing Keycloak container
        // if not and the Keycloak container has already started we just return
        if (closeables != null) {
            boolean restartRequired = !currentDevServicesConfiguration.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                FileTime currentRealmFileLastModifiedDate = getRealmFileLastModifiedDate(
                        currentDevServicesConfiguration.realmPath);
                if (currentRealmFileLastModifiedDate != null
                        && !currentRealmFileLastModifiedDate.equals(capturedRealmFileLastModifiedDate)) {
                    restartRequired = true;
                    capturedRealmFileLastModifiedDate = currentRealmFileLastModifiedDate;
                }
            }
            if (!restartRequired) {
                return prepareConfiguration(false, runTimeConfiguration, devServices);
            }
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (Throwable e) {
                    LOG.error("Failed to stop Keycloak container", e);
                }
            }
            closeables = null;
            capturedDevServicesConfiguration = null;
            capturedKeycloakUrl = null;
        }
        capturedDevServicesConfiguration = currentDevServicesConfiguration;

        StartResult startResult = startContainer();
        if (startResult == null) {
            return null;
        }

        closeables = Collections.singletonList(startResult.closeable);

        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (closeables != null) {
                        for (Closeable closeable : closeables) {
                            try {
                                closeable.close();
                            } catch (Throwable t) {
                                LOG.error("Failed to stop Keycloak container", t);
                            }
                        }
                    }
                    if (vertxInstance != null) {
                        try {
                            vertxInstance.close();
                        } catch (Throwable t) {
                            LOG.error("Failed to close Vertx instance", t);
                        }
                    }
                    first = true;
                    closeables = null;
                    capturedDevServicesConfiguration = null;
                    vertxInstance = null;
                    capturedRealmFileLastModifiedDate = null;
                }
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
            Thread closeHookThread = new Thread(closeTask, "Keycloak container shutdown thread");
            Runtime.getRuntime().addShutdownHook(closeHookThread);
            ((QuarkusClassLoader) cl.parent()).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    Runtime.getRuntime().removeShutdownHook(closeHookThread);
                }
            });
        }

        capturedKeycloakUrl = startResult.url + "/auth";
        if (vertxInstance == null) {
            vertxInstance = Vertx.vertx();
        }
        capturedRealmFileLastModifiedDate = getRealmFileLastModifiedDate(capturedDevServicesConfiguration.realmPath);
        return prepareConfiguration(!startResult.realmFileExists, runTimeConfiguration, devServices);
    }

    private DevServicesConfigBuildItem prepareConfiguration(boolean createRealm,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfiguration,
            BuildProducer<DevServicesNativeConfigResultBuildItem> devServices) {
        final String authServerUrl = capturedKeycloakUrl + "/realms/" + capturedDevServicesConfiguration.realmName;

        String oidcClientId = getOidcClientId();
        String oidcClientSecret = getOidcClientSecret();
        String oidcApplicationType = getOidcApplicationType();
        Map<String, String> users = getUsers(capturedDevServicesConfiguration.users);

        if (createRealm) {
            createRealm(capturedKeycloakUrl, users, oidcClientId, oidcClientSecret);
        }

        runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(KEYCLOAK_URL_KEY, capturedKeycloakUrl));
        runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(AUTH_SERVER_URL_CONFIG_KEY, authServerUrl));
        runTimeConfiguration
                .produce(new RunTimeConfigurationDefaultBuildItem(APPLICATION_TYPE_CONFIG_KEY, oidcApplicationType));
        runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(CLIENT_ID_CONFIG_KEY, oidcClientId));
        runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(CLIENT_SECRET_CONFIG_KEY, oidcClientSecret));

        devServices.produce(new DevServicesNativeConfigResultBuildItem(KEYCLOAK_URL_KEY, capturedKeycloakUrl));
        devServices.produce(new DevServicesNativeConfigResultBuildItem(AUTH_SERVER_URL_CONFIG_KEY, capturedKeycloakUrl));
        devServices.produce(new DevServicesNativeConfigResultBuildItem(APPLICATION_TYPE_CONFIG_KEY, oidcApplicationType));
        devServices.produce(new DevServicesNativeConfigResultBuildItem(CLIENT_ID_CONFIG_KEY, oidcClientId));
        devServices.produce(new DevServicesNativeConfigResultBuildItem(CLIENT_SECRET_CONFIG_KEY, oidcClientSecret));

        Map<String, Object> configProperties = new HashMap<>();
        configProperties.put(KEYCLOAK_URL_KEY, capturedKeycloakUrl);
        configProperties.put(AUTH_SERVER_URL_CONFIG_KEY, authServerUrl);
        configProperties.put(APPLICATION_TYPE_CONFIG_KEY, oidcApplicationType);
        configProperties.put(CLIENT_ID_CONFIG_KEY, oidcClientId);
        configProperties.put(CLIENT_SECRET_CONFIG_KEY, oidcClientSecret);
        configProperties.put(OIDC_USERS, users);

        return new DevServicesConfigBuildItem(configProperties);
    }

    private StartResult startContainer() {
        if (!capturedDevServicesConfiguration.enabled) {
            // explicitly disabled
            LOG.error("Not starting devservices for Keycloak as it has been disabled in the config");
            return null;
        }
        if (!isOidcTenantEnabled()) {
            LOG.debug("Not starting devservices for Keycloak as 'quarkus.oidc.tenant.enabled' is false");
            return null;
        }
        if (ConfigUtils.isPropertyPresent(AUTH_SERVER_URL_CONFIG_KEY)) {
            LOG.debug("Not starting devservices for Keycloak as 'quarkus.oidc.auth-server-url' has been provided");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            LOG.warn("Please configure 'quarkus.oidc.auth-server-url' or get a working docker instance");
            return null;
        }

        String imageName = capturedDevServicesConfiguration.imageName.get();
        DockerImageName dockerImageName = DockerImageName.parse(imageName)
                .asCompatibleSubstituteFor(imageName);
        FixedPortOidcContainer oidcContainer = new FixedPortOidcContainer(dockerImageName,
                capturedDevServicesConfiguration.port,
                capturedDevServicesConfiguration.realmPath);

        oidcContainer.start();

        String url = "http://" + oidcContainer.getHost() + ":" + oidcContainer.getPort();
        return new StartResult(url,
                oidcContainer.realmFileExists,
                new Closeable() {
                    @Override
                    public void close() {
                        oidcContainer.close();
                    }
                });
    }

    private static class StartResult {
        private final String url;
        private final boolean realmFileExists;
        private final Closeable closeable;

        public StartResult(String url, boolean realmFileExists, Closeable closeable) {
            this.url = url;
            this.realmFileExists = realmFileExists;
            this.closeable = closeable;
        }
    }

    private static class FixedPortOidcContainer extends GenericContainer {
        OptionalInt fixedExposedPort;
        Optional<String> realm;
        boolean realmFileExists;

        public FixedPortOidcContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, Optional<String> realm) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.realm = realm;
        }

        @Override
        protected void configure() {
            super.configure();
            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), KEYCLOAK_EXPOSED_PORT);
            } else {
                addExposedPort(KEYCLOAK_EXPOSED_PORT);
            }

            addEnv(KEYCLOAK_USER_PROP, KEYCLOAK_ADMIN_USER);
            addEnv(KEYCLOAK_PASSWORD_PROP, KEYCLOAK_ADMIN_PASSWORD);
            addEnv(KEYCLOAK_VENDOR_PROP, KEYCLOAK_DB_VENDOR);

            if (realm.isPresent()) {
                if (Thread.currentThread().getContextClassLoader().getResource(realm.get()) != null) {
                    realmFileExists = true;
                    withClasspathResourceMapping(realm.get(), KEYCLOAK_DOCKER_REALM_PATH, BindMode.READ_ONLY);
                    addEnv(KEYCLOAK_IMPORT_PROP, KEYCLOAK_DOCKER_REALM_PATH);
                } else {
                    Path filePath = Paths.get(realm.get());
                    if (Files.exists(filePath)) {
                        realmFileExists = true;
                        withFileSystemBind(realm.get(), KEYCLOAK_DOCKER_REALM_PATH, BindMode.READ_ONLY);
                        addEnv(KEYCLOAK_IMPORT_PROP, KEYCLOAK_DOCKER_REALM_PATH);
                    } else {
                        LOG.debugf("Realm %s resource is not available", realm.get());
                    }
                }
            }

            super.setWaitStrategy(Wait.forHttp("/auth").forPort(KEYCLOAK_EXPOSED_PORT));
        }

        public int getPort() {
            if (fixedExposedPort.isPresent()) {
                return fixedExposedPort.getAsInt();
            }
            return getFirstMappedPort();
        }
    }

    private FileTime getRealmFileLastModifiedDate(Optional<String> realm) {
        if (realm.isPresent()) {
            Path realmPath = Paths.get(realm.get());
            try {
                return Files.getLastModifiedTime(realmPath);
            } catch (IOException ex) {
                LOG.tracef("Unable to get the last modified date of the realm file %s", realmPath);
            }
        }
        return null;
    }

    private void createRealm(String keycloakUrl, Map<String, String> users, String oidcClientId, String oidcClientSecret) {
        RealmRepresentation realm = createRealmRep();

        realm.getClients().add(createClient(oidcClientId, oidcClientSecret));
        for (Map.Entry<String, String> entry : users.entrySet()) {
            realm.getUsers().add(createUser(entry.getKey(), entry.getValue(), getUserRoles(entry.getKey())));
        }

        WebClient client = KeycloakDevServicesUtils.createWebClient();
        try {
            String token = KeycloakDevServicesUtils.getPasswordAccessToken(client,
                    keycloakUrl + "/realms/master/protocol/openid-connect/token",
                    "admin-cli", null, "admin", "admin", capturedDevServicesConfiguration.webClienTimeout);

            client.postAbs(keycloakUrl + "/admin/realms")
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                    .sendBuffer(Buffer.buffer().appendString(JsonSerialization.writeValueAsString(realm)))
                    .await().atMost(capturedDevServicesConfiguration.webClienTimeout);
        } catch (Throwable t) {
            LOG.errorf("Realm %s can not be created: %s", realm.getRealm(), t.getMessage());
        } finally {
            client.close();
        }
    }

    private Map<String, String> getUsers(Map<String, String> configuredUsers) {
        if (configuredUsers.isEmpty()) {
            Map<String, String> users = new LinkedHashMap<String, String>();
            users.put("alice", "alice");
            users.put("bob", "bob");
            return users;
        } else {
            return configuredUsers;
        }
    }

    private String[] getUserRoles(String user) {
        String roles = capturedDevServicesConfiguration.roles.get(user);
        return roles == null ? ("alice".equals(user) ? new String[] { "admin", "user" } : new String[] { "user" })
                : roles.split(",");
    }

    private RealmRepresentation createRealmRep() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(capturedDevServicesConfiguration.realmName);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        if (capturedDevServicesConfiguration.roles.isEmpty()) {
            realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
            realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        } else {
            for (String role : capturedDevServicesConfiguration.roles.values()) {
                realm.getRoles().getRealm().add(new RoleRepresentation(role, null, false));
            }
        }
        return realm;
    }

    private ClientRepresentation createClient(String clientId, String oidcClientSecret) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setRedirectUris(Arrays.asList("*"));
        client.setPublicClient(false);
        client.setSecret(oidcClientSecret);
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setImplicitFlowEnabled(true);
        client.setEnabled(true);
        client.setRedirectUris(Arrays.asList("*"));
        client.setDefaultClientScopes(Arrays.asList("microprofile-jwt"));

        return client;
    }

    private UserRepresentation createUser(String username, String password, String... realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(Arrays.asList(realmRoles));

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        user.getCredentials().add(credential);

        return user;
    }

    private static boolean isOidcTenantEnabled() {
        return ConfigProvider.getConfig().getOptionalValue(TENANT_ENABLED_CONFIG_KEY, Boolean.class).orElse(true);
    }

    private static String getOidcApplicationType() {
        return ConfigProvider.getConfig().getOptionalValue(APPLICATION_TYPE_CONFIG_KEY, String.class).orElse("service");
    }

    private static String getOidcClientId() {
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_ID_CONFIG_KEY, String.class).orElse("quarkus-app");
    }

    private static String getOidcClientSecret() {
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_SECRET_CONFIG_KEY, String.class).orElse("secret");
    }
}
