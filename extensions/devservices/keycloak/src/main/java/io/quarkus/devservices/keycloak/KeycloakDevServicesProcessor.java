package io.quarkus.devservices.keycloak;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;
import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;
import static io.quarkus.devservices.keycloak.KeycloakDevServicesConfigBuildItem.getKeycloakUrl;
import static io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem.getDevServicesConfigurator;
import static io.quarkus.devservices.keycloak.KeycloakDevServicesUtils.createWebClient;
import static io.quarkus.devservices.keycloak.KeycloakDevServicesUtils.getPasswordAccessToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
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

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
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

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class KeycloakDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(KeycloakDevServicesProcessor.class);

    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String APPLICATION_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";

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
    private static final String KEYCLOAK_QUARKUS_ADMIN_PROP = "KC_BOOTSTRAP_ADMIN_USERNAME";
    private static final String KEYCLOAK_QUARKUS_ADMIN_PASSWORD_PROP = "KC_BOOTSTRAP_ADMIN_PASSWORD";
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
    private static final ContainerLocator KEYCLOAK_DEV_MODE_CONTAINER_LOCATOR = locateContainerWithLabels(KEYCLOAK_PORT,
            DEV_SERVICE_LABEL);

    // TODO can't be correct
    private static volatile Set<FileTime> capturedRealmFileLastModifiedDate;

    @BuildStep
    DevServicesResultBuildItem startKeycloakContainer(
            List<KeycloakDevServicesRequiredBuildItem> devSvcRequiredMarkerItems,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            BuildProducer<KeycloakDevServicesConfigBuildItem> keycloakBuildItemBuildProducer,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            KeycloakDevServicesConfig config,
            LaunchModeBuildItem launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig, DockerStatusBuildItem dockerStatusBuildItem) {

        // TODO if the reused
        if (!devServicesConfig.enabled() || !config.enabled()) {
            LOG.debug("Not starting Dev Services for Keycloak as it has been disabled in the configuration");
            return null;
        }

        if (devSvcRequiredMarkerItems.isEmpty()
                || oidcDevServicesEnabled()
                || linuxContainersNotAvailable(dockerStatusBuildItem, devSvcRequiredMarkerItems)) {
            return null;
        }
        var devServicesConfigurator = getDevServicesConfigurator(devSvcRequiredMarkerItems);

        // Figure out if we need to shut down and restart any existing Keycloak container
        // if not and the Keycloak container has already started we just return

        // TODO we need to capture and update the realm last modified
        Set<FileTime> currentRealmFileLastModifiedDate = getRealmFileLastModifiedDate(
                config.realmPath());
        if (currentRealmFileLastModifiedDate != null
                && !currentRealmFileLastModifiedDate.equals(capturedRealmFileLastModifiedDate)) {

        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        String imageName = config.imageName().orElse(null); // TODO handle the null properly
        // TODO do something with this
        List<String> errors = new ArrayList<>();

        return KEYCLOAK_DEV_MODE_CONTAINER_LOCATOR.locateContainer(config.serviceName(),
                config.shared(),
                LaunchMode.current())
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem, List.of(imageName, "keycloak"),
                        KEYCLOAK_PORT, LaunchMode.current(), useSharedNetwork))
                .map(containerAddress -> {
                    // TODO: this probably needs to be addressed
                    String sharedContainerUrl = getSharedContainerUrl(containerAddress);
                    Map<String, String> configs = prepareConfiguration(config,
                            sharedContainerUrl,
                            sharedContainerUrl, List.of(), errors, devServicesConfigurator, sharedContainerUrl);
                    return DevServicesResultBuildItem.discovered()
                            .feature(Feature.KEYCLOAK_AUTHORIZATION)
                            .containerId(containerAddress.getId())
                            .config(configs)
                            .build();
                }).orElseGet(() -> DevServicesResultBuildItem.owned().feature(Feature.KEYCLOAK_AUTHORIZATION)
                        .serviceName(Feature.KEYCLOAK_AUTHORIZATION.getName())
                        .serviceConfig(config)
                        .startable(
                                () -> new KeycloakServer(useSharedNetwork, config, devServicesConfig, devServicesConfigurator,
                                        composeProjectBuildItem))
                        .configProvider(createLazyConfigMap(devServicesConfigurator))
                        .build());

        //        var configPropertiesContext = new KeycloakDevServicesConfigurator.ConfigPropertiesContext(authServerInternalUrl, oidcClientId, oidcClientSecret,
        //                internalBaseUrl);
        //        configProperties.putAll(devServicesConfigurator.createProperties(configPropertiesContext));

        // TODO what to do for this?
        //        StartupLogCompressor compressor = new StartupLogCompressor(
        //                (launchMode.isTest() ? "(test) " : "") + "Keycloak Dev Services Starting:",
        //                consoleInstalledBuildItem, loggingSetupBuildItem);
        //            List<String> errors = new ArrayList<>();
        //
        //            if (newDevService == null) {
        //                if (errors.isEmpty()) {
        //                    compressor.close();
        //                } else {
        //                    compressor.closeAndDumpCaptured();
        //                }
        //                return null;
        //            }
        //
        //
        //            capturedRealmFileLastModifiedDate = getRealmFileLastModifiedDate(config.realmPath());
        //            if (devService != null && errors.isEmpty()) {
        //                compressor.close();
        //            } else {
        //                compressor.closeAndDumpCaptured();
        //            }
        //        } catch (Throwable t) {
        //            compressor.closeAndDumpCaptured();
        //            throw new RuntimeException(t);
        //        }

    }

    private static @NotNull Map<String, Function<KeycloakServer, String>> createLazyConfigMap(
            KeycloakDevServicesConfigurator devServicesConfigurator) {

        // Generate a dummy config map, to get the keys the configurator will set
        // We could move this method to the configurator interface, with a default implementation to avoid breaking anything, but a more efficient implementation would be non-DRY on the key list, so risky
        KeycloakDevServicesConfigurator.ConfigPropertiesContext ctx = new KeycloakDevServicesConfigurator.ConfigPropertiesContext(
                "", "", "", "");
        Map<String, String> dummyConfigs = devServicesConfigurator.createProperties(ctx);

        Map<String, Function<KeycloakServer, String>> configs = new HashMap<>();

        configs.put(
                KEYCLOAK_URL_KEY, s -> s.conf().get(KEYCLOAK_URL_KEY));
        configs.put(CLIENT_AUTH_SERVER_URL_CONFIG_KEY, s -> s.conf().get(CLIENT_AUTH_SERVER_URL_CONFIG_KEY));
        configs.put(OIDC_USERS, s -> s.conf().get(OIDC_USERS));
        configs.put(KEYCLOAK_REALMS, s -> s.conf().get(KEYCLOAK_REALMS));

        for (String key : dummyConfigs.keySet()) {
            configs.put(key, s -> s.conf().get(key));
        }

        return configs;
    }

    private String getRealm(KeycloakServer result) {
        //        String realmsString = result.getConfig().get(KEYCLOAK_REALMS);
        //        List<String> realms = (realmsString == null || realmsString.isBlank()) ? List.of()
        //                : Arrays.stream(realmsString.split(",")).toList();
        //        return realms;
        return "TODO realm"; // TODO

    }

    private String getUserMap(KeycloakServer result) {
        //        String usersString = result.getConfig().get(OIDC_USERS);
        //        Map<String, String> users = (usersString == null || usersString.isBlank()) ? Map.of()
        //                : Arrays.stream(usersString.split(","));
        //        return users;
        return "TODO users"; // TODO
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

    private static String getBaseURL(String scheme, String host, Integer port) {
        return scheme + host + ":" + port;
    }

    private static String startURL(String scheme, String host, Integer port, boolean isKeycloakX) {
        return getBaseURL(scheme, host, port) + (isKeycloakX ? "" : "/auth");
    }

    private static String startURL(String baseUrl, boolean isKeycloakX) {
        return baseUrl + (isKeycloakX ? "" : "/auth");
    }

    private static String realmsURL(String baseURL, String realmName) {
        return baseURL + "/realms/" + realmName;
    }

    private static String getDefaultRealmName(KeycloakDevServicesConfig config) {
        return config.realmName().orElse("quarkus");
    }

    // Wrap the vertx instance and the container, since both need to be started and closed
    private static class KeycloakServer implements Startable {
        private final boolean useSharedNetwork;
        private final KeycloakDevServicesConfig config;
        private final DevServicesConfig devServicesConfig;
        Vertx vertxInstance;
        QuarkusOidcContainer oidcContainer;
        private KeycloakDevServicesConfigurator devServicesConfigurator;
        private DevServicesComposeProjectBuildItem composeProjectBuildItem;
        private Map<String, String> generatedConfig;
        private String internalUrl;
        private String oidcClientId;
        private String oidcClientSecret;

        public KeycloakServer(boolean useSharedNetwork, KeycloakDevServicesConfig config, DevServicesConfig devServicesConfig,
                KeycloakDevServicesConfigurator devServicesConfigurator,
                DevServicesComposeProjectBuildItem composeProjectBuildItem) {
            this.useSharedNetwork = useSharedNetwork;
            this.config = config;
            this.devServicesConfig = devServicesConfig;
            this.devServicesConfigurator = devServicesConfigurator;
            this.composeProjectBuildItem = composeProjectBuildItem;
        }

        Map<String, String> conf() {
            return generatedConfig;
        }

        @Override
        public void start() {
            // TODO do something with this
            List<String> errors = new ArrayList<>();

            startContainer(config,
                    useSharedNetwork,
                    devServicesConfig.timeout(),
                    errors, devServicesConfigurator, composeProjectBuildItem);
        }

        @Override
        public String getConnectionInfo() {
            return generatedConfig.get(KEYCLOAK_URL_KEY);
        }

        @Override
        public String getContainerId() {
            return oidcContainer.getContainerId();
        }

        @Override
        public void close() {
            // TODO should we null check?
            oidcContainer.stop();
            vertxInstance.close();
        }

        private void startContainer(KeycloakDevServicesConfig config,
                boolean useSharedNetwork, Optional<Duration> timeout,
                List<String> errors, KeycloakDevServicesConfigurator devServicesConfigurator,
                DevServicesComposeProjectBuildItem composeProjectBuildItem) {
            // TODO only sometimes want compose??

            final Optional<ContainerAddress> maybeContainerAddress = KEYCLOAK_DEV_MODE_CONTAINER_LOCATOR.locateContainer(
                    config.serviceName(),
                    config.shared(),
                    LaunchMode.current());

            String imageName = config.imageName().orElse(null); // TODO handle the null properly
            DockerImageName dockerImageName = DockerImageName.parse(imageName).asCompatibleSubstituteFor(imageName);

            oidcContainer = new QuarkusOidcContainer(config, dockerImageName,
                    config.port(),
                    composeProjectBuildItem.getDefaultNetworkId(),
                    useSharedNetwork,
                    config.realmPath().orElse(List.of()),
                    resourcesMap(config, errors),
                    config.serviceName(),
                    config.shared(),
                    config.javaOpts(),
                    config.startCommand(),
                    config.features(),
                    config.showLogs(),
                    config.containerMemoryLimit(),
                    errors);

            timeout.ifPresent(oidcContainer::withStartupTimeout);
            oidcContainer.withEnv(config.containerEnv());
            oidcContainer.start();

            generatedConfig = sortConfig();

            //            return maybeContainerAddress
            //                    .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem, List.of(imageName, "keycloak"),
            //                            KEYCLOAK_PORT, LaunchMode.current(), useSharedNetwork))
            //                    .map(containerAddress -> {
            //                        // TODO: this probably needs to be addressed
            //                        String sharedContainerUrl = getSharedContainerUrl(containerAddress);
            //                        Map<String, String> configs = prepareConfiguration(config,
            //                                sharedContainerUrl,
            //                                sharedContainerUrl, List.of(), errors, devServicesConfigurator, sharedContainerUrl);
            //                        return new RunningDevService(KEYCLOAK_CONTAINER_NAME, containerAddress.getId(), null, configs);
            //                    })
            //                    .orElseGet(defaultKeycloakContainerSupplier);
        }

        // TODO sort the duplication with the parent class
        private Map<String, String> prepareConfiguration(KeycloakDevServicesConfig config,
                String internalURL,
                String hostURL, List<RealmRepresentation> realmReps, List<String> errors,
                KeycloakDevServicesConfigurator devServicesConfigurator, String internalBaseUrl) {
            final String realmName = !realmReps.isEmpty() ? realmReps.iterator().next().getRealm()
                    : getDefaultRealmName();
            final String authServerInternalUrl = realmsURL(internalURL, realmName);

            String clientAuthServerBaseUrl = hostURL != null ? hostURL : internalURL;
            String clientAuthServerUrl = realmsURL(clientAuthServerBaseUrl, realmName);

            // TODO messy
            internalUrl = authServerInternalUrl;

            boolean createDefaultRealm = (realmReps == null || realmReps.isEmpty())
                    && config.createRealm();

            // TODO think these don't need to be dynamic?
            oidcClientId = getOidcClientId(config);
            oidcClientSecret = getOidcClientSecret(config);

            Map<String, String> users = getUsers(config.users(), createDefaultRealm);

            List<String> realmNames = new LinkedList<>();

            if (createDefaultRealm || !realmReps.isEmpty()) {

                // TODO need to close this in the discovery case

                // this needs to be only if we actually start the dev-service as it adds a shutdown hook
                // whose TCCL is the Augmentation CL, which if not removed, causes a massive memory leaks
                if (vertxInstance == null) {
                    vertxInstance = Vertx.vertx();
                }

                WebClient client = createWebClient(vertxInstance);
                try {
                    String adminToken = getAdminToken(config, client, clientAuthServerBaseUrl);
                    if (createDefaultRealm) {
                        createDefaultRealm(config, client, adminToken, clientAuthServerBaseUrl, users, oidcClientId,
                                oidcClientSecret,
                                errors,
                                devServicesConfigurator);
                        realmNames.add(realmName);
                    } else if (realmReps != null) {
                        for (RealmRepresentation realmRep : realmReps) {
                            createRealm(config, client, adminToken, clientAuthServerBaseUrl, realmRep, errors);
                            realmNames.add(realmRep.getRealm());
                        }
                    }

                } finally {
                    client.close();
                }
            }

            Map<String, String> configProperties = new HashMap<>();
            var configPropertiesContext = new KeycloakDevServicesConfigurator.ConfigPropertiesContext(authServerInternalUrl,
                    oidcClientId, oidcClientSecret,
                    internalBaseUrl);
            configProperties.putAll(devServicesConfigurator.createProperties(configPropertiesContext));
            configProperties.put(KEYCLOAK_URL_KEY, internalURL);
            configProperties.put(CLIENT_AUTH_SERVER_URL_CONFIG_KEY, clientAuthServerUrl);
            configProperties.put(OIDC_USERS, users.entrySet().stream().map(Object::toString).collect(Collectors.joining(",")));
            configProperties.put(KEYCLOAK_REALMS, realmNames.stream().collect(Collectors.joining(",")));

            // TODO
            //        keycloakBuildItemBuildProducer
            //                .produce(new KeycloakDevServicesConfigBuildItem(configProperties,
            //                        Map.of(OIDC_USERS, users, KEYCLOAK_REALMS, realmNames), true));

            return configProperties;
        }

        private static String realmsURL(String baseURL, String realmName) {
            return baseURL + "/realms/" + realmName;
        }

        private static String getDefaultRealmName() {
            // TODO merge, no longer works , need to handle better!!!
            //  return capturedDevServicesConfiguration.realmName().orElse("quarkus");
            return "quarkus";
        }

        // TODO what to do with the errors
        private Map<String, String> sortConfig() {
            List<String> errors = new ArrayList<>();
            String internalBaseUrl = getBaseURL((oidcContainer.isHttps() ? "https://" : "http://"), oidcContainer.getHost(),
                    oidcContainer.getPort());
            // TODO overwriting field
            String internalUrl = startURL(internalBaseUrl, oidcContainer.keycloakX);
            String hostUrl = oidcContainer.useSharedNetwork
                    // we need to use auto-detected host and port, so it works when docker host != localhost
                    ? startURL("http://", oidcContainer.getSharedNetworkExternalHost(),
                            oidcContainer.getSharedNetworkExternalPort(),
                            oidcContainer.keycloakX)
                    : null;

            // TODO what to do with this?
            Map<String, String> configs = prepareConfiguration(config, internalUrl, hostUrl,
                    oidcContainer.realmReps, errors, devServicesConfigurator, internalBaseUrl);

            return configs;

        }

        public String authServerInternalUrl() {
            return internalUrl;
        }

        public String getOidcApplicationType() {
            // TODO      return ConfigProvider.getConfig().getOptionalValue(APPLICATION_TYPE_CONFIG_KEY, String.class).orElse("service");
            return "service";
        }

        public String oidcClientId() {
            return oidcClientId;
        }

        public String oidcClientSecret() {
            return oidcClientSecret;
        }
    }

    private static Map<String, String> prepareConfiguration(KeycloakDevServicesConfig config,
            String internalURL,
            String hostURL, List<RealmRepresentation> realmReps, List<String> errors,
            KeycloakDevServicesConfigurator devServicesConfigurator, String internalBaseUrl) {
        final String realmName = !realmReps.isEmpty() ? realmReps.iterator().next().getRealm()
                : getDefaultRealmName(config);
        final String authServerInternalUrl = realmsURL(internalURL, realmName);

        // TODO wrong wrong wrong
        Vertx vertxInstance = null;

        String clientAuthServerBaseUrl = hostURL != null ? hostURL : internalURL;
        String clientAuthServerUrl = realmsURL(clientAuthServerBaseUrl, realmName);

        boolean createDefaultRealm = (realmReps == null || realmReps.isEmpty())
                && config.createRealm();

        String oidcClientId = getOidcClientId(config);
        String oidcClientSecret = getOidcClientSecret(config);

        Map<String, String> users = getUsers(config.users(), createDefaultRealm);

        List<String> realmNames = new LinkedList<>();

        if (createDefaultRealm || !realmReps.isEmpty()) {

            // TODO need to close this in the discovery case

            // this needs to be only if we actually start the dev-service as it adds a shutdown hook
            // whose TCCL is the Augmentation CL, which if not removed, causes a massive memory leaks
            if (vertxInstance == null) {
                vertxInstance = Vertx.vertx();
            }

            WebClient client = createWebClient(vertxInstance);
            try {
                String adminToken = getAdminToken(config, client, clientAuthServerBaseUrl);
                if (createDefaultRealm) {
                    createDefaultRealm(config, client, adminToken, clientAuthServerBaseUrl, users, oidcClientId,
                            oidcClientSecret,
                            errors,
                            devServicesConfigurator);
                    realmNames.add(realmName);
                } else if (realmReps != null) {
                    for (RealmRepresentation realmRep : realmReps) {
                        createRealm(config, client, adminToken, clientAuthServerBaseUrl, realmRep, errors);
                        realmNames.add(realmRep.getRealm());
                    }
                }

            } finally {
                client.close();
            }
        }

        Map<String, String> configProperties = new HashMap<>();
        var configPropertiesContext = new KeycloakDevServicesConfigurator.ConfigPropertiesContext(authServerInternalUrl,
                oidcClientId, oidcClientSecret,
                internalBaseUrl);
        configProperties.putAll(devServicesConfigurator.createProperties(configPropertiesContext));
        configProperties.put(KEYCLOAK_URL_KEY, internalURL);
        configProperties.put(CLIENT_AUTH_SERVER_URL_CONFIG_KEY, clientAuthServerUrl);
        configProperties.put(OIDC_USERS, users.entrySet().stream().map(Object::toString).collect(Collectors.joining(",")));
        configProperties.put(KEYCLOAK_REALMS, realmNames.stream().collect(Collectors.joining(",")));

        // TODO
        //        keycloakBuildItemBuildProducer
        //                .produce(new KeycloakDevServicesConfigBuildItem(configProperties,
        //                        Map.of(OIDC_USERS, users, KEYCLOAK_REALMS, realmNames), true));

        return configProperties;
    }

    private static Map<String, String> resourcesMap(KeycloakDevServicesConfig config, List<String> errors) {
        Map<String, String> resources = new HashMap<>();
        for (Map.Entry<String, String> aliasEntry : config.resourceAliases().entrySet()) {
            if (config.resourceMappings().containsKey(aliasEntry.getKey())) {
                resources.put(aliasEntry.getValue(),
                        config.resourceMappings().get(aliasEntry.getKey()));
            } else {
                errors.add(String.format("%s alias for the %s resource does not have a mapping", aliasEntry.getKey(),
                        aliasEntry.getValue()));
                LOG.errorf("%s alias for the %s resource does not have a mapping", aliasEntry.getKey(),
                        aliasEntry.getValue());
            }
        }
        return resources;
    }

    private static boolean isKeycloakX(KeycloakDevServicesConfig config, DockerImageName dockerImageName) {
        return config.keycloakXImage().isPresent()
                ? config.keycloakXImage().get()
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
        private final String hostName;
        private final boolean keycloakX;
        private final List<RealmRepresentation> realmReps = new LinkedList<>();
        private final Optional<String> startCommand;
        private final Optional<Set<String>> features;
        private final boolean showLogs;
        private final MemorySize containerMemoryLimit;
        private final List<String> errors;

        public QuarkusOidcContainer(KeycloakDevServicesConfig config, DockerImageName dockerImageName,
                OptionalInt fixedExposedPort,
                String defaultNetworkId,
                boolean useSharedNetwork,
                List<String> realmPaths, Map<String, String> resources, String containerLabelValue,
                boolean sharedContainer, Optional<String> javaOpts, Optional<String> startCommand,
                Optional<Set<String>> features, boolean showLogs, MemorySize containerMemoryLimit, List<String> errors) {
            super(dockerImageName);

            this.useSharedNetwork = useSharedNetwork;
            this.realmPaths = realmPaths;
            this.resources = resources;
            this.containerLabelValue = containerLabelValue;
            this.sharedContainer = sharedContainer;
            this.javaOpts = javaOpts;
            this.keycloakX = isKeycloakX(config, dockerImageName);

            if (useSharedNetwork && fixedExposedPort.isEmpty()) {
                // We need to know the port we are exposing when using the shared network, in order to be able to tell
                // Keycloak what the client URL is. This is necessary in order for Keycloak to create the proper 'issuer'
                // when creating tokens
                fixedExposedPort = OptionalInt.of(findRandomPort());
            }

            this.fixedExposedPort = fixedExposedPort;
            this.startCommand = startCommand;
            this.features = features;
            this.showLogs = showLogs;
            this.containerMemoryLimit = containerMemoryLimit;
            this.errors = errors;

            super.setWaitStrategy(Wait.forLogMessage(".*Keycloak.*started.*", 1));
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "keycloak");
        }

        @Override
        protected void configure() {
            super.configure();

            if (useSharedNetwork) {
                if (keycloakX) {
                    addEnv(KEYCLOAK_QUARKUS_HOSTNAME, (isHttps() ? "https://" : "http://") + hostName + ":" + KEYCLOAK_PORT);
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
                withLabel(QUARKUS_DEV_SERVICE, containerLabelValue);
            }

            if (javaOpts.isPresent()) {
                addEnv(JAVA_OPTS, javaOpts.get());
            }

            if (keycloakX) {
                addEnv(KEYCLOAK_QUARKUS_ADMIN_PROP, KEYCLOAK_ADMIN_USER);
                addEnv(KEYCLOAK_QUARKUS_ADMIN_PASSWORD_PROP, KEYCLOAK_ADMIN_PASSWORD);
                String finalStartCommand = startCommand.orElse(KEYCLOAK_QUARKUS_START_CMD);
                if (features.isPresent()) {
                    finalStartCommand += (" --features=" + features.get().stream().collect(Collectors.joining(",")));
                }
                withCommand(finalStartCommand);
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

    private static void createDefaultRealm(KeycloakDevServicesConfig config, WebClient client, String token, String keycloakUrl,
            Map<String, String> users,
            String oidcClientId, String oidcClientSecret, List<String> errors,
            KeycloakDevServicesConfigurator devServicesConfigurator) {
        RealmRepresentation realm = createDefaultRealmRep(config);

        if (config.createClient()) {
            realm.getClients().add(createClient(oidcClientId, oidcClientSecret));
        }
        for (Map.Entry<String, String> entry : users.entrySet()) {
            realm.getUsers().add(createUser(entry.getKey(), entry.getValue(), getUserRoles(config, entry.getKey())));
        }

        devServicesConfigurator.customizeDefaultRealm(realm);

        createRealm(config, client, token, keycloakUrl, realm, errors);
    }

    private static String getAdminToken(KeycloakDevServicesConfig config, WebClient client, String keycloakUrl) {
        try {
            LOG.tracef("Acquiring admin token");

            return getPasswordAccessToken(client,
                    keycloakUrl + "/realms/master/protocol/openid-connect/token",
                    "admin-cli", null, "admin", "admin", null)
                    .await().atMost(config.webClientTimeout());
        } catch (TimeoutException e) {
            LOG.error("Admin token can not be acquired due to a client connection timeout. " +
                    "You may try increasing the `quarkus.keycloak.devservices.web-client-timeout` property.");
        } catch (Throwable t) {
            LOG.error("Admin token can not be acquired", t);
        }
        return null;
    }

    private static void createRealm(KeycloakDevServicesConfig config, WebClient client, String token, String keycloakUrl,
            RealmRepresentation realm,
            List<String> errors) {
        try {
            LOG.tracef("Creating the realm %s", realm.getRealm());
            HttpResponse<Buffer> createRealmResponse = client.postAbs(keycloakUrl + "/admin/realms")
                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token)
                    .sendBuffer(Buffer.buffer().appendString(JsonSerialization.writeValueAsString(realm)))
                    .await().atMost(config.webClientTimeout());

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
        return t -> (t instanceof SocketException
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

    private static List<String> getUserRoles(KeycloakDevServicesConfig config, String user) {
        List<String> roles = config.roles().get(user);
        return roles == null ? ("alice".equals(user) ? List.of("admin", "user") : List.of("user"))
                : roles;
    }

    private static RealmRepresentation createDefaultRealmRep(KeycloakDevServicesConfig config) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(getDefaultRealmName(config));
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(600);
        realm.setSsoSessionMaxLifespan(600);
        realm.setRefreshTokenMaxReuse(10);
        realm.setRequiredActions(List.of());

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        if (config.roles().isEmpty()) {
            realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
            realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        } else {
            Set<String> allRoles = new HashSet<>();
            for (List<String> distinctRoles : config.roles().values()) {
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
        client.setPublicClient(false);
        client.setSecret(oidcClientSecret);
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setImplicitFlowEnabled(true);
        client.setEnabled(true);
        client.setRedirectUris(List.of("*"));
        client.setWebOrigins(List.of("*"));
        client.setDefaultClientScopes(List.of("microprofile-jwt", "basic"));

        return client;
    }

    private static UserRepresentation createUser(String username, String password, List<String> realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(realmRoles);
        user.setEmailVerified(true);
        user.setRequiredActions(List.of());

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        user.getCredentials().add(credential);

        return user;
    }

    private static String getOidcClientId(KeycloakDevServicesConfig config) {
        // if the application type is web-app or hybrid, OidcRecorder will enforce that the client id and secret are configured
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_ID_CONFIG_KEY, String.class)
                .orElse(config.createClient() ? "quarkus-app" : "");
    }

    private static String getOidcClientSecret(KeycloakDevServicesConfig config) {
        // if the application type is web-app or hybrid, OidcRecorder will enforce that the client id and secret are configured
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_SECRET_CONFIG_KEY, String.class)
                .orElse(config.createClient() ? "secret" : "");
    }

}
