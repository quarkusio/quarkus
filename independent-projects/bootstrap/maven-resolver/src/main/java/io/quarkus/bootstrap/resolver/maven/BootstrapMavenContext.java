package io.quarkus.bootstrap.resolver.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.cli.transfer.BatchModeMavenTransferListener;
import org.apache.maven.cli.transfer.ConsoleMavenTransferListener;
import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.smallrye.beanbag.maven.MavenFactory;

public class BootstrapMavenContext {

    private static final Logger log = Logger.getLogger(BootstrapMavenContext.class);

    private static final String BASEDIR = "basedir";
    private static final String DEFAULT_REMOTE_REPO_ID = "central";
    private static final String DEFAULT_REMOTE_REPO_URL = "https://repo.maven.apache.org/maven2";
    private static final String MAVEN_DOT_HOME = "maven.home";
    private static final String MAVEN_HOME = "MAVEN_HOME";
    private static final String MAVEN_SETTINGS = "maven.settings";
    private static final String MAVEN_TOP_LEVEL_PROJECT_BASEDIR = "maven.top-level-basedir";
    private static final String SETTINGS_XML = "settings.xml";
    private static final String SETTINGS_SECURITY = "settings.security";

    private static final String EFFECTIVE_MODEL_BUILDER_PROP = "quarkus.bootstrap.effective-model-builder";
    private static final String WARN_ON_FAILING_WS_MODULES_PROP = "quarkus.bootstrap.warn-on-failing-workspace-modules";

    private static final String MAVEN_RESOLVER_TRANSPORT_KEY = "maven.resolver.transport";
    private static final String MAVEN_RESOLVER_TRANSPORT_DEFAULT = "default";
    private static final String MAVEN_RESOLVER_TRANSPORT_WAGON = "wagon";
    private static final String MAVEN_RESOLVER_TRANSPORT_NATIVE = "native";
    private static final String MAVEN_RESOLVER_TRANSPORT_AUTO = "auto";
    private static final String WAGON_TRANSPORTER_PRIORITY_KEY = "aether.priority.WagonTransporterFactory";
    private static final String NATIVE_HTTP_TRANSPORTER_PRIORITY_KEY = "aether.priority.HttpTransporterFactory";
    private static final String NATIVE_FILE_TRANSPORTER_PRIORITY_KEY = "aether.priority.FileTransporterFactory";
    private static final String RESOLVER_MAX_PRIORITY = String.valueOf(Float.MAX_VALUE);

    private boolean artifactTransferLogging;
    private BootstrapMavenOptions cliOptions;
    private File userSettings;
    private File globalSettings;
    private Boolean offline;

    // Typically, this property will not be enabled in Quarkus application development use-cases
    // It was introduced to support use-cases of using the bootstrap resolver API beyond Quarkus application development
    private Boolean warnOnFailingWorkspaceModules;

    private LocalWorkspace workspace;
    private LocalProject currentProject;
    private Settings settings;
    private List<org.apache.maven.model.Profile> activeSettingsProfiles;
    private RepositorySystem repoSystem;
    private RepositorySystemSession repoSession;
    private List<RemoteRepository> remoteRepos;
    private List<RemoteRepository> remotePluginRepos;
    private RemoteRepositoryManager remoteRepoManager;
    private String localRepo;
    private String[] localRepoTail;
    private Boolean localRepoTailIgnoreAvailability;
    private Path currentPom;
    private Boolean currentProjectExists;
    private String alternatePomName;
    private Path rootProjectDir;
    private boolean preferPomsFromWorkspace;
    private Boolean effectiveModelBuilder;
    private Boolean wsModuleParentHierarchy;
    private SettingsDecrypter settingsDecrypter;
    private final List<String> excludeSisuBeanPackages;
    private final List<String> includeSisuBeanPackages;

    public static BootstrapMavenContextConfig<?> config() {
        return new BootstrapMavenContextConfig<>();
    }

    public BootstrapMavenContext() throws BootstrapMavenException {
        this(new BootstrapMavenContextConfig<>());
    }

    public BootstrapMavenContext(BootstrapMavenContextConfig<?> config)
            throws BootstrapMavenException {
        /*
         * WARNING: this constructor calls instance methods as part of the initialization.
         * This means the values that are available in the config should be set before
         * the instance method invocations.
         */
        this.alternatePomName = config.alternatePomName;
        this.artifactTransferLogging = config.artifactTransferLogging;
        this.localRepo = config.localRepo;
        this.localRepoTail = config.localRepoTail;
        this.localRepoTailIgnoreAvailability = config.localRepoTailIgnoreAvailability;
        this.offline = config.offline;
        this.warnOnFailingWorkspaceModules = config.warnOnFailedWorkspaceModules;
        this.repoSystem = config.repoSystem;
        this.repoSession = config.repoSession;
        this.remoteRepos = config.remoteRepos;
        this.remotePluginRepos = config.remotePluginRepos;
        this.remoteRepoManager = config.remoteRepoManager;
        this.settingsDecrypter = config.settingsDecrypter;
        this.cliOptions = config.cliOptions;
        this.excludeSisuBeanPackages = config.getExcludeSisuBeanPackages();
        this.includeSisuBeanPackages = config.getIncludeSisuBeanPackages();
        if (config.rootProjectDir == null) {
            final String topLevelBaseDirStr = PropertyUtils.getProperty(MAVEN_TOP_LEVEL_PROJECT_BASEDIR);
            if (topLevelBaseDirStr != null) {
                final Path tmp = Path.of(topLevelBaseDirStr);
                if (!Files.exists(tmp)) {
                    throw new BootstrapMavenException("Top-level project base directory " + topLevelBaseDirStr
                            + " specified with system property " + MAVEN_TOP_LEVEL_PROJECT_BASEDIR + " does not exist");
                }
                this.rootProjectDir = tmp;
            }
        } else {
            this.rootProjectDir = config.rootProjectDir;
        }
        this.preferPomsFromWorkspace = config.preferPomsFromWorkspace;
        this.effectiveModelBuilder = config.effectiveModelBuilder;
        this.wsModuleParentHierarchy = config.wsModuleParentHierarchy;
        this.userSettings = config.userSettings;
        if (config.currentProject != null) {
            this.currentProject = config.currentProject;
            this.currentPom = currentProject.getRawModel().getPomFile().toPath();
            this.workspace = config.currentProject.getWorkspace();
        } else if (config.workspaceDiscovery) {
            currentProject = resolveCurrentProject(config.modelProvider);
            this.workspace = currentProject == null ? null : currentProject.getWorkspace();
            if (workspace != null) {
                if (config.repoSession == null && repoSession != null && repoSession.getWorkspaceReader() == null) {
                    repoSession = new DefaultRepositorySystemSession(repoSession).setWorkspaceReader(workspace);
                    if (config.remoteRepos == null && remoteRepos != null) {
                        final List<RemoteRepository> rawProjectRepos = resolveRawProjectRepos(remoteRepos);
                        if (!rawProjectRepos.isEmpty()) {
                            remoteRepos = getRemoteRepositoryManager().aggregateRepositories(repoSession, remoteRepos,
                                    rawProjectRepos, true);
                        }
                    }
                }
            }
        }
    }

    public ArtifactCoords getCurrentProjectArtifact(String extension) throws BootstrapMavenException {
        if (currentProject != null) {
            return currentProject.getAppArtifact(extension);
        }
        final Model model = loadCurrentProjectModel();
        if (model == null) {
            return null;
        }
        return ArtifactCoords.of(ModelUtils.getGroupId(model), model.getArtifactId(), ArtifactCoords.DEFAULT_CLASSIFIER,
                extension, ModelUtils.getVersion(model));
    }

    public LocalProject getCurrentProject() {
        return currentProject;
    }

    public LocalWorkspace getWorkspace() {
        return workspace;
    }

    public BootstrapMavenOptions getCliOptions() {
        return cliOptions == null ? cliOptions = BootstrapMavenOptions.newInstance() : cliOptions;
    }

    public File getUserSettings() {
        if (userSettings == null) {
            final String quarkusMavenSettings = getProperty(MAVEN_SETTINGS);
            if (quarkusMavenSettings != null) {
                var f = new File(quarkusMavenSettings);
                return userSettings = f.exists() ? f : null;
            }
            return userSettings = resolveSettingsFile(
                    getCliOptions().getOptionValue(BootstrapMavenOptions.ALTERNATE_USER_SETTINGS),
                    () -> new File(getUserMavenConfigurationHome(), SETTINGS_XML));
        }
        return userSettings;
    }

    private static File getUserMavenConfigurationHome() {
        return new File(PropertyUtils.getUserHome(), ".m2");
    }

    private String getProperty(String name) {
        String value = PropertyUtils.getProperty(name);
        if (value != null) {
            return value;
        }
        final Properties props = getCliOptions().getSystemProperties();
        return props == null ? null : props.getProperty(name);
    }

    public File getGlobalSettings() {
        return globalSettings == null
                ? globalSettings = resolveSettingsFile(
                        getCliOptions().getOptionValue(BootstrapMavenOptions.ALTERNATE_GLOBAL_SETTINGS),
                        () -> {
                            String mavenHome = getProperty(MAVEN_DOT_HOME);
                            if (mavenHome == null) {
                                mavenHome = System.getenv(MAVEN_HOME);
                                if (mavenHome == null) {
                                    mavenHome = "";
                                }
                            }
                            return new File(mavenHome, "conf/settings.xml");
                        })
                : globalSettings;
    }

    public boolean isOffline() throws BootstrapMavenException {
        return offline == null
                ? offline = (getCliOptions().hasOption(BootstrapMavenOptions.OFFLINE) || getEffectiveSettings().isOffline())
                : offline;
    }

    public boolean isWarnOnFailingWorkspaceModules() {
        return warnOnFailingWorkspaceModules == null
                ? warnOnFailingWorkspaceModules = Boolean.getBoolean(WARN_ON_FAILING_WS_MODULES_PROP)
                : warnOnFailingWorkspaceModules;
    }

    public RepositorySystem getRepositorySystem() throws BootstrapMavenException {
        if (repoSystem == null) {
            initRepoSystemAndManager();
        }
        return repoSystem;
    }

    public RemoteRepositoryManager getRemoteRepositoryManager() {
        if (remoteRepoManager == null) {
            initRepoSystemAndManager();
        }
        return remoteRepoManager;
    }

    public RepositorySystemSession getRepositorySystemSession() throws BootstrapMavenException {
        return repoSession == null ? repoSession = newRepositorySystemSession() : repoSession;
    }

    public List<RemoteRepository> getRemoteRepositories() throws BootstrapMavenException {
        return remoteRepos == null ? remoteRepos = resolveRemoteRepos() : remoteRepos;
    }

    public List<RemoteRepository> getRemotePluginRepositories() throws BootstrapMavenException {
        return remotePluginRepos == null ? remotePluginRepos = resolveRemotePluginRepos() : remotePluginRepos;
    }

    public SettingsDecrypter getSettingsDecrypter() {
        if (settingsDecrypter == null) {
            initRepoSystemAndManager();
        }
        return settingsDecrypter;
    }

    public Settings getEffectiveSettings() throws BootstrapMavenException {
        if (settings != null) {
            return settings;
        }

        final DefaultSettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest()
                .setSystemProperties(System.getProperties())
                .setUserSettingsFile(getUserSettings())
                .setGlobalSettingsFile(getGlobalSettings());

        final Properties cmdLineProps = getCliOptions().getSystemProperties();
        if (cmdLineProps != null) {
            settingsRequest.setUserProperties(cmdLineProps);
        }

        final Settings effectiveSettings;
        try {
            final SettingsBuildingResult result = new DefaultSettingsBuilderFactory()
                    .newInstance().build(settingsRequest);
            final List<SettingsProblem> problems = result.getProblems();
            if (!problems.isEmpty()) {
                for (SettingsProblem problem : problems) {
                    switch (problem.getSeverity()) {
                        case ERROR:
                        case FATAL:
                            throw new BootstrapMavenException("Settings problem encountered at " + problem.getLocation(),
                                    problem.getException());
                        default:
                            log.warn("Settings problem encountered at " + problem.getLocation(), problem.getException());
                    }
                }
            }
            effectiveSettings = result.getEffectiveSettings();
        } catch (SettingsBuildingException e) {
            throw new BootstrapMavenException("Failed to initialize Maven repository settings", e);
        }
        return settings = effectiveSettings;
    }

    public String getLocalRepo() throws BootstrapMavenException {
        return localRepo == null ? localRepo = resolveLocalRepo(getEffectiveSettings()) : localRepo;
    }

    private String[] getLocalRepoTail() {
        return localRepoTail == null ? localRepoTail = resolveLocalRepoTail() : localRepoTail;
    }

    private boolean getLocalRepoTailIgnoreAvailability() {
        return localRepoTailIgnoreAvailability == null
                ? localRepoTailIgnoreAvailability = resolveLocalRepoTailIgnoreAvailability()
                : localRepoTailIgnoreAvailability;
    }

    private LocalProject resolveCurrentProject(Function<Path, Model> modelProvider) throws BootstrapMavenException {
        try {
            return LocalProject.loadWorkspace(this, modelProvider);
        } catch (Exception e) {
            throw new BootstrapMavenException("Failed to load current project at " + getCurrentProjectPomOrNull(), e);
        }
    }

    private String resolveLocalRepo(Settings settings) {
        String localRepo = System.getenv("QUARKUS_LOCAL_REPO");
        if (localRepo != null) {
            return localRepo;
        }
        localRepo = getProperty("maven.repo.local");
        if (localRepo != null) {
            return localRepo;
        }
        localRepo = settings.getLocalRepository();
        return localRepo == null ? new File(getUserMavenConfigurationHome(), "repository").getAbsolutePath() : localRepo;
    }

    private String[] resolveLocalRepoTail() {
        final String localRepoTail = getProperty("maven.repo.local.tail");
        if (localRepoTail == null) {
            return new String[] {};
        }
        if (localRepoTail.trim().isEmpty()) {
            return new String[] {};
        }
        return localRepoTail.split(",");
    }

    private boolean resolveLocalRepoTailIgnoreAvailability() {
        final String ignoreAvailability = getProperty("maven.repo.local.tail.ignoreAvailability");

        // The only "falsy" value is `false` itself
        if ("false".equalsIgnoreCase(ignoreAvailability)) {
            return false;
        }

        //All other strings are interpreted as `true`.
        return true;
    }

    private File resolveSettingsFile(String settingsArg, Supplier<File> supplier) {
        File userSettings;
        if (settingsArg != null) {
            userSettings = new File(settingsArg);
            if (userSettings.exists()) {
                return userSettings;
            }
            if (userSettings.isAbsolute()) {
                return null;
            }

            // in case the settings path is a relative one we check whether the pom path is also a relative one
            // in which case we can resolve the settings path relative to the project directory
            // otherwise, we don't have a clue what the settings path is relative to
            String alternatePomDir = getCliOptions().getOptionValue(BootstrapMavenOptions.ALTERNATE_POM_FILE);
            if (alternatePomDir != null) {
                File tmp = new File(alternatePomDir);
                if (tmp.isAbsolute()) {
                    alternatePomDir = null;
                } else {
                    if (!tmp.isDirectory()) {
                        tmp = tmp.getParentFile();
                    }
                    alternatePomDir = tmp == null ? null : tmp.toString();
                }
            }

            // Root project base dir
            userSettings = resolveSettingsFile(settingsArg, alternatePomDir, System.getenv("MAVEN_PROJECTBASEDIR"));
            if (userSettings != null) {
                return userSettings;
            }
            // current module project base dir
            userSettings = resolveSettingsFile(settingsArg, alternatePomDir, PropertyUtils.getProperty(BASEDIR));
            if (userSettings != null) {
                return userSettings;
            }
            userSettings = new File(PropertyUtils.getUserHome(), settingsArg);
            if (userSettings.exists()) {
                return userSettings;
            }
        }
        userSettings = supplier.get();
        return userSettings.exists() ? userSettings : null;
    }

    private File resolveSettingsFile(String settingsArg, String alternatePomDir, String projectBaseDir) {
        if (projectBaseDir == null) {
            return null;
        }
        File userSettings;
        if (alternatePomDir != null && projectBaseDir.endsWith(alternatePomDir)) {
            userSettings = new File(projectBaseDir.substring(0, projectBaseDir.length() - alternatePomDir.length()),
                    settingsArg);
            if (userSettings.exists()) {
                return userSettings;
            }
        }
        userSettings = new File(projectBaseDir, settingsArg);
        if (userSettings.exists()) {
            return userSettings;
        }
        return null;
    }

    private DefaultRepositorySystemSession newRepositorySystemSession() throws BootstrapMavenException {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        final Settings settings = getEffectiveSettings();
        final List<Mirror> mirrors = settings.getMirrors();
        if (mirrors != null && !mirrors.isEmpty()) {
            final DefaultMirrorSelector ms = new DefaultMirrorSelector();
            for (Mirror m : mirrors) {
                ms.add(m.getId(), m.getUrl(), m.getLayout(), false, m.isBlocked(), m.getMirrorOf(), m.getMirrorOfLayouts());
            }
            session.setMirrorSelector(ms);
        }

        final String localRepoPath = getLocalRepo();
        final String[] localRepoTailPaths = getLocalRepoTail();

        final LocalRepositoryManager head = getRepositorySystem().newLocalRepositoryManager(session,
                new LocalRepository(localRepoPath));

        if (localRepoTailPaths.length == 0) {
            session.setLocalRepositoryManager(head);
        } else {
            final List<LocalRepositoryManager> tail = new ArrayList<>(localRepoTailPaths.length);
            for (final String tailPath : localRepoTailPaths) {
                tail.add(getRepositorySystem().newLocalRepositoryManager(session, new LocalRepository(tailPath)));
            }
            session.setLocalRepositoryManager(
                    new ChainedLocalRepositoryManager(head, tail, getLocalRepoTailIgnoreAvailability()));
        }

        session.setOffline(isOffline());

        final BootstrapMavenOptions mvnArgs = getCliOptions();
        if (!mvnArgs.isEmpty()) {
            if (mvnArgs.hasOption(BootstrapMavenOptions.SUPRESS_SNAPSHOT_UPDATES)) {
                session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
            } else if (mvnArgs.hasOption(BootstrapMavenOptions.UPDATE_SNAPSHOTS)) {
                session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
            }
            if (mvnArgs.hasOption(BootstrapMavenOptions.CHECKSUM_FAILURE_POLICY)) {
                session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
            } else if (mvnArgs.hasOption(BootstrapMavenOptions.CHECKSUM_WARNING_POLICY)) {
                session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
            }
        }

        final SettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies(settings.getProxies());
        decrypt.setServers(settings.getServers());
        // set settings.security property to ~/.m2/settings-security.xml unless it's already set to some other value
        File settingsSecurityXml = null;
        final boolean setSettingsSecurity = !System.getProperties().contains(SETTINGS_SECURITY)
                && ((settingsSecurityXml = new File(getUserMavenConfigurationHome(), "settings-security.xml")).exists());
        if (setSettingsSecurity) {
            System.setProperty(SETTINGS_SECURITY, settingsSecurityXml.toString());
        }
        final SettingsDecryptionResult decrypted = getSettingsDecrypter().decrypt(decrypt);
        if (setSettingsSecurity) {
            System.clearProperty(SETTINGS_SECURITY);
        }
        if (!decrypted.getProblems().isEmpty() && log.isDebugEnabled()) {
            // this is how maven handles these
            for (SettingsProblem p : decrypted.getProblems()) {
                log.debug(p.getMessage(), p.getException());
            }
        }

        final DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (org.apache.maven.settings.Proxy p : decrypted.getProxies()) {
            if (p.isActive()) {
                proxySelector.add(toAetherProxy(p), p.getNonProxyHosts());
            }
        }
        session.setProxySelector(proxySelector);

        final Map<Object, Object> configProps = new LinkedHashMap<>(session.getConfigProperties());
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, settings.isInteractiveMode());

        final DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : decrypted.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            authSelector.add(server.getId(), authBuilder.build());

            if (server.getConfiguration() != null) {
                Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for (int i = dom.getChildCount() - 1; i >= 0; i--) {
                    Xpp3Dom child = dom.getChild(i);
                    if ("wagonProvider".equals(child.getName())) {
                        dom.removeChild(i);
                    }
                }
                XmlPlexusConfiguration config = new XmlPlexusConfiguration(dom);
                configProps.put("aether.connector.wagon.config." + server.getId(), config);

                // Translate to proper resolver configuration properties as well (as Plexus XML above is Wagon specific
                // only), but support only configuration/httpConfiguration/all, see
                // https://maven.apache.org/guides/mini/guide-http-settings.html
                Map<String, String> headers = null;
                Integer connectTimeout = null;
                Integer requestTimeout = null;

                PlexusConfiguration httpHeaders = config.getChild("httpHeaders", false);
                if (httpHeaders != null) {
                    PlexusConfiguration[] properties = httpHeaders.getChildren("property");
                    if (properties != null && properties.length > 0) {
                        headers = new HashMap<>();
                        for (PlexusConfiguration property : properties) {
                            headers.put(
                                    property.getChild("name").getValue(),
                                    property.getChild("value").getValue());
                        }
                    }
                }

                PlexusConfiguration connectTimeoutXml = config.getChild("connectTimeout", false);
                if (connectTimeoutXml != null) {
                    connectTimeout = Integer.parseInt(connectTimeoutXml.getValue());
                } else {
                    // fallback configuration name
                    PlexusConfiguration httpConfiguration = config.getChild("httpConfiguration", false);
                    if (httpConfiguration != null) {
                        PlexusConfiguration httpConfigurationAll = httpConfiguration.getChild("all", false);
                        if (httpConfigurationAll != null) {
                            connectTimeoutXml = httpConfigurationAll.getChild("connectionTimeout", false);
                            if (connectTimeoutXml != null) {
                                connectTimeout = Integer.parseInt(connectTimeoutXml.getValue());
                                log.warn("Settings for server " + server.getId() + " uses legacy format");
                            }
                        }
                    }
                }

                PlexusConfiguration requestTimeoutXml = config.getChild("requestTimeout", false);
                if (requestTimeoutXml != null) {
                    requestTimeout = Integer.parseInt(requestTimeoutXml.getValue());
                } else {
                    // fallback configuration name
                    PlexusConfiguration httpConfiguration = config.getChild("httpConfiguration", false);
                    if (httpConfiguration != null) {
                        PlexusConfiguration httpConfigurationAll = httpConfiguration.getChild("all", false);
                        if (httpConfigurationAll != null) {
                            requestTimeoutXml = httpConfigurationAll.getChild("readTimeout", false);
                            if (requestTimeoutXml != null) {
                                requestTimeout = Integer.parseInt(requestTimeoutXml.getValue());
                                log.warn("Settings for server " + server.getId() + " uses legacy format");
                            }
                        }
                    }
                }

                // org.eclipse.aether.ConfigurationProperties.HTTP_HEADERS => Map<String, String>
                if (headers != null) {
                    configProps.put(ConfigurationProperties.HTTP_HEADERS + "." + server.getId(), headers);
                }
                // org.eclipse.aether.ConfigurationProperties.CONNECT_TIMEOUT => int
                if (connectTimeout != null) {
                    configProps.put(ConfigurationProperties.CONNECT_TIMEOUT + "." + server.getId(), connectTimeout);
                }
                // org.eclipse.aether.ConfigurationProperties.REQUEST_TIMEOUT => int
                if (requestTimeout != null) {
                    configProps.put(ConfigurationProperties.REQUEST_TIMEOUT + "." + server.getId(), requestTimeout);
                }
            }
            configProps.put("aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions());
            configProps.put("aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions());
        }
        session.setAuthenticationSelector(authSelector);

        Object transport = configProps.getOrDefault(MAVEN_RESOLVER_TRANSPORT_KEY, MAVEN_RESOLVER_TRANSPORT_DEFAULT);
        if (MAVEN_RESOLVER_TRANSPORT_DEFAULT.equals(transport)) {
            // The "default" mode (user did not set anything) from now on defaults to AUTO
        } else if (MAVEN_RESOLVER_TRANSPORT_NATIVE.equals(transport)) {
            // Make sure (whatever extra priority is set) that resolver native is selected
            configProps.put(NATIVE_FILE_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
            configProps.put(NATIVE_HTTP_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (MAVEN_RESOLVER_TRANSPORT_WAGON.equals(transport)) {
            // Make sure (whatever extra priority is set) that wagon is selected
            configProps.put(WAGON_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (!MAVEN_RESOLVER_TRANSPORT_AUTO.equals(transport)) {
            throw new IllegalArgumentException("Unknown resolver transport '" + transport
                    + "'. Supported transports are: " + MAVEN_RESOLVER_TRANSPORT_WAGON + ", "
                    + MAVEN_RESOLVER_TRANSPORT_NATIVE + ", " + MAVEN_RESOLVER_TRANSPORT_AUTO);
        }

        session.setConfigProperties(configProps);

        if (session.getCache() == null) {
            session.setCache(new DefaultRepositoryCache());
        }

        if (workspace != null) {
            session.setWorkspaceReader(workspace);
        }

        if (session.getTransferListener() == null && artifactTransferLogging) {
            TransferListener transferListener;
            if (mvnArgs.hasOption(BootstrapMavenOptions.NO_TRANSFER_PROGRESS)) {
                transferListener = new QuietMavenTransferListener();
            } else if (mvnArgs.hasOption(BootstrapMavenOptions.BATCH_MODE)) {
                transferListener = new BatchModeMavenTransferListener(System.out);
            } else {
                transferListener = new ConsoleMavenTransferListener(System.out, true);
            }

            session.setTransferListener(transferListener);
        }

        for (var e : System.getProperties().entrySet()) {
            session.setSystemProperty(e.getKey().toString(), e.getValue().toString());
        }

        return session;
    }

    private List<RemoteRepository> resolveRemoteRepos() throws BootstrapMavenException {
        final List<RemoteRepository> rawRepos = new ArrayList<>();
        readMavenReposFromEnv(rawRepos, System.getenv());
        addReposFromProfiles(rawRepos);

        final boolean centralConfiguredInSettings = includesDefaultRepo(rawRepos);
        if (!centralConfiguredInSettings) {
            rawRepos.add(newDefaultRepository());
        }

        if (workspace == null) {
            return newResolutionRepos(rawRepos);
        }

        final List<RemoteRepository> rawProjectRepos = resolveRawProjectRepos(newResolutionRepos(rawRepos));
        if (!rawProjectRepos.isEmpty()) {
            if (!centralConfiguredInSettings) {
                // if the default repo was added here, we are removing it to add it last
                rawRepos.remove(rawRepos.size() - 1);
            }
            rawRepos.addAll(rawProjectRepos);
            if (!centralConfiguredInSettings && !includesDefaultRepo(rawProjectRepos)) {
                rawRepos.add(newDefaultRepository());
            }
        }

        return newResolutionRepos(rawRepos);
    }

    private List<RemoteRepository> resolveRemotePluginRepos() throws BootstrapMavenException {
        final List<RemoteRepository> rawRepos = new ArrayList<>();
        addReposFromProfiles(rawRepos);
        // central must be there
        if (!includesDefaultRepo(rawRepos)) {
            rawRepos.add(newDefaultRepository());
        }
        return newResolutionRepos(rawRepos);
    }

    private List<RemoteRepository> newResolutionRepos(final List<RemoteRepository> rawRepos)
            throws BootstrapMavenException {
        return getRepositorySystem().newResolutionRepositories(getRepositorySystemSession(), rawRepos);
    }

    private void addReposFromProfiles(final List<RemoteRepository> rawRepos) throws BootstrapMavenException {
        // reverse the order of the profiles to match the order in which the repos appear in Maven mojos
        final List<org.apache.maven.model.Profile> profiles = getActiveSettingsProfiles();
        for (int i = profiles.size() - 1; i >= 0; --i) {
            addProfileRepos(profiles.get(i).getRepositories(), rawRepos);
        }
    }

    public static RemoteRepository newDefaultRepository() {
        return new RemoteRepository.Builder(DEFAULT_REMOTE_REPO_ID, "default", DEFAULT_REMOTE_REPO_URL)
                .setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY,
                        RepositoryPolicy.CHECKSUM_POLICY_WARN))
                .setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY,
                        RepositoryPolicy.CHECKSUM_POLICY_WARN))
                .build();
    }

    private Model loadCurrentProjectModel() throws BootstrapMavenException {
        final Path pom = getCurrentProjectPomOrNull();
        if (pom == null) {
            return null;
        }
        try {
            return ModelUtils.readModel(pom);
        } catch (IOException e) {
            throw new BootstrapMavenException("Failed to parse " + pom, e);
        }
    }

    private List<RemoteRepository> resolveRawProjectRepos(List<RemoteRepository> repos)
            throws BootstrapMavenException {
        final Artifact projectArtifact;
        if (currentProject == null) {
            final Model model = loadCurrentProjectModel();
            if (model == null) {
                return List.of();
            }
            projectArtifact = new DefaultArtifact(ModelUtils.getGroupId(model), model.getArtifactId(),
                    ArtifactCoords.DEFAULT_CLASSIFIER, ArtifactCoords.TYPE_POM, ModelUtils.getVersion(model));
        } else {
            projectArtifact = new DefaultArtifact(currentProject.getGroupId(), currentProject.getArtifactId(),
                    ArtifactCoords.DEFAULT_CLASSIFIER, ArtifactCoords.TYPE_POM, currentProject.getVersion());
        }

        final RepositorySystem repoSystem = getRepositorySystem();
        final RepositorySystemSession repoSession = getRepositorySystemSession();
        try {
            return repoSystem
                    .readArtifactDescriptor(repoSession, new ArtifactDescriptorRequest()
                            .setArtifact(projectArtifact)
                            .setRepositories(repos))
                    .getRepositories();
        } catch (ArtifactDescriptorException e) {
            throw new BootstrapMavenException("Failed to read artifact descriptor for " + projectArtifact, e);
        }
    }

    public List<org.apache.maven.model.Profile> getActiveSettingsProfiles()
            throws BootstrapMavenException {
        if (activeSettingsProfiles != null) {
            return activeSettingsProfiles;
        }

        final Settings settings = getEffectiveSettings();
        final List<Profile> allSettingsProfiles = settings.getProfiles();
        if (allSettingsProfiles.isEmpty()) {
            return activeSettingsProfiles = List.of();
        }

        final BootstrapMavenOptions mvnArgs = getCliOptions();
        final Path currentPom = getCurrentProjectPomOrNull();
        final DefaultProfileActivationContext context = new DefaultProfileActivationContext()
                .setActiveProfileIds(mvnArgs.getActiveProfileIds())
                .setInactiveProfileIds(mvnArgs.getInactiveProfileIds())
                .setSystemProperties(System.getProperties())
                .setProjectDirectory(
                        currentPom == null ? getCurrentProjectBaseDir().toFile() : currentPom.getParent().toFile());
        final DefaultProfileSelector profileSelector = new DefaultProfileSelector()
                .addProfileActivator(new PropertyProfileActivator())
                .addProfileActivator(new JdkVersionProfileActivator())
                .addProfileActivator(new OperatingSystemProfileActivator())
                .addProfileActivator(createFileProfileActivator());
        List<org.apache.maven.model.Profile> selectedProfiles = profileSelector
                .getActiveProfiles(toModelProfiles(allSettingsProfiles), context, new ModelProblemCollector() {
                    public void add(ModelProblemCollectorRequest req) {
                        log.error("Failed to activate a Maven profile: " + req.getMessage());
                    }
                });

        if (!settings.getActiveProfiles().isEmpty()) {
            final Set<String> activeProfiles = new HashSet<>(settings.getActiveProfiles());
            // remove already activated to avoid duplicates
            selectedProfiles.forEach(p -> activeProfiles.remove(p.getId()));
            if (!activeProfiles.isEmpty()) {
                final List<org.apache.maven.model.Profile> allActiveProfiles = new ArrayList<>(
                        selectedProfiles.size() + activeProfiles.size());
                allActiveProfiles.addAll(selectedProfiles);
                for (Profile profile : allSettingsProfiles) {
                    if (activeProfiles.contains(profile.getId())) {
                        allActiveProfiles.add(SettingsUtils.convertFromSettingsProfile(profile));
                    }
                }
                selectedProfiles = allActiveProfiles;
            }
        }
        return activeSettingsProfiles = selectedProfiles;
    }

    private static List<org.apache.maven.model.Profile> toModelProfiles(List<Profile> profiles) {
        final List<org.apache.maven.model.Profile> result = new ArrayList<>(profiles.size());
        for (Profile p : profiles) {
            result.add(SettingsUtils.convertFromSettingsProfile(p));
        }
        return result;
    }

    private static boolean includesDefaultRepo(List<RemoteRepository> repositories) {
        if (repositories.isEmpty()) {
            return false;
        }
        for (ArtifactRepository repository : repositories) {
            if (repository.getId().equals(DEFAULT_REMOTE_REPO_ID)) {
                return true;
            }
        }
        return false;
    }

    private static void addProfileRepos(List<org.apache.maven.model.Repository> repositories,
            final List<RemoteRepository> all) {
        for (org.apache.maven.model.Repository repo : repositories) {
            final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repo.getId(), repo.getLayout(),
                    repo.getUrl());
            org.apache.maven.model.RepositoryPolicy policy = repo.getReleases();
            if (policy != null) {
                repoBuilder.setReleasePolicy(toAetherRepoPolicy(policy));
            }
            policy = repo.getSnapshots();
            if (policy != null) {
                repoBuilder.setSnapshotPolicy(toAetherRepoPolicy(policy));
            }
            all.add(repoBuilder.build());
        }
    }

    private static RepositoryPolicy toAetherRepoPolicy(org.apache.maven.model.RepositoryPolicy modelPolicy) {
        return new RepositoryPolicy(modelPolicy.isEnabled(),
                isEmpty(modelPolicy.getUpdatePolicy()) ? RepositoryPolicy.UPDATE_POLICY_DAILY
                        : modelPolicy.getUpdatePolicy(),
                isEmpty(modelPolicy.getChecksumPolicy()) ? RepositoryPolicy.CHECKSUM_POLICY_WARN
                        : modelPolicy.getChecksumPolicy());
    }

    private static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Convert a {@link org.apache.maven.settings.Proxy} to a {@link Proxy}.
     *
     * @param proxy Maven proxy settings, may be {@code null}.
     * @return Aether repository proxy or {@code null} if given {@link org.apache.maven.settings.Proxy} is {@code null}.
     */
    private static Proxy toAetherProxy(org.apache.maven.settings.Proxy proxy) {
        if (proxy == null) {
            return null;
        }
        Authentication auth = null;
        if (proxy.getUsername() != null) {
            auth = new AuthenticationBuilder()
                    .addUsername(proxy.getUsername())
                    .addPassword(proxy.getPassword())
                    .build();
        }
        return new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth);
    }

    private void initRepoSystemAndManager() {
        final MavenFactory factory = configureMavenFactory();
        if (repoSystem == null) {
            repoSystem = factory.getRepositorySystem();
        }
        if (remoteRepoManager == null) {
            remoteRepoManager = factory.getContainer().requireBean(RemoteRepositoryManager.class);
        }
        if (settingsDecrypter == null) {
            settingsDecrypter = factory.getContainer().requireBean(SettingsDecrypter.class);
        }
    }

    protected MavenFactory configureMavenFactory() {
        return MavenFactory.create(RepositorySystem.class.getClassLoader(), builder -> {
            for (var pkg : includeSisuBeanPackages) {
                builder.includePackage(pkg);
            }
            for (var pkg : excludeSisuBeanPackages) {
                builder.excludePackage(pkg);
            }
            builder.addBean(ModelBuilder.class)
                    .setSupplier(scope -> new MavenModelBuilder(BootstrapMavenContext.this))
                    .setPriority(100).build();
        });
    }

    private static String getUserAgent() {
        return "Apache-Maven/" + getMavenVersion() + " (Java " + PropertyUtils.getProperty("java.version") + "; "
                + PropertyUtils.getProperty("os.name") + " " + PropertyUtils.getProperty("os.version") + ")";
    }

    private static String getMavenVersion() {
        final String mvnVersion = PropertyUtils.getProperty("maven.version");
        if (mvnVersion != null) {
            return mvnVersion;
        }
        final Properties props = new Properties();
        try (InputStream is = BootstrapMavenContext.class.getResourceAsStream(
                "/META-INF/maven/org.apache.maven/maven-core/pom.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            log.debug("Failed to read Maven version", e);
        }
        return props.getProperty("version", "unknown-version");
    }

    public boolean isCurrentProjectExists() {
        return currentProjectExists == null
                ? currentProjectExists = getCurrentProjectPomOrNull() != null
                : currentProjectExists;
    }

    public Path getCurrentProjectPomOrNull() {
        if (currentPom != null
                || currentProjectExists != null && !currentProjectExists) {
            return currentPom;
        }
        final Path pom = resolveCurrentPom();
        return currentPom = (currentProjectExists = pom != null) ? pom : null;
    }

    private Path resolveCurrentPom() {
        Path alternatePom = null;
        // explicitly set absolute path has a priority
        if (alternatePomName != null) {
            alternatePom = Paths.get(alternatePomName);
            if (alternatePom.isAbsolute()) {
                return pomXmlOrNull(alternatePom);
            }
        }

        if (alternatePom == null) {
            // check whether an alternate pom was provided as a CLI arg
            final String cliPomName = getCliOptions().getOptionValue(BootstrapMavenOptions.ALTERNATE_POM_FILE);
            if (cliPomName != null) {
                alternatePom = Path.of(cliPomName);
            }
        }

        final String basedirProp = PropertyUtils.getProperty(BASEDIR);
        if (basedirProp != null) {
            // this is the actual current project dir
            return getPomForDirOrNull(Path.of(basedirProp), alternatePom);
        }

        // we are not in the context of a Maven build
        if (alternatePom != null && alternatePom.isAbsolute()) {
            return pomXmlOrNull(alternatePom);
        }

        // trying the current dir as the basedir
        final Path basedir = Path.of("").normalize().toAbsolutePath();
        if (alternatePom != null) {
            return pomXmlOrNull(basedir.resolve(alternatePom));
        }
        final Path pom = basedir.resolve(LocalProject.POM_XML);
        return Files.exists(pom) ? pom : null;
    }

    static Path getPomForDirOrNull(final Path basedir, Path alternatePom) {
        if (alternatePom != null) {
            if (alternatePom.getNameCount() == 1 || basedir.endsWith(alternatePom.getParent())) {
                if (alternatePom.isAbsolute()) {
                    // if the basedir matches the parent of the alternate pom, it's the alternate pom
                    return alternatePom;
                }
                // if the basedir ends with the alternate POM parent relative path, we can try it as the base dir
                final Path pom = basedir.resolve(alternatePom.getFileName());
                if (Files.exists(pom)) {
                    return pom;
                }
            }
        }

        final Path pom = basedir.resolve(LocalProject.POM_XML);
        if (Files.exists(pom)) {
            return pom;
        }

        // give up
        return null;
    }

    private static Path pomXmlOrNull(Path path) {
        if (Files.isDirectory(path)) {
            path = path.resolve(LocalProject.POM_XML);
        }
        return Files.exists(path) ? path.normalize() : null;
    }

    public Path getCurrentProjectBaseDir() {
        if (currentProject != null) {
            return currentProject.getDir();
        }
        final String basedirProp = PropertyUtils.getProperty(BASEDIR);
        return basedirProp == null ? Path.of("").normalize().toAbsolutePath() : Paths.get(basedirProp);
    }

    public Path getRootProjectBaseDir() {
        // originally we checked for MAVEN_PROJECTBASEDIR which is set by the mvn script
        // and points to the first parent containing '.mvn' dir but it's not consistent
        // with how Maven discovers the workspace and also created issues testing the Quarkus platform
        // due to its specific FS layout
        return rootProjectDir;
    }

    public boolean isPreferPomsFromWorkspace() {
        return preferPomsFromWorkspace;
    }

    public boolean isEffectiveModelBuilder() {
        if (effectiveModelBuilder == null) {
            final String s = PropertyUtils.getProperty(EFFECTIVE_MODEL_BUILDER_PROP);
            effectiveModelBuilder = s == null ? false : Boolean.parseBoolean(s);
        }
        return effectiveModelBuilder;
    }

    public boolean isWorkspaceModuleParentHierarchy() {
        return wsModuleParentHierarchy == null ? false : wsModuleParentHierarchy;
    }

    static final String BOOTSTRAP_MAVEN_REPOS = "BOOTSTRAP_MAVEN_REPOS";
    static final String BOOTSTRAP_MAVEN_REPO_PREFIX = "BOOTSTRAP_MAVEN_REPO_";
    static final String URL_SUFFIX = "_URL";
    static final String SNAPSHOT_SUFFIX = "_SNAPSHOT";
    static final String RELEASE_SUFFIX = "_RELEASE";

    static void readMavenReposFromEnv(List<RemoteRepository> repos, Map<String, String> env) {
        final String envRepos = env.get(BOOTSTRAP_MAVEN_REPOS);
        if (envRepos == null || envRepos.isBlank()) {
            return;
        }
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < envRepos.length(); ++i) {
            final char c = envRepos.charAt(i);
            if (c == ',') {
                initMavenRepoFromEnv(envRepos, buf.toString(), env, repos);
                buf.setLength(0);
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            initMavenRepoFromEnv(envRepos, buf.toString(), env, repos);
        }
    }

    private static void initMavenRepoFromEnv(String envRepos, String repoId, Map<String, String> env,
            List<RemoteRepository> repos) {
        final String envRepoId = toEnvVarPart(repoId);
        String repoUrl = null;
        boolean snapshot = true;
        boolean release = true;
        for (Map.Entry<String, String> envvar : env.entrySet()) {
            final String varName = envvar.getKey();
            if (varName.startsWith(BOOTSTRAP_MAVEN_REPO_PREFIX)
                    && varName.regionMatches(BOOTSTRAP_MAVEN_REPO_PREFIX.length(), envRepoId, 0, envRepoId.length())) {
                if (isMavenRepoEnvVarOption(varName, repoId, URL_SUFFIX)) {
                    repoUrl = envvar.getValue();
                } else if (isMavenRepoEnvVarOption(varName, repoId, SNAPSHOT_SUFFIX)) {
                    snapshot = Boolean.parseBoolean(envvar.getValue());
                } else if (isMavenRepoEnvVarOption(varName, repoId, RELEASE_SUFFIX)) {
                    release = Boolean.parseBoolean(envvar.getValue());
                }
            }
        }
        if (repoUrl == null || repoUrl.isBlank()) {
            log.warn("Maven repository " + repoId + " listed in " + BOOTSTRAP_MAVEN_REPOS + "=" + envRepos
                    + " was ignored because the corresponding " + BOOTSTRAP_MAVEN_REPO_PREFIX + envRepoId + URL_SUFFIX
                    + " is missing");
        } else {
            final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repoId, "default", repoUrl);
            if (!release) {
                repoBuilder.setReleasePolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY,
                        RepositoryPolicy.CHECKSUM_POLICY_WARN));
            }
            if (!snapshot) {
                repoBuilder.setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY,
                        RepositoryPolicy.CHECKSUM_POLICY_WARN));
            }
            repos.add(repoBuilder.build());
        }
    }

    private static String toEnvVarPart(String s) {
        final StringBuilder buf = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ++i) {
            final char c = s.charAt(i);
            if (c == '.' || c == '-') {
                buf.append('_');
            } else {
                buf.append(Character.toUpperCase(c));
            }
        }
        return buf.toString();
    }

    private static boolean isMavenRepoEnvVarOption(String varName, String repoId, String option) {
        return varName.length() == BOOTSTRAP_MAVEN_REPO_PREFIX.length() + repoId.length() + option.length()
                && varName.endsWith(option);
    }

    private static FileProfileActivator createFileProfileActivator() throws BootstrapMavenException {
        var activator = new FileProfileActivator();
        var translator = new DefaultPathTranslator();
        try {
            var pathInterpolator = new ProfileActivationFilePathInterpolator();
            pathInterpolator.setPathTranslator(translator);
            activator.setProfileActivationFilePathInterpolator(pathInterpolator);
        } catch (NoClassDefFoundError e) {
            // ProfileActivationFilePathInterpolator not found; Maven < 3.8.5 (https://github.com/apache/maven/pull/649)
            try {
                activator.getClass().getMethod("setPathTranslator", org.apache.maven.model.path.PathTranslator.class)
                        .invoke(activator, translator);
            } catch (ReflectiveOperationException reflectionExc) {
                throw new BootstrapMavenException(
                        "Failed to set up DefaultPathTranslator for Maven < 3.8.5 DefaultPathTranslator",
                        reflectionExc);
            }
        }
        return activator;
    }
}
