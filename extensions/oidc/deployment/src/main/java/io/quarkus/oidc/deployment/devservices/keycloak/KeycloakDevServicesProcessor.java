package io.quarkus.oidc.deployment.devservices.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.oidc.deployment.OidcBuildStep.IsEnabled;
import io.quarkus.oidc.deployment.devservices.OidcDevServicesBuildItem;
import io.quarkus.oidc.deployment.devservices.OidcDevServicesUtils;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class KeycloakDevServicesProcessor {
    static volatile Vertx vertxInstance;

    private static final Logger LOG = Logger.getLogger(KeycloakDevServicesProcessor.class);

    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String PROVIDER_CONFIG_KEY = CONFIG_PREFIX + "provider";
    // avoid the Quarkus prefix in order to prevent warnings when the application starts in container integration tests
    private static final String CLIENT_AUTH_SERVER_URL_CONFIG_KEY = "client." + CONFIG_PREFIX + "auth-server-url";
    private static final String APPLICATION_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    private static final String KEYCLOAK_URL_KEY = "keycloak.url";

    private static final String KEYCLOAK_CONTAINER_NAME = "keycloak";
    private static final int KEYCLOAK_PORT = 8080;

    private static final String KEYCLOAK_LEGACY_IMAGE_VERSION_PART = "-legacy";

    private static final String KEYCLOAK_ADMIN_USER = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "admin";

    // Properties recognized by Wildfly-powered Keycloak
    private static final String KEYCLOAK_WILDFLY_FRONTEND_URL = "KEYCLOAK_FRONTEND_URL";
    private static final String KEYCLOAK_WILDFLY_USER_PROP = "KEYCLOAK_USER";
    private static final String KEYCLOAK_WILDFLY_PASSWORD_PROP = "KEYCLOAK_PASSWORD";
    private static final String KEYCLOAK_WILDFLY_IMPORT_PROP = "KEYCLOAK_IMPORT";
    private static final String KEYCLOAK_WILDFLY_DB_VENDOR = "H2";
    private static final String KEYCLOAK_WILDFLY_VENDOR_PROP = "DB_VENDOR";

    // Properties recognized by Quarkus-powered Keycloak
    private static final String KEYCLOAK_QUARKUS_HOSTNAME = "KC_HOSTNAME";
    private static final String KEYCLOAK_QUARKUS_ADMIN_PROP = "KEYCLOAK_ADMIN";
    private static final String KEYCLOAK_QUARKUS_ADMIN_PASSWORD_PROP = "KEYCLOAK_ADMIN_PASSWORD";
    private static final String KEYCLOAK_QUARKUS_START_CMD = "start --http-enabled=true --hostname-strict=false --hostname-strict-https=false";

    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String KEYCLOAK_DOCKER_REALM_PATH = "/tmp/realm.json";
    private static final String OIDC_USERS = "oidc.users";

    /**
     * Label to add to shared Dev Service for Keycloak running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-keycloak";
    private static final ContainerLocator keycloakDevModeContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL,
            KEYCLOAK_PORT);

    private static volatile RunningDevService devService;
    static volatile DevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;
    private static volatile FileTime capturedRealmFileLastModifiedDate;

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = { IsEnabled.class, GlobalDevServicesConfig.Enabled.class })
    public DevServicesResultBuildItem startKeycloakContainer(
            DockerStatusBuildItem dockerStatusBuildItem,
            BuildProducer<KeycloakDevServicesConfigBuildItem> keycloakBuildItemBuildProducer,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<OidcDevServicesBuildItem> oidcProviderBuildItem,
            KeycloakBuildTimeConfig config,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        if (oidcProviderBuildItem.isPresent()) {
            // Dev Services for the alternative OIDC provider are enabled
            return null;
        }

        DevServicesConfig currentDevServicesConfiguration = config.devservices;
        // Figure out if we need to shut down and restart any existing Keycloak container
        // if not and the Keycloak container has already started we just return
        if (devService != null) {
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
                DevServicesResultBuildItem result = devService.toBuildItem();
                String usersString = result.getConfig().get(OIDC_USERS);
                Map<String, String> users = (usersString == null || usersString.isBlank()) ? Map.of()
                        : Arrays.stream(usersString.split(","))
                                .map(s -> s.split("=")).collect(Collectors.toMap(s -> s[0], s -> s[1]));
                keycloakBuildItemBuildProducer
                        .produce(new KeycloakDevServicesConfigBuildItem(result.getConfig(), Map.of(OIDC_USERS, users)));
                return result;
            }
            try {
                devService.close();
            } catch (Throwable e) {
                LOG.error("Failed to stop Keycloak container", e);
            }
            devService = null;
            capturedDevServicesConfiguration = null;
        }
        capturedDevServicesConfiguration = currentDevServicesConfiguration;
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "KeyCloak Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        if (vertxInstance == null) {
            vertxInstance = Vertx.vertx();
        }
        try {
            RunningDevService newDevService = startContainer(dockerStatusBuildItem, keycloakBuildItemBuildProducer,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout);
            if (newDevService == null) {
                compressor.close();
                return null;
            }

            devService = newDevService;

            if (first) {
                first = false;
                Runnable closeTask = new Runnable() {
                    @Override
                    public void run() {
                        if (devService != null) {
                            try {
                                devService.close();
                            } catch (Throwable t) {
                                LOG.error("Failed to stop Keycloak container", t);
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
                        devService = null;
                        capturedDevServicesConfiguration = null;
                        vertxInstance = null;
                        capturedRealmFileLastModifiedDate = null;
                    }
                };
                closeBuildItem.addCloseTask(closeTask, true);
            }

            capturedRealmFileLastModifiedDate = getRealmFileLastModifiedDate(capturedDevServicesConfiguration.realmPath);
            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
        LOG.info("Dev Services for Keycloak started.");

        return devService.toBuildItem();
    }

    private String startURL(String host, Integer port, boolean isKeyCloakX) {
        return "http://" + host + ":" + port + (isKeyCloakX ? "" : "/auth");
    }

    private Map<String, String> prepareConfiguration(
            BuildProducer<KeycloakDevServicesConfigBuildItem> keycloakBuildItemBuildProducer, String internalURL,
            String hostURL, RealmRepresentation realmRep,
            boolean keycloakX) {
        final String realmName = realmRep != null ? realmRep.getRealm() : getDefaultRealmName();
        final String authServerInternalUrl = realmsURL(internalURL, realmName);

        String clientAuthServerBaseUrl = hostURL != null ? hostURL : internalURL;
        String clientAuthServerUrl = realmsURL(clientAuthServerBaseUrl, realmName);

        String oidcClientId = getOidcClientId();
        String oidcClientSecret = getOidcClientSecret();
        String oidcApplicationType = getOidcApplicationType();

        boolean createDefaultRealm = realmRep == null && capturedDevServicesConfiguration.createRealm;
        Map<String, String> users = getUsers(capturedDevServicesConfiguration.users, createDefaultRealm);

        if (createDefaultRealm) {
            createDefaultRealm(clientAuthServerBaseUrl, users, oidcClientId, oidcClientSecret);
        } else if (realmRep != null && keycloakX) {
            createRealm(clientAuthServerBaseUrl, realmRep);
        }

        Map<String, String> configProperties = new HashMap<>();
        configProperties.put(KEYCLOAK_URL_KEY, internalURL);
        configProperties.put(AUTH_SERVER_URL_CONFIG_KEY, authServerInternalUrl);
        configProperties.put(CLIENT_AUTH_SERVER_URL_CONFIG_KEY, clientAuthServerUrl);
        configProperties.put(APPLICATION_TYPE_CONFIG_KEY, oidcApplicationType);
        configProperties.put(CLIENT_ID_CONFIG_KEY, oidcClientId);
        configProperties.put(CLIENT_SECRET_CONFIG_KEY, oidcClientSecret);
        configProperties.put(OIDC_USERS, users.entrySet().stream()
                .map(e -> e.toString()).collect(Collectors.joining(",")));

        keycloakBuildItemBuildProducer
                .produce(new KeycloakDevServicesConfigBuildItem(configProperties, Map.of(OIDC_USERS, users)));

        return configProperties;
    }

    private String realmsURL(String baseURL, String realmName) {
        return baseURL + "/realms/" + realmName;
    }

    private String getDefaultRealmName() {
        return capturedDevServicesConfiguration.realmName.orElse("quarkus");
    }

    private RunningDevService startContainer(DockerStatusBuildItem dockerStatusBuildItem,
            BuildProducer<KeycloakDevServicesConfigBuildItem> keycloakBuildItemBuildProducer,
            boolean useSharedNetwork, Optional<Duration> timeout) {
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
        if (ConfigUtils.isPropertyPresent(PROVIDER_CONFIG_KEY)) {
            LOG.debug("Not starting Dev Services for Keycloak as 'quarkus.oidc.provider' has been provided");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            LOG.warn("Please configure 'quarkus.oidc.auth-server-url' or get a working docker instance");
            return null;
        }

        final Optional<ContainerAddress> maybeContainerAddress = keycloakDevModeContainerLocator.locateContainer(
                capturedDevServicesConfiguration.serviceName,
                capturedDevServicesConfiguration.shared,
                LaunchMode.current());

        String imageName = capturedDevServicesConfiguration.imageName;
        DockerImageName dockerImageName = DockerImageName.parse(imageName).asCompatibleSubstituteFor(imageName);

        final Supplier<RunningDevService> defaultKeycloakContainerSupplier = () -> {

            QuarkusOidcContainer oidcContainer = new QuarkusOidcContainer(dockerImageName,
                    capturedDevServicesConfiguration.port,
                    useSharedNetwork,
                    capturedDevServicesConfiguration.realmPath,
                    capturedDevServicesConfiguration.serviceName,
                    capturedDevServicesConfiguration.shared,
                    capturedDevServicesConfiguration.javaOpts);

            timeout.ifPresent(oidcContainer::withStartupTimeout);
            oidcContainer.start();

            String internalUrl = startURL(oidcContainer.getHost(), oidcContainer.getPort(), oidcContainer.keycloakX);
            String hostUrl = oidcContainer.useSharedNetwork
                    ? startURL("localhost", oidcContainer.fixedExposedPort.getAsInt(), oidcContainer.keycloakX)
                    : null;

            Map<String, String> configs = prepareConfiguration(keycloakBuildItemBuildProducer, internalUrl, hostUrl,
                    oidcContainer.realmRep,
                    oidcContainer.keycloakX);
            return new RunningDevService(KEYCLOAK_CONTAINER_NAME, oidcContainer.getContainerId(),
                    oidcContainer::close, configs);
        };

        return maybeContainerAddress
                .map(containerAddress -> {
                    // TODO: this probably needs to be addressed
                    Map<String, String> configs = prepareConfiguration(keycloakBuildItemBuildProducer,
                            getSharedContainerUrl(containerAddress),
                            getSharedContainerUrl(containerAddress), null, false);
                    return new RunningDevService(KEYCLOAK_CONTAINER_NAME, containerAddress.getId(), null, configs);
                })
                .orElseGet(defaultKeycloakContainerSupplier);
    }

    private static boolean isKeycloakX(DockerImageName dockerImageName) {
        return capturedDevServicesConfiguration.keycloakXImage.isPresent()
                ? capturedDevServicesConfiguration.keycloakXImage.get()
                : !dockerImageName.getVersionPart().endsWith(KEYCLOAK_LEGACY_IMAGE_VERSION_PART);
    }

    private String getSharedContainerUrl(ContainerAddress containerAddress) {
        return "http://" + ("0.0.0.0".equals(containerAddress.getHost()) ? "localhost" : containerAddress.getHost())
                + ":" + containerAddress.getPort();
    }

    private static class QuarkusOidcContainer extends GenericContainer<QuarkusOidcContainer> {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;
        private final Optional<String> realmPath;
        private final String containerLabelValue;
        private final Optional<String> javaOpts;
        private final boolean sharedContainer;
        private String hostName;
        private final boolean keycloakX;
        private RealmRepresentation realmRep;

        public QuarkusOidcContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, boolean useSharedNetwork,
                Optional<String> realmPath, String containerLabelValue,
                boolean sharedContainer, Optional<String> javaOpts) {
            super(dockerImageName);

            this.useSharedNetwork = useSharedNetwork;
            this.realmPath = realmPath;
            this.containerLabelValue = containerLabelValue;
            this.sharedContainer = sharedContainer;
            this.javaOpts = javaOpts;
            this.keycloakX = isKeycloakX(dockerImageName);

            if (sharedContainer && fixedExposedPort.isEmpty()) {
                // We need to know the port we are exposing when using the shared network, in order to be able to tell
                // Keycloak what the client URL is. This is necessary in order for Keycloak to create the proper 'issuer'
                // when creating tokens
                fixedExposedPort = OptionalInt.of(findRandomPort());
            }

            this.fixedExposedPort = fixedExposedPort;
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, "keycloak");
                if (keycloakX) {
                    addEnv(KEYCLOAK_QUARKUS_HOSTNAME, "localhost");
                } else {
                    addEnv(KEYCLOAK_WILDFLY_FRONTEND_URL, "http://localhost:" + fixedExposedPort.getAsInt());
                }
            }

            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), KEYCLOAK_PORT);
            } else {
                addExposedPort(KEYCLOAK_PORT);
            }

            if (sharedContainer && LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                withLabel(DEV_SERVICE_LABEL, containerLabelValue);
            }

            if (javaOpts.isPresent()) {
                addEnv(JAVA_OPTS, javaOpts.get());
            }

            if (keycloakX) {
                addEnv(KEYCLOAK_QUARKUS_ADMIN_PROP, KEYCLOAK_ADMIN_USER);
                addEnv(KEYCLOAK_QUARKUS_ADMIN_PASSWORD_PROP, KEYCLOAK_ADMIN_PASSWORD);
                withCommand(KEYCLOAK_QUARKUS_START_CMD
                        + (useSharedNetwork ? " --hostname-port=" + fixedExposedPort.getAsInt() : ""));
            } else {
                addEnv(KEYCLOAK_WILDFLY_USER_PROP, KEYCLOAK_ADMIN_USER);
                addEnv(KEYCLOAK_WILDFLY_PASSWORD_PROP, KEYCLOAK_ADMIN_PASSWORD);
                addEnv(KEYCLOAK_WILDFLY_VENDOR_PROP, KEYCLOAK_WILDFLY_DB_VENDOR);
            }

            if (realmPath.isPresent()) {
                URL realmPathUrl = null;
                if ((realmPathUrl = Thread.currentThread().getContextClassLoader().getResource(realmPath.get())) != null) {
                    realmRep = readRealmFile(realmPathUrl, realmPath.get());
                    if (!keycloakX) {
                        withClasspathResourceMapping(realmPath.get(), KEYCLOAK_DOCKER_REALM_PATH, BindMode.READ_ONLY);
                    }
                } else {
                    Path filePath = Paths.get(realmPath.get());
                    if (Files.exists(filePath)) {
                        if (!keycloakX) {
                            withFileSystemBind(realmPath.get(), KEYCLOAK_DOCKER_REALM_PATH, BindMode.READ_ONLY);
                        }
                        realmRep = readRealmFile(filePath.toUri(), realmPath.get());
                    } else {
                        LOG.debugf("Realm %s resource is not available", realmPath.get());
                    }
                }

            }

            if (realmRep != null && !keycloakX) {
                addEnv(KEYCLOAK_WILDFLY_IMPORT_PROP, KEYCLOAK_DOCKER_REALM_PATH);
            }

            LOG.infof("Using %s powered Keycloak distribution", keycloakX ? "Quarkus" : "WildFly");
            super.setWaitStrategy(Wait.forLogMessage(".*Keycloak.*started.*", 1));
        }

        private Integer findRandomPort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private RealmRepresentation readRealmFile(URI uri, String realmPath) {
            try {
                return readRealmFile(uri.toURL(), realmPath);
            } catch (MalformedURLException ex) {
                // Will not happen as this method is called only when it is confirmed the file exists
                throw new RuntimeException(ex);
            }
        }

        private RealmRepresentation readRealmFile(URL url, String realmPath) {
            try {
                try (InputStream is = url.openStream()) {
                    return JsonSerialization.readValue(is, RealmRepresentation.class);
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

    private void createDefaultRealm(String keycloakUrl, Map<String, String> users, String oidcClientId,
            String oidcClientSecret) {
        RealmRepresentation realm = createRealmRep();

        realm.getClients().add(createClient(oidcClientId, oidcClientSecret));
        for (Map.Entry<String, String> entry : users.entrySet()) {
            realm.getUsers().add(createUser(entry.getKey(), entry.getValue(), getUserRoles(entry.getKey())));
        }

        createRealm(keycloakUrl, realm);
    }

    private void createRealm(String keycloakUrl, RealmRepresentation realm) {
        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
        try {
            String token = OidcDevServicesUtils.getPasswordAccessToken(client,
                    keycloakUrl + "/realms/master/protocol/openid-connect/token",
                    "admin-cli", null, "admin", "admin", null, capturedDevServicesConfiguration.webClienTimeout);

            HttpResponse<Buffer> createRealmResponse = client.postAbs(keycloakUrl + "/admin/realms")
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                    .sendBuffer(Buffer.buffer().appendString(JsonSerialization.writeValueAsString(realm)))
                    .await().atMost(capturedDevServicesConfiguration.webClienTimeout);

            if (createRealmResponse.statusCode() > 299) {
                LOG.errorf("Realm %s can not be created %d - %s ", realm.getRealm(), createRealmResponse.statusCode(),
                        createRealmResponse.statusMessage());
            }

            Uni<Integer> realmStatusCodeUni = client.getAbs(keycloakUrl + "/realms/" + realm.getRealm())
                    .send().onItem()
                    .transform(resp -> {
                        LOG.debugf("Realm status: %d", resp.statusCode());
                        if (resp.statusCode() == 200) {
                            return 200;
                        } else {
                            throw new RealmEndpointAccessException(resp.statusCode());
                        }
                    }).onFailure(realmEndpointNotAvailable())
                    .retry()
                    .withBackOff(Duration.ofSeconds(2), Duration.ofSeconds(2))
                    .expireIn(10 * 1000)
                    .onFailure().transform(t -> t.getCause());
            realmStatusCodeUni.await().atMost(Duration.ofSeconds(10));
        } catch (Throwable t) {
            LOG.errorf("Realm %s can not be created: %s", realm.getRealm(), t.getMessage());
        } finally {
            client.close();
        }
    }

    @SuppressWarnings("serial")
    static class RealmEndpointAccessException extends RuntimeException {
        private final int errorStatus;

        public RealmEndpointAccessException(int errorStatus) {
            this.errorStatus = errorStatus;
        }

        public int getErrorStatus() {
            return errorStatus;
        }
    }

    public static Predicate<? super Throwable> realmEndpointNotAvailable() {
        return t -> (t instanceof ConnectException
                || (t instanceof RealmEndpointAccessException && ((RealmEndpointAccessException) t).getErrorStatus() == 404));
    }

    private Map<String, String> getUsers(Map<String, String> configuredUsers, boolean createRealm) {
        if (configuredUsers.isEmpty() && createRealm) {
            Map<String, String> users = new LinkedHashMap<String, String>();
            users.put("alice", "alice");
            users.put("bob", "bob");
            return users;
        } else {
            return configuredUsers;
        }
    }

    private List<String> getUserRoles(String user) {
        List<String> roles = capturedDevServicesConfiguration.roles.get(user);
        return roles == null ? ("alice".equals(user) ? List.of("admin", "user") : List.of("user"))
                : roles;
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
            Set<String> allRoles = new HashSet<>();
            for (List<String> distinctRoles : capturedDevServicesConfiguration.roles.values()) {
                for (String role : distinctRoles) {
                    if (!allRoles.contains(role)) {
                        allRoles.add(role);
                        realm.getRoles().getRealm().add(new RoleRepresentation(role, null, false));
                    }
                }
            }
        }
        return realm;
    }

    private ClientRepresentation createClient(String clientId, String oidcClientSecret) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setRedirectUris(List.of("*"));
        client.setPublicClient(false);
        client.setSecret(oidcClientSecret);
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setImplicitFlowEnabled(true);
        client.setEnabled(true);
        client.setRedirectUris(List.of("*"));
        client.setDefaultClientScopes(List.of("microprofile-jwt"));

        return client;
    }

    private UserRepresentation createUser(String username, String password, List<String> realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(realmRoles);

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
