package io.quarkus.devservices.keycloak;

import static io.quarkus.devservices.keycloak.KeycloakDevServicesConfigBuildItem.getKeycloakUrl;
import static io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem.getDevServicesConfigurator;
import static io.quarkus.devservices.keycloak.KeycloakDevServicesUtils.createWebClient;
import static io.quarkus.devservices.keycloak.KeycloakDevServicesUtils.getPasswordAccessToken;

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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.IsDevelopment;
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
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.ConfigPropertiesContext;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class KeycloakDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(KeycloakDevServicesProcessor.class);

    private static final String CONFIG_PREFIX = "quarkus.oidc.";

    // avoid the Quarkus prefix in order to prevent warnings when the application starts in container integration tests
    private static final String CLIENT_AUTH_SERVER_URL_CONFIG_KEY = "client." + CONFIG_PREFIX + "auth-server-url";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    static final String KEYCLOAK_URL_KEY = "keycloak.url";

    private static final String KEYCLOAK_CONTAINER_NAME = "keycloak";
    private static final int KEYCLOAK_PORT = 8080;
    private static final int KEYCLOAK_HTTPS_PORT = 8443;

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
    private static final String KEYCLOAK_QUARKUS_START_CMD = "start --http-enabled=true --hostname-strict=false "
            + "--spi-user-profile-declarative-user-profile-config-file=/opt/keycloak/upconfig.json";

    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String OIDC_USERS = "oidc.users";
    private static final String KEYCLOAK_REALMS = "keycloak.realms";

    /**
     * Label to add to shared Dev Service for Keycloak running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-keycloak";
    private static final ContainerLocator KEYCLOAK_DEV_MODE_CONTAINER_LOCATOR = new ContainerLocator(DEV_SERVICE_LABEL,
            KEYCLOAK_PORT);

    private static volatile RunningDevService devService;
    private static volatile KeycloakDevServicesConfig capturedDevServicesConfiguration;
    private static volatile boolean first = true;
    private static volatile Set<FileTime> capturedRealmFileLastModifiedDate;
    private static volatile Vertx vertxInstance;

    @BuildStep
    DevServicesResultBuildItem startKeycloakContainer(
            List<KeycloakDevServicesRequiredBuildItem> devSvcRequiredMarkerItems,
            BuildProducer<KeycloakDevServicesConfigBuildItem> keycloakBuildItemBuildProducer,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            KeycloakDevServicesConfig config,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LaunchModeBuildItem launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig, DockerStatusBuildItem dockerStatusBuildItem) {

        if (devSvcRequiredMarkerItems.isEmpty()
                || linuxContainersNotAvailable(dockerStatusBuildItem, devSvcRequiredMarkerItems)
                || oidcDevServicesEnabled()) {
            if (devService != null) {
                closeDevService();
            }
            return null;
        }
        var devServicesConfigurator = getDevServicesConfigurator(devSvcRequiredMarkerItems);

        // Figure out if we need to shut down and restart any existing Keycloak container
        // if not and the Keycloak container has already started we just return
        if (devService != null) {
            boolean restartRequired = !config.equals(capturedDevServicesConfiguration);
            if (!restartRequired) {
                Set<FileTime> currentRealmFileLastModifiedDate = getRealmFileLastModifiedDate(
                        config.realmPath());
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
                        : Arrays.stream(realmsString.split(",")).toList();
                keycloakBuildItemBuildProducer
                        .produce(new KeycloakDevServicesConfigBuildItem(result.getConfig(),
                                Map.of(OIDC_USERS, users, KEYCLOAK_REALMS, realms), false));
                return result;
            }
            closeDevService();
        }
        capturedDevServicesConfiguration = config;
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Keycloak Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {
            List<String> errors = new ArrayList<>();

            boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);
            RunningDevService newDevService = startContainer(keycloakBuildItemBuildProducer, useSharedNetwork,
                    devServicesConfig.timeout(), errors, devServicesConfigurator);
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

            capturedRealmFileLastModifiedDate = getRealmFileLastModifiedDate(capturedDevServicesConfiguration.realmPath());
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

    private static boolean oidcDevServicesEnabled() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.oidc.devservices.enabled", boolean.class).orElse(false);
    }

    private static boolean linuxContainersNotAvailable(DockerStatusBuildItem dockerStatusBuildItem,
            List<KeycloakDevServicesRequiredBuildItem> devSvcRequiredMarkerItems) {
        if (dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            return false;
        }

        for (var requirement : devSvcRequiredMarkerItems) {
            LOG.warnf("Please configure '%s' or get a working docker instance", requirement.getAuthServerUrl());
        }
        return true;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void produceDevUiCardWithKeycloakUrl(Optional<KeycloakDevServicesConfigBuildItem> configProps,
            List<KeycloakAdminPageBuildItem> keycloakAdminPageBuildItems,
            BuildProducer<CardPageBuildItem> cardPageProducer) {
        final String keycloakAdminUrl = getKeycloakUrl(configProps);
        if (keycloakAdminUrl != null) {
            keycloakAdminPageBuildItems.forEach(i -> {
                i.cardPage.addPage(Page
                        .externalPageBuilder("Keycloak Admin")
                        .icon("font-awesome-solid:key")
                        .doNotEmbed(true)
                        .url(keycloakAdminUrl));
                cardPageProducer.produce(i.cardPage);
            });
        }
    }

    private static void closeDevService() {
        try {
            devService.close();
        } catch (Throwable e) {
            LOG.error("Failed to stop Keycloak container", e);
        }
        devService = null;
        capturedDevServicesConfiguration = null;
    }

    private static String getBaseURL(String scheme, String host, Integer port) {
        return scheme + host + ":" + port;
    }

    private static String startURL(String scheme, String host, Integer port, boolean isKeycloakX) {
        return getBaseURL(scheme, host, port) + (isKeycloakX ? "" : "/auth");
    }

    private static String startURL(String baseUrl, boolean isKeycloakX) {
        return baseUrl + (isKeycloakX ? "" : "/auth");
    }

    private static Map<String, String> prepareConfiguration(
            BuildProducer<KeycloakDevServicesConfigBuildItem> keycloakBuildItemBuildProducer, String internalURL,
            String hostURL, List<RealmRepresentation> realmReps, List<String> errors,
            KeycloakDevServicesConfigurator devServicesConfigurator, String internalBaseUrl) {
        final String realmName = !realmReps.isEmpty() ? realmReps.iterator().next().getRealm()
                : getDefaultRealmName();
        final String authServerInternalUrl = realmsURL(internalURL, realmName);

        String clientAuthServerBaseUrl = hostURL != null ? hostURL : internalURL;
        String clientAuthServerUrl = realmsURL(clientAuthServerBaseUrl, realmName);

        boolean createDefaultRealm = (realmReps == null || realmReps.isEmpty())
                && capturedDevServicesConfiguration.createRealm();

        String oidcClientId = getOidcClientId();
        String oidcClientSecret = getOidcClientSecret();

        Map<String, String> users = getUsers(capturedDevServicesConfiguration.users(), createDefaultRealm);

        List<String> realmNames = new LinkedList<>();

        if (createDefaultRealm || !realmReps.isEmpty()) {

            // this needs to be only if we actually start the dev-service as it adds a shutdown hook
            // whose TCCL is the Augmentation CL, which if not removed, causes a massive memory leaks
            if (vertxInstance == null) {
                vertxInstance = Vertx.vertx();
            }

            WebClient client = createWebClient(vertxInstance);
            try {
                String adminToken = getAdminToken(client, clientAuthServerBaseUrl);
                if (createDefaultRealm) {
                    createDefaultRealm(client, adminToken, clientAuthServerBaseUrl, users, oidcClientId, oidcClientSecret,
                            errors,
                            devServicesConfigurator);
                    realmNames.add(realmName);
                } else if (realmReps != null) {
                    for (RealmRepresentation realmRep : realmReps) {
                        createRealm(client, adminToken, clientAuthServerBaseUrl, realmRep, errors);
                        realmNames.add(realmRep.getRealm());
                    }
                }

            } finally {
                client.close();
            }
        }

        Map<String, String> configProperties = new HashMap<>();
        var configPropertiesContext = new ConfigPropertiesContext(authServerInternalUrl, oidcClientId, oidcClientSecret,
                internalBaseUrl);
        configProperties.putAll(devServicesConfigurator.createProperties(configPropertiesContext));
        configProperties.put(KEYCLOAK_URL_KEY, internalURL);
        configProperties.put(CLIENT_AUTH_SERVER_URL_CONFIG_KEY, clientAuthServerUrl);
        configProperties.put(OIDC_USERS, users.entrySet().stream().map(Object::toString).collect(Collectors.joining(",")));
        configProperties.put(KEYCLOAK_REALMS, realmNames.stream().collect(Collectors.joining(",")));

        keycloakBuildItemBuildProducer
                .produce(new KeycloakDevServicesConfigBuildItem(configProperties,
                        Map.of(OIDC_USERS, users, KEYCLOAK_REALMS, realmNames), true));

        return configProperties;
    }

    private static String realmsURL(String baseURL, String realmName) {
        return baseURL + "/realms/" + realmName;
    }

    private static String getDefaultRealmName() {
        return capturedDevServicesConfiguration.realmName().orElse("quarkus");
    }

    private static RunningDevService startContainer(
            BuildProducer<KeycloakDevServicesConfigBuildItem> keycloakBuildItemBuildProducer,
            boolean useSharedNetwork, Optional<Duration> timeout,
            List<String> errors, KeycloakDevServicesConfigurator devServicesConfigurator) {
        if (!capturedDevServicesConfiguration.enabled()) {
            // explicitly disabled
            LOG.debug("Not starting Dev Services for Keycloak as it has been disabled in the config");
            return null;
        }

        final Optional<ContainerAddress> maybeContainerAddress = KEYCLOAK_DEV_MODE_CONTAINER_LOCATOR.locateContainer(
                capturedDevServicesConfiguration.serviceName(),
                capturedDevServicesConfiguration.shared(),
                LaunchMode.current());

        String imageName = capturedDevServicesConfiguration.imageName();
        DockerImageName dockerImageName = DockerImageName.parse(imageName).asCompatibleSubstituteFor(imageName);

        final Supplier<RunningDevService> defaultKeycloakContainerSupplier = () -> {

            QuarkusOidcContainer oidcContainer = new QuarkusOidcContainer(dockerImageName,
                    capturedDevServicesConfiguration.port(),
                    useSharedNetwork,
                    capturedDevServicesConfiguration.realmPath().orElse(List.of()),
                    resourcesMap(errors),
                    capturedDevServicesConfiguration.serviceName(),
                    capturedDevServicesConfiguration.shared(),
                    capturedDevServicesConfiguration.javaOpts(),
                    capturedDevServicesConfiguration.startCommand(),
                    capturedDevServicesConfiguration.showLogs(),
                    capturedDevServicesConfiguration.containerMemoryLimit(),
                    errors);

            timeout.ifPresent(oidcContainer::withStartupTimeout);
            oidcContainer.withEnv(capturedDevServicesConfiguration.containerEnv());
            oidcContainer.start();

            String internalBaseUrl = getBaseURL((oidcContainer.isHttps() ? "https://" : "http://"), oidcContainer.getHost(),
                    oidcContainer.getPort());
            String internalUrl = startURL(internalBaseUrl, oidcContainer.keycloakX);
            String hostUrl = oidcContainer.useSharedNetwork
                    // we need to use auto-detected host and port, so it works when docker host != localhost
                    ? startURL("http://", oidcContainer.getSharedNetworkExternalHost(),
                            oidcContainer.getSharedNetworkExternalPort(),
                            oidcContainer.keycloakX)
                    : null;

            Map<String, String> configs = prepareConfiguration(keycloakBuildItemBuildProducer, internalUrl, hostUrl,
                    oidcContainer.realmReps, errors, devServicesConfigurator, internalBaseUrl);
            return new RunningDevService(KEYCLOAK_CONTAINER_NAME, oidcContainer.getContainerId(),
                    oidcContainer::close, configs);
        };

        return maybeContainerAddress
                .map(containerAddress -> {
                    // TODO: this probably needs to be addressed
                    String sharedContainerUrl = getSharedContainerUrl(containerAddress);
                    Map<String, String> configs = prepareConfiguration(keycloakBuildItemBuildProducer, sharedContainerUrl,
                            sharedContainerUrl, List.of(), errors, devServicesConfigurator, sharedContainerUrl);
                    return new RunningDevService(KEYCLOAK_CONTAINER_NAME, containerAddress.getId(), null, configs);
                })
                .orElseGet(defaultKeycloakContainerSupplier);
    }

    private static Map<String, String> resourcesMap(List<String> errors) {
        Map<String, String> resources = new HashMap<>();
        for (Map.Entry<String, String> aliasEntry : capturedDevServicesConfiguration.resourceAliases().entrySet()) {
            if (capturedDevServicesConfiguration.resourceMappings().containsKey(aliasEntry.getKey())) {
                resources.put(aliasEntry.getValue(),
                        capturedDevServicesConfiguration.resourceMappings().get(aliasEntry.getKey()));
            } else {
                errors.add(String.format("%s alias for the %s resource does not have a mapping", aliasEntry.getKey(),
                        aliasEntry.getValue()));
                LOG.errorf("%s alias for the %s resource does not have a mapping", aliasEntry.getKey(),
                        aliasEntry.getValue());
            }
        }
        return resources;
    }

    private static boolean isKeycloakX(DockerImageName dockerImageName) {
        return capturedDevServicesConfiguration.keycloakXImage().isPresent()
                ? capturedDevServicesConfiguration.keycloakXImage().get()
                : !dockerImageName.getVersionPart().endsWith(KEYCLOAK_LEGACY_IMAGE_VERSION_PART);
    }

    private static String getSharedContainerUrl(ContainerAddress containerAddress) {
        return "http://" + ("0.0.0.0".equals(containerAddress.getHost()) ? "localhost" : containerAddress.getHost())
                + ":" + containerAddress.getPort();
    }

    private static class QuarkusOidcContainer extends GenericContainer<QuarkusOidcContainer> {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;
        private final List<String> realmPaths;
        private final Map<String, String> resources;
        private final String containerLabelValue;
        private final Optional<String> javaOpts;
        private final boolean sharedContainer;
        private String hostName;
        private final boolean keycloakX;
        private final List<RealmRepresentation> realmReps = new LinkedList<>();
        private final Optional<String> startCommand;
        private final boolean showLogs;
        private final MemorySize containerMemoryLimit;
        private final List<String> errors;

        public QuarkusOidcContainer(DockerImageName dockerImageName, OptionalInt fixedExposedPort, boolean useSharedNetwork,
                List<String> realmPaths, Map<String, String> resources, String containerLabelValue,
                boolean sharedContainer, Optional<String> javaOpts, Optional<String> startCommand, boolean showLogs,
                MemorySize containerMemoryLimit, List<String> errors) {
            super(dockerImageName);

            this.useSharedNetwork = useSharedNetwork;
            this.realmPaths = realmPaths;
            this.resources = resources;
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
            this.containerMemoryLimit = containerMemoryLimit;
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
                addUpConfigResource();
                if (isHttps()) {
                    addExposedPort(KEYCLOAK_HTTPS_PORT);
                }
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
            for (Map.Entry<String, String> resource : resources.entrySet()) {
                mapResource(resource.getKey(), resource.getValue());
            }

            if (showLogs) {
                super.withLogConsumer(t -> {
                    LOG.info("Keycloak: " + t.getUtf8StringWithoutLineEnding());
                });
            }

            super.withCreateContainerCmdModifier((container) -> Optional.ofNullable(container.getHostConfig())
                    .ifPresent(hostConfig -> {
                        final var limit = containerMemoryLimit.asLongValue();
                        hostConfig.withMemory(limit);
                        LOG.debug("Set container memory limit (bytes): " + limit);
                    }));

            LOG.infof("Using %s powered Keycloak distribution", keycloakX ? "Quarkus" : "WildFly");
        }

        private void mapResource(String resourcePath, String mappedResource) {
            if (Thread.currentThread().getContextClassLoader().getResource(resourcePath) != null) {
                LOG.debugf("Mapping the classpath %s resource to %s", resourcePath, mappedResource);
                withClasspathResourceMapping(resourcePath, mappedResource, BindMode.READ_ONLY);
            } else if (Files.exists(Paths.get(resourcePath))) {
                LOG.debugf("Mapping the file system %s resource to %s", resourcePath, mappedResource);
                withFileSystemBind(resourcePath, mappedResource, BindMode.READ_ONLY);
            } else {
                errors.add(
                        String.format(
                                "%s resource can not be mapped to %s because it is not available on the classpath and file system",
                                resourcePath, mappedResource));
                LOG.errorf("%s resource can not be mapped to %s because it is not available on the classpath and file system",
                        resourcePath, mappedResource);
            }
        }

        private void addUpConfigResource() {
            if (Thread.currentThread().getContextClassLoader().getResource("/dev-service/upconfig.json") != null) {
                LOG.debug("Mapping the classpath /dev-service/upconfig.json resource to /opt/keycloak/upconfig.json");
                withClasspathResourceMapping("/dev-service/upconfig.json", "/opt/keycloak/upconfig.json", BindMode.READ_ONLY);
            }
        }

        private static Integer findRandomPort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static Optional<RealmRepresentation> readRealmFile(URI uri, String realmPath, List<String> errors) {
            try {
                return readRealmFile(uri.toURL(), realmPath, errors);
            } catch (MalformedURLException ex) {
                // Will not happen as this method is called only when it is confirmed the file exists
                throw new RuntimeException(ex);
            }
        }

        private static Optional<RealmRepresentation> readRealmFile(URL url, String realmPath, List<String> errors) {
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
            return super.getMappedPort(isHttps() ? KEYCLOAK_HTTPS_PORT : KEYCLOAK_PORT);
        }

        public boolean isHttps() {
            return startCommand.isPresent() && startCommand.get().contains("--https");
        }
    }

    private static Set<FileTime> getRealmFileLastModifiedDate(Optional<List<String>> realms) {
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

    private static void createDefaultRealm(WebClient client, String token, String keycloakUrl, Map<String, String> users,
            String oidcClientId, String oidcClientSecret, List<String> errors,
            KeycloakDevServicesConfigurator devServicesConfigurator) {
        RealmRepresentation realm = createDefaultRealmRep();

        if (capturedDevServicesConfiguration.createClient()) {
            realm.getClients().add(createClient(oidcClientId, oidcClientSecret));
        }
        for (Map.Entry<String, String> entry : users.entrySet()) {
            realm.getUsers().add(createUser(entry.getKey(), entry.getValue(), getUserRoles(entry.getKey())));
        }

        devServicesConfigurator.customizeDefaultRealm(realm);

        createRealm(client, token, keycloakUrl, realm, errors);
    }

    private static String getAdminToken(WebClient client, String keycloakUrl) {
        try {
            LOG.tracef("Acquiring admin token");

            return getPasswordAccessToken(client,
                    keycloakUrl + "/realms/master/protocol/openid-connect/token",
                    "admin-cli", null, "admin", "admin", null)
                    .await().atMost(capturedDevServicesConfiguration.webClientTimeout());
        } catch (TimeoutException e) {
            LOG.error("Admin token can not be acquired due to a client connection timeout. " +
                    "You may try increasing the `quarkus.oidc.devui.web-client-timeout` property.");
        } catch (Throwable t) {
            LOG.error("Admin token can not be acquired", t);
        }
        return null;
    }

    private static void createRealm(WebClient client, String token, String keycloakUrl, RealmRepresentation realm,
            List<String> errors) {
        try {
            LOG.tracef("Creating the realm %s", realm.getRealm());
            HttpResponse<Buffer> createRealmResponse = client.postAbs(keycloakUrl + "/admin/realms")
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                    .sendBuffer(Buffer.buffer().appendString(JsonSerialization.writeValueAsString(realm)))
                    .await().atMost(capturedDevServicesConfiguration.webClientTimeout());

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

            LOG.errorf(t, "Realm %s can not be created", realm.getRealm());
        }
    }

    @SuppressWarnings("serial")
    private static class RealmEndpointAccessException extends RuntimeException {
        private final int errorStatus;

        public RealmEndpointAccessException(int errorStatus) {
            this.errorStatus = errorStatus;
        }

        public int getErrorStatus() {
            return errorStatus;
        }
    }

    private static Predicate<? super Throwable> realmEndpointNotAvailable() {
        return t -> (t instanceof ConnectException
                || (t instanceof RealmEndpointAccessException && ((RealmEndpointAccessException) t).getErrorStatus() == 404));
    }

    private static Map<String, String> getUsers(Map<String, String> configuredUsers, boolean createRealm) {
        if (configuredUsers.isEmpty() && createRealm) {
            Map<String, String> users = new LinkedHashMap<String, String>();
            users.put("alice", "alice");
            users.put("bob", "bob");
            return users;
        } else {
            return configuredUsers;
        }
    }

    private static List<String> getUserRoles(String user) {
        List<String> roles = capturedDevServicesConfiguration.roles().get(user);
        return roles == null ? ("alice".equals(user) ? List.of("admin", "user") : List.of("user"))
                : roles;
    }

    private static RealmRepresentation createDefaultRealmRep() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(getDefaultRealmName());
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(600);
        realm.setSsoSessionMaxLifespan(600);
        realm.setRefreshTokenMaxReuse(10);

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        if (capturedDevServicesConfiguration.roles().isEmpty()) {
            realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
            realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        } else {
            Set<String> allRoles = new HashSet<>();
            for (List<String> distinctRoles : capturedDevServicesConfiguration.roles().values()) {
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

    private static ClientRepresentation createClient(String clientId, String oidcClientSecret) {
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

    private static UserRepresentation createUser(String username, String password, List<String> realmRoles) {
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

    private static String getOidcClientId() {
        // if the application type is web-app or hybrid, OidcRecorder will enforce that the client id and secret are configured
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_ID_CONFIG_KEY, String.class)
                .orElse(capturedDevServicesConfiguration.createClient() ? "quarkus-app" : "");
    }

    private static String getOidcClientSecret() {
        // if the application type is web-app or hybrid, OidcRecorder will enforce that the client id and secret are configured
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_SECRET_CONFIG_KEY, String.class)
                .orElse(capturedDevServicesConfiguration.createClient() ? "secret" : "");
    }

}
