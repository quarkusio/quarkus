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
import java.util.LinkedList;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
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
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
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

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { IsEnabled.class, GlobalDevServicesConfig.Enabled.class })
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
    private static final String KEYCLOAK_WILDFLY_DB_VENDOR = "H2";
    private static final String KEYCLOAK_WILDFLY_VENDOR_PROP = "DB_VENDOR";

    // Properties recognized by Quarkus-powered Keycloak
    private static final String KEYCLOAK_QUARKUS_HOSTNAME = "KC_HOSTNAME";
    private static final String KEYCLOAK_QUARKUS_ADMIN_PROP = "KEYCLOAK_ADMIN";
    private static final String KEYCLOAK_QUARKUS_ADMIN_PASSWORD_PROP = "KEYCLOAK_ADMIN_PASSWORD";
    private static final String KEYCLOAK_QUARKUS_START_CMD = "start --storage=chm --http-enabled=true --hostname-strict=false --hostname-strict-https=false";

    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String OIDC_USERS = "oidc.users";
    private static final String KEYCLOAK_REALMS = "keycloak.realms";

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
    private static volatile Set<FileTime> capturedRealmFileLastModifiedDate;

    OidcBuildTimeConfig oidcConfig;

    @BuildStep
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
                Set<FileTime> currentRealmFileLastModifiedDate = getRealmFileLastModifiedDate(
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
                String realmsString = result.getConfig().get(KEYCLOAK_REALMS);
                List<String> realms = (realmsString == null || realmsString.isBlank()) ? List.of()
                        : Arrays.stream(realmsString.split(",")).collect(Collectors.toList());
                keycloakBuildItemBuildProducer
                        .produce(new KeycloakDevServicesConfigBuildItem(result.getConfig(),
                                Map.of(OIDC_USERS, users, KEYCLOAK_REALMS, realms)));
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
                (launchMode.isTest() ? "(test) " : "") + "Keycloak Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        if (vertxInstance == null) {
            vertxInstance = Vertx.vertx();
        }
        try {
            List<String> errors = new ArrayList<>();

            RunningDevService newDevService = startContainer(dockerStatusBuildItem, keycloakBuildItemBuildProducer,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout,
                    errors);
            if (newDevService == null) {
                if (errors.isEmpty()) {
                    compressor.close();
                } else {
                    compressor.closeAndDumpCaptured();
                }
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
            if (devService != null && errors.isEmpty()) {
                compressor.close();
            } else {
                compressor.closeAndDumpCaptured();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
        LOG.info("Dev Services for Keycloak started.");

        return devService.toBuildItem();
    }

    private String startURL(String host, Integer port, boolean isKeycloakX) {
        return "http://" + host + ":" + port + (isKeycloakX ? "" : "/auth");
    }

    private Map<String, String> prepareConfiguration(
            BuildProducer<KeycloakDevServicesConfigBuildItem> keycloakBuildItemBuildProducer, String internalURL,
            String hostURL, List<RealmRepresentation> realmReps,
            boolean keycloakX, List<String> errors) {
        final String realmName = realmReps != null && !realmReps.isEmpty() ? realmReps.iterator().next().getRealm()
                : getDefaultRealmName();
        final String authServerInternalUrl = realmsURL(internalURL, realmName);

        String clientAuthServerBaseUrl = hostURL != null ? hostURL : internalURL;
        String clientAuthServerUrl = realmsURL(clientAuthServerBaseUrl, realmName);

        boolean createDefaultRealm = realmReps.isEmpty() && capturedDevServicesConfiguration.createRealm;

        String oidcClientId = getOidcClientId(createDefaultRealm);
        String oidcClientSecret = getOidcClientSecret(createDefaultRealm);
        String oidcApplicationType = getOidcApplicationType();

        Map<String, String> users = getUsers(capturedDevServicesConfiguration.users, createDefaultRealm);

        List<String> realmNames = new LinkedList<>();

        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
        try {
            String adminToken = getAdminToken(client, clientAuthServerBaseUrl);
            if (createDefaultRealm) {
                createDefaultRealm(client, adminToken, clientAuthServerBaseUrl, users, oidcClientId, oidcClientSecret, errors);
                realmNames.add(realmName);
            } else {
                for (RealmRepresentation realmRep : realmReps) {
                    createRealm(client, adminToken, clientAuthServerBaseUrl, realmRep, errors);
                    realmNames.add(realmRep.getRealm());
                }
            }
        } finally {
            client.close();
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
        configProperties.put(KEYCLOAK_REALMS, realmNames.stream().collect(Collectors.joining(",")));

        keycloakBuildItemBuildProducer
                .produce(new KeycloakDevServicesConfigBuildItem(configProperties,
                        Map.of(OIDC_USERS, users, KEYCLOAK_REALMS, realmNames)));

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
            boolean useSharedNetwork, Optional<Duration> timeout,
            List<String> errors) {
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
                    capturedDevServicesConfiguration.realmPath.orElse(List.of()),
                    capturedDevServicesConfiguration.serviceName,
                    capturedDevServicesConfiguration.shared,
                    capturedDevServicesConfiguration.javaOpts,
                    capturedDevServicesConfiguration.startCommand,
                    capturedDevServicesConfiguration.showLogs,
                    errors);

            timeout.ifPresent(oidcContainer::withStartupTimeout);
            oidcContainer.start();

            String internalUrl = startURL(oidcContainer.getHost(), oidcContainer.getPort(), oidcContainer.keycloakX);
            String hostUrl = oidcContainer.useSharedNetwork
                    // we need to use auto-detected host and port, so it works when docker host != localhost
                    ? startURL(oidcContainer.getSharedNetworkExternalHost(), oidcContainer.getSharedNetworkExternalPort(),
                            oidcContainer.keycloakX)
                    : null;

            Map<String, String> configs = prepareConfiguration(keycloakBuildItemBuildProducer, internalUrl, hostUrl,
                    oidcContainer.realmReps,
                    oidcContainer.keycloakX,
                    errors);
            return new RunningDevService(KEYCLOAK_CONTAINER_NAME, oidcContainer.getContainerId(),
                    oidcContainer::close, configs);
        };

        return maybeContainerAddress
                .map(containerAddress -> {
                    // TODO: this probably needs to be addressed
                    Map<String, String> configs = prepareConfiguration(keycloakBuildItemBuildProducer,
                            getSharedContainerUrl(containerAddress),
                            getSharedContainerUrl(containerAddress), null, false, errors);
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
        private final List<String> realmPaths;
        private final String containerLabelValue;
        private final Optional<String> javaOpts;
        private final boolean sharedContainer;
        private String hostName;
        private final boolean keycloakX;
        private List<RealmRepresentation> realmReps = new LinkedList<>();
        private final Optional<String> startCommand;
        private final boolean showLogs;
        private final List<String> errors;

        public QuarkusOidcContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, boolean useSharedNetwork,
                List<String> realmPaths, String containerLabelValue,
                boolean sharedContainer, Optional<String> javaOpts, Optional<String> startCommand, boolean showLogs,
                List<String> errors) {
            super(dockerImageName);

            this.useSharedNetwork = useSharedNetwork;
            this.realmPaths = realmPaths;
            this.containerLabelValue = containerLabelValue;
            this.sharedContainer = sharedContainer;
            this.javaOpts = javaOpts;
            this.keycloakX = isKeycloakX(dockerImageName);

            if (useSharedNetwork && fixedExposedPort.isEmpty()) {
                // We need to know the port we are exposing when using the shared network, in order to be able to tell
                // Keycloak what the client URL is. This is necessary in order for Keycloak to create the proper 'issuer'
                // when creating tokens
                fixedExposedPort = OptionalInt.of(findRandomPort());
            }

            this.fixedExposedPort = fixedExposedPort;
            this.startCommand = startCommand;
            this.showLogs = showLogs;
            this.errors = errors;

            super.setWaitStrategy(Wait.forLogMessage(".*Keycloak.*started.*", 1));
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
                if (useSharedNetwork) {
                    // expose random port for which we are able to ask Testcontainers for the actual mapped port at runtime
                    // as from the host's perspective Testcontainers actually expose container ports on random host port
                    addExposedPort(KEYCLOAK_PORT);
                }
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
                withCommand(startCommand.orElse(KEYCLOAK_QUARKUS_START_CMD)
                        + (useSharedNetwork ? " --hostname-port=" + fixedExposedPort.getAsInt() : ""));
            } else {
                addEnv(KEYCLOAK_WILDFLY_USER_PROP, KEYCLOAK_ADMIN_USER);
                addEnv(KEYCLOAK_WILDFLY_PASSWORD_PROP, KEYCLOAK_ADMIN_PASSWORD);
                addEnv(KEYCLOAK_WILDFLY_VENDOR_PROP, KEYCLOAK_WILDFLY_DB_VENDOR);
            }

            for (String realmPath : realmPaths) {
                URL realmPathUrl = null;
                if ((realmPathUrl = Thread.currentThread().getContextClassLoader().getResource(realmPath)) != null) {
                    readRealmFile(realmPathUrl, realmPath, errors).ifPresent(realmRep -> realmReps.add(realmRep));
                } else {
                    Path filePath = Paths.get(realmPath);
                    if (Files.exists(filePath)) {
                        readRealmFile(filePath.toUri(), realmPath, errors).ifPresent(realmRep -> realmReps.add(realmRep));
                    } else {
                        errors.add(String.format("Realm %s resource is not available", realmPath));
                        LOG.debugf("Realm %s resource is not available", realmPath);
                    }
                }

            }

            if (showLogs) {
                super.withLogConsumer(t -> {
                    LOG.info("Keycloak: " + t.getUtf8String());
                });
            }

            LOG.infof("Using %s powered Keycloak distribution", keycloakX ? "Quarkus" : "WildFly");
        }

        private Integer findRandomPort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private Optional<RealmRepresentation> readRealmFile(URI uri, String realmPath, List<String> errors) {
            try {
                return readRealmFile(uri.toURL(), realmPath, errors);
            } catch (MalformedURLException ex) {
                // Will not happen as this method is called only when it is confirmed the file exists
                throw new RuntimeException(ex);
            }
        }

        private Optional<RealmRepresentation> readRealmFile(URL url, String realmPath, List<String> errors) {
            try {
                try (InputStream is = url.openStream()) {
                    return Optional.of(JsonSerialization.readValue(is, RealmRepresentation.class));
                }
            } catch (IOException ex) {
                errors.add(String.format("Realm %s resource can not be opened: %s", realmPath, ex.getMessage()));

                LOG.errorf("Realm %s resource can not be opened: %s", realmPath, ex.getMessage());
            }
            return Optional.empty();
        }

        @Override
        public String getHost() {
            if (useSharedNetwork) {
                return hostName;
            }
            return super.getHost();
        }

        /**
         * Host name used for calls from outside of docker when {@code useSharedNetwork} is true.
         *
         * @return host name
         */
        private String getSharedNetworkExternalHost() {
            return super.getHost();
        }

        /**
         * Host port used for calls from outside of docker when {@code useSharedNetwork} is true.
         *
         * @return port
         */
        private int getSharedNetworkExternalPort() {
            return getFirstMappedPort();
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

    private Set<FileTime> getRealmFileLastModifiedDate(Optional<List<String>> realms) {
        if (realms.isPresent()) {
            Set<FileTime> times = new HashSet<>();

            for (String realm : realms.get()) {
                Path realmPath = Paths.get(realm);
                try {
                    times.add(Files.getLastModifiedTime(realmPath));
                } catch (IOException ex) {
                    LOG.tracef("Unable to get the last modified date of the realm file %s", realmPath);
                }
            }

            return times;
        }
        return null;
    }

    private void createDefaultRealm(WebClient client, String token, String keycloakUrl, Map<String, String> users,
            String oidcClientId,
            String oidcClientSecret,
            List<String> errors) {
        RealmRepresentation realm = createDefaultRealmRep();

        realm.getClients().add(createClient(oidcClientId, oidcClientSecret));
        for (Map.Entry<String, String> entry : users.entrySet()) {
            realm.getUsers().add(createUser(entry.getKey(), entry.getValue(), getUserRoles(entry.getKey())));
        }

        createRealm(client, token, keycloakUrl, realm, errors);
    }

    private String getAdminToken(WebClient client, String keycloakUrl) {
        try {
            LOG.tracef("Acquiring admin token");

            return OidcDevServicesUtils.getPasswordAccessToken(client,
                    keycloakUrl + "/realms/master/protocol/openid-connect/token",
                    "admin-cli", null, "admin", "admin", null, oidcConfig.devui.webClientTimeout);
        } catch (Throwable t) {
            LOG.errorf("Admin token can not be acquired: %s", t.getMessage());
        }
        return null;
    }

    private void createRealm(WebClient client, String token, String keycloakUrl, RealmRepresentation realm,
            List<String> errors) {
        try {
            LOG.tracef("Creating the realm %s", realm.getRealm());
            HttpResponse<Buffer> createRealmResponse = client.postAbs(keycloakUrl + "/admin/realms")
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                    .sendBuffer(Buffer.buffer().appendString(JsonSerialization.writeValueAsString(realm)))
                    .await().atMost(oidcConfig.devui.webClientTimeout);

            if (createRealmResponse.statusCode() > 299) {
                errors.add(String.format("Realm %s can not be created %d - %s ", realm.getRealm(),
                        createRealmResponse.statusCode(),
                        createRealmResponse.statusMessage()));

                LOG.errorf("Realm %s can not be created %d - %s ", realm.getRealm(), createRealmResponse.statusCode(),
                        createRealmResponse.statusMessage());
            }

            Uni<Integer> realmStatusCodeUni = client.getAbs(keycloakUrl + "/realms/" + realm.getRealm())
                    .send().onItem()
                    .transform(resp -> {
                        LOG.debugf("Realm status: %d", resp.statusCode());
                        if (resp.statusCode() == 200) {
                            LOG.debugf("Realm %s has been created", realm.getRealm());
                            return 200;
                        } else {
                            throw new RealmEndpointAccessException(resp.statusCode());
                        }
                    }).onFailure(realmEndpointNotAvailable())
                    .retry()
                    .withBackOff(Duration.ofSeconds(2), Duration.ofSeconds(2))
                    .expireIn(10 * 1000)
                    .onFailure().transform(t -> {
                        return new RuntimeException("Keycloak server is not available"
                                + (t.getMessage() != null ? (": " + t.getMessage()) : ""));
                    });
            realmStatusCodeUni.await().atMost(Duration.ofSeconds(10));
        } catch (Throwable t) {
            errors.add(String.format("Realm %s can not be created: %s", realm.getRealm(), t.getMessage()));

            LOG.errorf("Realm %s can not be created: %s", realm.getRealm(), t.getMessage());
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

    private RealmRepresentation createDefaultRealmRep() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(getDefaultRealmName());
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(600);
        realm.setSsoSessionMaxLifespan(600);

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

    private static String getOidcClientId(boolean createRealm) {
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_ID_CONFIG_KEY, String.class)
                .orElse(createRealm ? "quarkus-app" : "");
    }

    private static String getOidcClientSecret(boolean createRealm) {
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_SECRET_CONFIG_KEY, String.class)
                .orElse(createRealm ? "secret" : "");
    }
}
