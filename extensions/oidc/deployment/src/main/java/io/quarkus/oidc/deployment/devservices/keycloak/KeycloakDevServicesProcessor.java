package io.quarkus.oidc.deployment.devservices.keycloak;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.oidc.deployment.OidcBuildStep.IsEnabled;
import io.quarkus.oidc.deployment.devservices.OidcDevServicesBuildItem;
import io.quarkus.oidc.deployment.devservices.OidcDevServicesUtils;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
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
    private static final String KEYCLOAK_REALM_KEY = "keycloak.realm";

    private static final int KEYCLOAK_PORT = 8080;
    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String KEYCLOAK_DOCKER_REALM_PATH = "/tmp/realm.json";
    private static final String KEYCLOAK_USER_PROP = "KEYCLOAK_USER";
    private static final String KEYCLOAK_PASSWORD_PROP = "KEYCLOAK_PASSWORD";
    private static final String KEYCLOAK_VENDOR_PROP = "DB_VENDOR";
    private static final String KEYCLOAK_IMPORT_PROP = "KEYCLOAK_IMPORT";
    private static final String KEYCLOAK_ADMIN_USER = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "admin";
    private static final String KEYCLOAK_DB_VENDOR = "H2";
    private static final String OIDC_USERS = "oidc.users";

    /**
     * Label to add to shared Dev Service for Keycloak running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-keycloak";
    private static final ContainerLocator keycloakDevModeContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL,
            KEYCLOAK_PORT);

    private static volatile List<Closeable> closeables;
    private static volatile boolean first = true;
    private static volatile String capturedKeycloakUrl;
    private static volatile FileTime capturedRealmFileLastModifiedDate;
    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);
    private static volatile KeycloakDevServicesConfigBuildItem existingDevServiceConfig;

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = { IsEnabled.class, GlobalDevServicesConfig.Enabled.class })
    public KeycloakDevServicesConfigBuildItem startKeycloakContainer(
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            BuildProducer<DevServicesConfigResultBuildItem> devServices,
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
                return existingDevServiceConfig;
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
            existingDevServiceConfig = null;
        }
        capturedDevServicesConfiguration = currentDevServicesConfiguration;

        StartResult startResult = startContainer(devServicesSharedNetworkBuildItem.isPresent());
        if (startResult == null) {
            return null;
        }

        closeables = startResult.closeable != null ? Collections.singletonList(startResult.closeable) : null;

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
        }

        capturedKeycloakUrl = startResult.url + "/auth";
        if (vertxInstance == null) {
            vertxInstance = Vertx.vertx();
        }
        capturedRealmFileLastModifiedDate = getRealmFileLastModifiedDate(capturedDevServicesConfiguration.realmPath);

        LOG.info("Dev Services for Keycloak started.");

        return prepareConfiguration(capturedDevServicesConfiguration.createRealm && startResult.createDefaultRealm,
                startResult.realmNameToUse, devServices, startResult.shared);
    }

    private KeycloakDevServicesConfigBuildItem prepareConfiguration(boolean createRealm, String realmNameToUse,
            BuildProducer<DevServicesConfigResultBuildItem> devServices, boolean shared) {
        final String realmName = realmNameToUse != null ? realmNameToUse : getDefaultRealmName();
        final String authServerUrl = capturedKeycloakUrl + "/realms/" + realmName;

        String oidcClientId = getOidcClientId();
        String oidcClientSecret = getOidcClientSecret();
        String oidcApplicationType = getOidcApplicationType();
        Map<String, String> users = getUsers(capturedDevServicesConfiguration.users);

        if (createRealm) {
            createRealm(capturedKeycloakUrl, users, oidcClientId, oidcClientSecret);
        }
        devServices.produce(new DevServicesConfigResultBuildItem(KEYCLOAK_URL_KEY, capturedKeycloakUrl));
        devServices.produce(new DevServicesConfigResultBuildItem(AUTH_SERVER_URL_CONFIG_KEY, authServerUrl));
        devServices.produce(new DevServicesConfigResultBuildItem(APPLICATION_TYPE_CONFIG_KEY, oidcApplicationType));
        devServices.produce(new DevServicesConfigResultBuildItem(CLIENT_ID_CONFIG_KEY, oidcClientId));
        devServices.produce(new DevServicesConfigResultBuildItem(CLIENT_SECRET_CONFIG_KEY, oidcClientSecret));

        Map<String, Object> configProperties = new HashMap<>();
        configProperties.put(KEYCLOAK_URL_KEY, capturedKeycloakUrl);
        configProperties.put(KEYCLOAK_REALM_KEY, realmName);
        configProperties.put(AUTH_SERVER_URL_CONFIG_KEY, authServerUrl);
        configProperties.put(APPLICATION_TYPE_CONFIG_KEY, oidcApplicationType);
        configProperties.put(CLIENT_ID_CONFIG_KEY, oidcClientId);
        configProperties.put(CLIENT_SECRET_CONFIG_KEY, oidcClientSecret);
        configProperties.put(OIDC_USERS, users);

        existingDevServiceConfig = new KeycloakDevServicesConfigBuildItem(configProperties);
        return existingDevServiceConfig;
    }

    private String getDefaultRealmName() {
        return capturedDevServicesConfiguration.realmName.orElse("quarkus");
    }

    private StartResult startContainer(boolean useSharedContainer) {
        if (!capturedDevServicesConfiguration.enabled) {
            // explicitly disabled
            LOG.debug("Not starting Dev Services for Keycloak as it has been disabled in the config");
            return null;
        }
        if (!isOidcTenantEnabled()) {
            LOG.debug("Not starting Dev Services for Keycloak as 'quarkus.oidc.tenant.enabled' is false");
            return null;
        }
        if (ConfigUtils.isPropertyPresent(AUTH_SERVER_URL_CONFIG_KEY)) {
            LOG.debug("Not starting Dev Services for Keycloak as 'quarkus.oidc.auth-server-url' has been provided");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            LOG.warn("Please configure 'quarkus.oidc.auth-server-url' or get a working docker instance");
            return null;
        }

        final Optional<ContainerAddress> maybeContainerAddress = keycloakDevModeContainerLocator.locateContainer(
                capturedDevServicesConfiguration.serviceName,
                capturedDevServicesConfiguration.shared,
                LaunchMode.current());

        final Supplier<StartResult> defaultKeycloakContainerSupplier = () -> {
            String imageName = capturedDevServicesConfiguration.imageName;
            DockerImageName dockerImageName = DockerImageName.parse(imageName)
                    .asCompatibleSubstituteFor(imageName);
            QuarkusOidcContainer oidcContainer = new QuarkusOidcContainer(dockerImageName,
                    capturedDevServicesConfiguration.port,
                    useSharedContainer,
                    capturedDevServicesConfiguration.realmName,
                    capturedDevServicesConfiguration.realmPath,
                    capturedDevServicesConfiguration.serviceName,
                    capturedDevServicesConfiguration.shared,
                    capturedDevServicesConfiguration.javaOpts);

            oidcContainer.start();

            String url = "http://" + oidcContainer.getHost() + ":" + oidcContainer.getPort();
            return new StartResult(url,
                    !oidcContainer.realmFileExists,
                    oidcContainer.realmNameToUse,
                    new Closeable() {
                        @Override
                        public void close() {
                            oidcContainer.close();

                            LOG.info("Dev Services for Keycloak shut down.");
                        }
                    },
                    false);
        };

        return maybeContainerAddress
                .map(containerAddress -> new StartResult(getSharedContainerUrl(containerAddress), false,
                        null, null, true))
                .orElseGet(defaultKeycloakContainerSupplier);
    }

    private String getSharedContainerUrl(ContainerAddress containerAddress) {
        return "http://" + ("0.0.0.0".equals(containerAddress.getHost()) ? "localhost" : containerAddress.getHost())
                + ":" + containerAddress.getPort();
    }

    private static class StartResult {
        private final String url;
        private final boolean createDefaultRealm;
        private String realmNameToUse;
        private final Closeable closeable;
        private final boolean shared;

        public StartResult(String url, boolean createDefaultRealm, String realmNameToUse, Closeable closeable, boolean shared) {
            this.url = url;
            this.createDefaultRealm = createDefaultRealm;
            this.realmNameToUse = realmNameToUse;
            this.closeable = closeable;
            this.shared = shared;
        }
    }

    private static class QuarkusOidcContainer extends GenericContainer {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;
        private final Optional<String> configuredRealmName;
        private final Optional<String> realmPath;
        private final String containerLabelValue;
        private final Optional<String> javaOpts;
        private final boolean sharedContainer;
        private boolean realmFileExists;
        private String hostName = null;
        private String realmNameToUse;

        public QuarkusOidcContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, boolean useSharedNetwork,
                Optional<String> configuredRealmName, Optional<String> realmPath, String containerLabelValue,
                boolean sharedContainer, Optional<String> javaOpts) {
            super(dockerImageName);
            this.fixedExposedPort = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            this.configuredRealmName = configuredRealmName;
            this.realmPath = realmPath;
            this.containerLabelValue = containerLabelValue;
            this.sharedContainer = sharedContainer;
            this.javaOpts = javaOpts;
        }

        @Override
        protected void configure() {
            super.configure();
            if (useSharedNetwork) {
                // When a shared network is requested for the launched containers, we need to configure
                // the container to use it. We also need to create a hostname that will be applied to the returned
                // Keycloak URL
                setNetwork(Network.SHARED);
                hostName = "keycloak-" + Base58.randomString(5);
                setNetworkAliases(Collections.singletonList(hostName));
            } else {
                if (fixedExposedPort.isPresent()) {
                    addFixedExposedPort(fixedExposedPort.getAsInt(), KEYCLOAK_PORT);
                } else {
                    addExposedPort(KEYCLOAK_PORT);
                }
            }

            if (sharedContainer && LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                withLabel(DEV_SERVICE_LABEL, containerLabelValue);
            }

            addEnv(KEYCLOAK_USER_PROP, KEYCLOAK_ADMIN_USER);
            addEnv(KEYCLOAK_PASSWORD_PROP, KEYCLOAK_ADMIN_PASSWORD);
            addEnv(KEYCLOAK_VENDOR_PROP, KEYCLOAK_DB_VENDOR);
            if (javaOpts.isPresent()) {
                addEnv(JAVA_OPTS, javaOpts.get());
            }

            if (realmPath.isPresent()) {
                URL realmPathUrl = null;
                if ((realmPathUrl = Thread.currentThread().getContextClassLoader().getResource(realmPath.get())) != null) {
                    realmFileExists = true;
                    realmNameToUse = configuredRealmName.isPresent() ? null
                            : getRealmNameFromRealmFile(realmPathUrl, realmPath.get());
                    withClasspathResourceMapping(realmPath.get(), KEYCLOAK_DOCKER_REALM_PATH, BindMode.READ_ONLY);
                    addEnv(KEYCLOAK_IMPORT_PROP, KEYCLOAK_DOCKER_REALM_PATH);
                } else {
                    Path filePath = Paths.get(realmPath.get());
                    if (Files.exists(filePath)) {
                        realmFileExists = true;
                        realmNameToUse = configuredRealmName.isPresent() ? null
                                : getRealmNameFromRealmFile(filePath.toUri(), realmPath.get());
                        withFileSystemBind(realmPath.get(), KEYCLOAK_DOCKER_REALM_PATH, BindMode.READ_ONLY);
                        addEnv(KEYCLOAK_IMPORT_PROP, KEYCLOAK_DOCKER_REALM_PATH);
                    } else {
                        LOG.debugf("Realm %s resource is not available", realmPath.get());
                    }
                }
            }

            super.setWaitStrategy(Wait.forHttp("/auth").forPort(KEYCLOAK_PORT));
        }

        private String getRealmNameFromRealmFile(URI uri, String realmPath) {
            try {
                return getRealmNameFromRealmFile(uri.toURL(), realmPath);
            } catch (MalformedURLException ex) {
                // Will not happen as this method is called only when it is confirmed the file exists
                throw new RuntimeException(ex);
            }
        }

        private String getRealmNameFromRealmFile(URL url, String realmPath) {
            try {
                try (InputStream is = url.openStream()) {
                    JsonObject realmJson = new JsonObject(io.vertx.core.buffer.Buffer.buffer(is.readAllBytes()));
                    String realmName = realmJson.getString("realm");
                    return realmName;
                }
            } catch (IOException ex) {
                LOG.errorf("Realm %s resource can not be opened: %s", realmPath, ex.getMessage());
            }
            return null;
        }

        @Override
        public String getHost() {
            if (useSharedNetwork) {
                return hostName;
            }
            return super.getHost();
        }

        public int getPort() {
            if (useSharedNetwork) {
                return KEYCLOAK_PORT;
            }
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

        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
        try {
            String token = OidcDevServicesUtils.getPasswordAccessToken(client,
                    keycloakUrl + "/realms/master/protocol/openid-connect/token",
                    "admin-cli", null, "admin", "admin", capturedDevServicesConfiguration.webClienTimeout);

            HttpResponse<Buffer> response = client.postAbs(keycloakUrl + "/admin/realms")
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                    .sendBuffer(Buffer.buffer().appendString(JsonSerialization.writeValueAsString(realm)))
                    .await().atMost(capturedDevServicesConfiguration.webClienTimeout);

            if (response.statusCode() > 299) {
                LOG.errorf("Realm %s can not be created %d - %s ", realm.getRealm(), response.statusCode(),
                        response.statusMessage());
            }
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

        realm.setRealm(getDefaultRealmName());
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
            List<String> distinctRoles = capturedDevServicesConfiguration.roles.values().stream().distinct()
                    .collect(Collectors.toList());
            for (String role : distinctRoles) {
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
