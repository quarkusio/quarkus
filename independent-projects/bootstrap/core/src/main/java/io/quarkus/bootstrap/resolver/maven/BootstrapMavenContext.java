package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.PropertyUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.cli.transfer.ConsoleMavenTransferListener;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.path.DefaultPathTranslator;
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
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jboss.logging.Logger;

public class BootstrapMavenContext {

    private static final Logger log = Logger.getLogger(BootstrapMavenContext.class);

    private static final String BASEDIR = "basedir";
    private static final String DEFAULT_REMOTE_REPO_ID = "central";
    private static final String DEFAULT_REMOTE_REPO_URL = "https://repo.maven.apache.org/maven2";
    private static final String MAVEN_DOT_HOME = "maven.home";
    private static final String MAVEN_HOME = "MAVEN_HOME";
    private static final String MAVEN_PROJECTBASEDIR = "MAVEN_PROJECTBASEDIR";
    private static final String SETTINGS_XML = "settings.xml";

    private static final String ALTERNATE_USER_SETTINGS = "s";
    private static final String ALTERNATE_GLOBAL_SETTINGS = "gs";
    private static final String ALTERNATE_POM_FILE = "f";
    private static final String OFFLINE = "o";
    private static final String SUPRESS_SNAPSHOT_UPDATES = "nsu";
    private static final String UPDATE_SNAPSHOTS = "U";
    private static final String CHECKSUM_FAILURE_POLICY = "C";
    private static final String CHECKSUM_WARNING_POLICY = "c";

    private static final String userHome = PropertyUtils.getUserHome();
    private static final File userMavenConfigurationHome = new File(userHome, ".m2");

    private boolean artifactTransferLogging;
    private BootstrapMavenOptions cliOptions;
    private File userSettings;
    private File globalSettings;
    private Boolean offline;
    private LocalWorkspace workspace;
    private LocalProject currentProject;
    private Settings settings;
    private List<org.apache.maven.model.Profile> activeSettingsProfiles;
    private RepositorySystem repoSystem;
    private RepositorySystemSession repoSession;
    private List<RemoteRepository> remoteRepos;
    private String localRepo;
    private Path currentPom;
    private Boolean currentProjectExists;
    private DefaultServiceLocator serviceLocator;
    private String alternatePomName;

    public static BootstrapMavenContextConfig<?> config() {
        return new BootstrapMavenContextConfig<>();
    }

    public BootstrapMavenContext() throws AppModelResolverException {
        this(new BootstrapMavenContextConfig<>());
    }

    public BootstrapMavenContext(BootstrapMavenContextConfig<?> config)
            throws AppModelResolverException {
        /*
         * WARNING: this constructor calls instance method as part of the initialization.
         * This means the values that are available in the config should be set before
         * the instance method invocations.
         */
        this.alternatePomName = config.alternativePomName;
        this.artifactTransferLogging = config.artifactTransferLogging;
        this.localRepo = config.localRepo;
        this.offline = config.offline;
        this.repoSystem = config.repoSystem;
        this.repoSession = config.repoSession;
        this.remoteRepos = config.remoteRepos;
        if (config.workspace != null) {
            this.workspace = config.workspace;
        } else if (config.workspaceDiscovery) {
            currentProject = resolveCurrentProject();
            this.workspace = currentProject == null ? null : currentProject.getWorkspace();
        }
        userSettings = config.userSettings == null
                ? resolveSettingsFile(getCliOptions().getOptionValue(ALTERNATE_USER_SETTINGS),
                        () -> new File(userMavenConfigurationHome, SETTINGS_XML))
                : config.userSettings;
        globalSettings = resolveSettingsFile(getCliOptions().getOptionValue(ALTERNATE_GLOBAL_SETTINGS), () -> {
            final String envM2Home = System.getenv(MAVEN_HOME);
            return new File(PropertyUtils.getProperty(MAVEN_DOT_HOME, envM2Home != null ? envM2Home : ""), "conf/settings.xml");
        });
    }

    public AppArtifact getCurrentProjectArtifact(String extension) throws AppModelResolverException {
        if (currentProject != null) {
            return currentProject.getAppArtifact(extension);
        }
        final Model model = loadCurrentProjectModel();
        if (model == null) {
            return null;
        }
        return new AppArtifact(ModelUtils.getGroupId(model), model.getArtifactId(), "", extension,
                ModelUtils.getVersion(model));
    }

    public LocalWorkspace getWorkspace() {
        return workspace;
    }

    public BootstrapMavenOptions getCliOptions() {
        return cliOptions == null ? cliOptions = BootstrapMavenOptions.newInstance() : cliOptions;
    }

    public File getUserSettings() {
        return userSettings;
    }

    public File getGlobalSettings() {
        return globalSettings;
    }

    public boolean isOffline() throws AppModelResolverException {
        return offline == null ? offline = getCliOptions().hasOption(OFFLINE) || getEffectiveSettings().isOffline() : offline;
    }

    public RepositorySystem getRepositorySystem() throws AppModelResolverException {
        return repoSystem == null ? repoSystem = newRepositorySystem() : repoSystem;
    }

    public RepositorySystemSession getRepositorySystemSession() throws AppModelResolverException {
        return repoSession == null ? repoSession = newRepositorySystemSession() : repoSession;
    }

    public List<RemoteRepository> getRemoteRepositories() throws AppModelResolverException {
        return remoteRepos == null ? remoteRepos = resolveRemoteRepos() : remoteRepos;
    }

    public Settings getEffectiveSettings() throws AppModelResolverException {
        if (settings != null) {
            return settings;
        }

        final Settings effectiveSettings;
        try {
            final SettingsBuildingResult result = new DefaultSettingsBuilderFactory()
                    .newInstance().build(new DefaultSettingsBuildingRequest()
                            .setSystemProperties(System.getProperties())
                            .setUserSettingsFile(getUserSettings())
                            .setGlobalSettingsFile(getGlobalSettings()));
            final List<SettingsProblem> problems = result.getProblems();
            if (!problems.isEmpty()) {
                for (SettingsProblem problem : problems) {
                    switch (problem.getSeverity()) {
                        case ERROR:
                        case FATAL:
                            throw new AppModelResolverException("Settings problem encountered at " + problem.getLocation(),
                                    problem.getException());
                        default:
                            log.warn("Settings problem encountered at " + problem.getLocation(), problem.getException());
                    }
                }
            }
            effectiveSettings = result.getEffectiveSettings();
        } catch (SettingsBuildingException e) {
            throw new AppModelResolverException("Failed to initialize Maven repository settings", e);
        }
        return settings = effectiveSettings;
    }

    public String getLocalRepo() throws AppModelResolverException {
        return localRepo == null ? localRepo = resolveLocalRepo(getEffectiveSettings()) : localRepo;
    }

    private LocalProject resolveCurrentProject() throws AppModelResolverException {
        try {
            return LocalProject.loadWorkspace(this);
        } catch (BootstrapException e) {
            throw new AppModelResolverException("Failed to load current project at " + getCurrentProjectPomOrNull());
        }
    }

    public static String resolveLocalRepo(Settings settings) {
        String localRepo = System.getenv("QUARKUS_LOCAL_REPO");
        if (localRepo != null) {
            return localRepo;
        }
        localRepo = PropertyUtils.getProperty("maven.repo.local");
        if (localRepo != null) {
            return localRepo;
        }
        localRepo = settings.getLocalRepository();
        return localRepo == null ? new File(userMavenConfigurationHome, "repository").getAbsolutePath() : localRepo;
    }

    private static File resolveSettingsFile(String settingsArg, Supplier<File> supplier) {
        File userSettings;
        if (settingsArg != null) {
            userSettings = new File(settingsArg);
            if (userSettings.exists()) {
                return userSettings;
            }
            String base = System.getenv("MAVEN_PROJECTBASEDIR"); // Root project base dir
            if (base != null) {
                userSettings = new File(base, settingsArg);
                if (userSettings.exists()) {
                    return userSettings;
                }
            }
            base = PropertyUtils.getProperty(BASEDIR); // current module project base dir
            if (base != null) {
                userSettings = new File(base, settingsArg);
                if (userSettings.exists()) {
                    return userSettings;
                }
            }
            userSettings = new File(PropertyUtils.getUserHome(), settingsArg);
            if (userSettings.exists()) {
                return userSettings;
            }
        }
        userSettings = supplier.get();
        return userSettings.exists() ? userSettings : null;
    }

    private DefaultRepositorySystemSession newRepositorySystemSession() throws AppModelResolverException {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        final Settings settings = getEffectiveSettings();

        final List<Mirror> mirrors = settings.getMirrors();
        if (mirrors != null && !mirrors.isEmpty()) {
            final DefaultMirrorSelector ms = new DefaultMirrorSelector();
            for (Mirror m : mirrors) {
                ms.add(m.getId(), m.getUrl(), m.getLayout(), false, m.getMirrorOf(), m.getMirrorOfLayouts());
            }
            session.setMirrorSelector(ms);
        }
        final String localRepoPath = getLocalRepo();
        session.setLocalRepositoryManager(
                getRepositorySystem().newLocalRepositoryManager(session, new LocalRepository(localRepoPath)));

        session.setOffline(isOffline());

        final BootstrapMavenOptions mvnArgs = getCliOptions();
        if (!mvnArgs.isEmpty()) {
            if (!session.isOffline() && mvnArgs.hasOption(OFFLINE)) {
                session.setOffline(true);
            }
            if (mvnArgs.hasOption(SUPRESS_SNAPSHOT_UPDATES)) {
                session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
            } else if (mvnArgs.hasOption(UPDATE_SNAPSHOTS)) {
                session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
            }
            if (mvnArgs.hasOption(CHECKSUM_FAILURE_POLICY)) {
                session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
            } else if (mvnArgs.hasOption(CHECKSUM_WARNING_POLICY)) {
                session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
            }
        }

        final DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies(settings.getProxies());
        decrypt.setServers(settings.getServers());
        final SettingsDecryptionResult decrypted = new SettingsDecrypterImpl().decrypt(decrypt);
        if (!decrypted.getProblems().isEmpty() && log.isDebugEnabled()) {
            // this is how maven handles these
            for (SettingsProblem p : decrypted.getProblems()) {
                log.debug(p.getMessage(), p.getException());
            }
        }

        final DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (org.apache.maven.settings.Proxy p : decrypted.getProxies()) {
            proxySelector.add(toAetherProxy(p), p.getNonProxyHosts());
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
            }
            configProps.put("aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions());
            configProps.put("aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions());
        }
        session.setAuthenticationSelector(authSelector);

        session.setConfigProperties(configProps);

        if (session.getCache() == null) {
            session.setCache(new DefaultRepositoryCache());
        }

        if (workspace != null) {
            session.setWorkspaceReader(workspace);
        }

        if (session.getTransferListener() == null && artifactTransferLogging) {
            session.setTransferListener(new ConsoleMavenTransferListener(System.out, true));
        }

        return session;
    }

    private List<RemoteRepository> resolveRemoteRepos() throws AppModelResolverException {
        final List<RemoteRepository> rawRepos = new ArrayList<>();

        getActiveSettingsProfiles().forEach(p -> addProfileRepos(p, rawRepos));

        // central must be there
        if (rawRepos.isEmpty() || !includesDefaultRepo(rawRepos)) {
            rawRepos.add(newDefaultRepository());
        }
        final List<RemoteRepository> repos = getRepositorySystem().newResolutionRepositories(getRepositorySystemSession(),
                rawRepos);

        return workspace == null ? repos : resolveCurrentProjectRepos(repos);
    }

    public static RemoteRepository newDefaultRepository() {
        return new RemoteRepository.Builder(DEFAULT_REMOTE_REPO_ID, "default", DEFAULT_REMOTE_REPO_URL)
                .setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY,
                        RepositoryPolicy.CHECKSUM_POLICY_WARN))
                .setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY,
                        RepositoryPolicy.CHECKSUM_POLICY_WARN))
                .build();
    }

    private Model loadCurrentProjectModel() throws AppModelResolverException {
        final Path pom = getCurrentProjectPomOrNull();
        if (pom == null) {
            return null;
        }
        try {
            return ModelUtils.readModel(pom);
        } catch (IOException e) {
            throw new AppModelResolverException("Failed to parse " + pom, e);
        }
    }

    private List<RemoteRepository> resolveCurrentProjectRepos(List<RemoteRepository> repos)
            throws AppModelResolverException {
        final Model model = loadCurrentProjectModel();
        if (model == null) {
            return repos;
        }
        final Artifact projectArtifact = new DefaultArtifact(ModelUtils.getGroupId(model), model.getArtifactId(), "", "pom",
                ModelUtils.getVersion(model));
        try {
            return getRepositorySystem().readArtifactDescriptor(getRepositorySystemSession(), new ArtifactDescriptorRequest()
                    .setArtifact(projectArtifact)
                    .setRepositories(repos))
                    .getRepositories();
        } catch (ArtifactDescriptorException e) {
            throw new AppModelResolverException("Failed to read artifact descriptor for " + projectArtifact, e);
        }
    }

    public List<org.apache.maven.model.Profile> getActiveSettingsProfiles()
            throws AppModelResolverException {
        if (activeSettingsProfiles != null) {
            return activeSettingsProfiles;
        }

        final Settings settings = getEffectiveSettings();
        final int profilesTotal = settings.getProfiles().size();
        if (profilesTotal == 0) {
            return Collections.emptyList();
        }
        List<org.apache.maven.model.Profile> modelProfiles = new ArrayList<>(profilesTotal);
        for (Profile profile : settings.getProfiles()) {
            modelProfiles.add(SettingsUtils.convertFromSettingsProfile(profile));
        }

        final BootstrapMavenOptions mvnArgs = getCliOptions();
        List<String> activeProfiles = mvnArgs.getActiveProfileIds();
        final List<String> inactiveProfiles = mvnArgs.getInactiveProfileIds();

        final Path currentPom = getCurrentProjectPomOrNull();
        final DefaultProfileActivationContext context = new DefaultProfileActivationContext()
                .setActiveProfileIds(activeProfiles)
                .setInactiveProfileIds(inactiveProfiles)
                .setSystemProperties(System.getProperties())
                .setProjectDirectory(
                        currentPom == null ? getCurrentProjectBaseDir().toFile() : currentPom.getParent().toFile());
        final DefaultProfileSelector profileSelector = new DefaultProfileSelector()
                .addProfileActivator(new PropertyProfileActivator())
                .addProfileActivator(new JdkVersionProfileActivator())
                .addProfileActivator(new OperatingSystemProfileActivator())
                .addProfileActivator(new FileProfileActivator().setPathTranslator(new DefaultPathTranslator()));
        modelProfiles = profileSelector.getActiveProfiles(modelProfiles, context, new ModelProblemCollector() {
            public void add(ModelProblemCollectorRequest req) {
                log.error("Failed to activate a Maven profile: " + req.getMessage());
            }
        });

        activeProfiles = settings.getActiveProfiles();
        if (!activeProfiles.isEmpty()) {
            for (String profileName : activeProfiles) {
                final Profile profile = getProfile(profileName, settings);
                if (profile != null) {
                    modelProfiles.add(SettingsUtils.convertFromSettingsProfile(profile));
                }
            }
        }
        return activeSettingsProfiles = modelProfiles;
    }

    private static Profile getProfile(String name, Settings settings) throws AppModelResolverException {
        final Profile profile = settings.getProfilesAsMap().get(name);
        if (profile == null) {
            unrecognizedProfile(name, true);
        }
        return profile;
    }

    private static void unrecognizedProfile(String name, boolean activate) {
        final StringBuilder buf = new StringBuilder();
        buf.append("The requested Maven profile \"").append(name).append("\" could not be ");
        if (!activate) {
            buf.append("de");
        }
        buf.append("activated because it does not exist.");
        log.warn(buf.toString());
    }

    private static boolean includesDefaultRepo(List<RemoteRepository> repositories) {
        for (ArtifactRepository repository : repositories) {
            if (repository.getId().equals(DEFAULT_REMOTE_REPO_ID)) {
                return true;
            }
        }
        return false;
    }

    private static void addProfileRepos(final org.apache.maven.model.Profile profile, final List<RemoteRepository> all) {
        final List<org.apache.maven.model.Repository> repositories = profile.getRepositories();
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
                StringUtils.isEmpty(modelPolicy.getUpdatePolicy()) ? RepositoryPolicy.UPDATE_POLICY_DAILY
                        : modelPolicy.getUpdatePolicy(),
                StringUtils.isEmpty(modelPolicy.getChecksumPolicy()) ? RepositoryPolicy.CHECKSUM_POLICY_WARN
                        : modelPolicy.getChecksumPolicy());
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

    private RepositorySystem newRepositorySystem() throws AppModelResolverException {
        final DefaultServiceLocator locator = getServiceLocator();
        if (!isOffline()) {
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, WagonTransporterFactory.class);
            locator.setServices(WagonProvider.class, new BootstrapWagonProvider());
        }
        locator.setServices(ModelBuilder.class, new MavenModelBuilder(workspace, getCliOptions(),
                workspace == null ? Collections.emptyList() : getActiveSettingsProfiles()));
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                log.error("Failed to initialize " + impl.getName() + " as a service implementing " + type.getName(), exception);
            }
        });
        return locator.getService(RepositorySystem.class);
    }

    public RemoteRepositoryManager getRemoteRepositoryManager() {
        final DefaultRemoteRepositoryManager remoteRepoManager = new DefaultRemoteRepositoryManager();
        remoteRepoManager.initService(getServiceLocator());
        return remoteRepoManager;
    }

    private DefaultServiceLocator getServiceLocator() {
        return serviceLocator == null ? serviceLocator = MavenRepositorySystemUtils.newServiceLocator() : serviceLocator;
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
        String pomName = alternatePomName == null ? getCliOptions().getOptionValue(ALTERNATE_POM_FILE) : alternatePomName;
        if (pomName == null) {
            pomName = "pom.xml";
        }
        Path pom = Paths.get(pomName);
        if (!pom.isAbsolute()) {
            pom = getCurrentProjectBaseDir().resolve(pom);
        }
        if (Files.isDirectory(pom)) {
            pom = pom.resolve("pom.xml");
        }
        return currentPom = (currentProjectExists = Files.exists(pom)) ? pom : null;
    }

    public Path getCurrentProjectBaseDir() {
        final String basedirProp = PropertyUtils.getProperty(BASEDIR);
        return basedirProp == null ? Paths.get("").normalize().toAbsolutePath() : Paths.get(basedirProp);
    }

    public Path getRootProjectBaseDir() {
        final String rootBaseDir = System.getenv(MAVEN_PROJECTBASEDIR);
        if (rootBaseDir == null) {
            return null;
        }
        // if the alternate POM was set (not on the CLI) and its base dir does not match the base dir
        // set by the Maven process then the root project set by the Maven process is probably not relevant too
        if (alternatePomName != null) {
            final Path currentPom = getCurrentProjectPomOrNull();
            if (currentPom == null || !getCurrentProjectBaseDir().equals(currentPom.getParent())) {
                return null;
            }
        }
        return Paths.get(rootBaseDir);
    }
}
