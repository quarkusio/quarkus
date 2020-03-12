package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.util.PropertyUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
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
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jboss.logging.Logger;

/**
 * @author Alexey Loubyansky
 */
public class MavenRepoInitializer {

    public static final String QUARKUS_INTERNAL_MAVEN_CMD_LINE_ARGS = "quarkus-internal.maven-cmd-line-args";

    private static final String DEFAULT_REMOTE_REPO_ID = "central";
    private static final String DEFAULT_REMOTE_REPO_URL = "https://repo.maven.apache.org/maven2";

    private static final String BASEDIR = "basedir";
    private static final String MAVEN_CMD_LINE_ARGS = "MAVEN_CMD_LINE_ARGS";
    private static final String DOT_M2 = ".m2";
    private static final String MAVEN_DOT_HOME = "maven.home";
    private static final String MAVEN_HOME = "MAVEN_HOME";
    private static final String SETTINGS_XML = "settings.xml";

    private static final String userHome = PropertyUtils.getUserHome();
    private static final File userMavenConfigurationHome = new File(userHome, DOT_M2);
    private static final String envM2Home = System.getenv(MAVEN_HOME);
    private static final File USER_SETTINGS_FILE;
    private static final File GLOBAL_SETTINGS_FILE;

    private static final String ALTERNATE_USER_SETTINGS = "s";
    private static final String ALTERNATE_GLOBAL_SETTINGS = "gs";
    private static final String OFFLINE = "o";
    private static final String SUPRESS_SNAPSHOT_UPDATES = "nsu";
    private static final String UPDATE_SNAPSHOTS = "U";
    private static final String CHECKSUM_FAILURE_POLICY = "C";
    private static final String CHECKSUM_WARNING_POLICY = "c";

    private static final BootstrapMavenOptions mvnArgs;

    static {
        String mvnCmd = System.getenv(MAVEN_CMD_LINE_ARGS);
        if (mvnCmd == null) {
            mvnCmd = System.getProperty(QUARKUS_INTERNAL_MAVEN_CMD_LINE_ARGS);
        }
        mvnArgs = BootstrapMavenOptions.newInstance(mvnCmd);
        final String userSettings = mvnArgs.getOptionValue(ALTERNATE_USER_SETTINGS);
        final String globalSettings = mvnArgs.getOptionValue(ALTERNATE_GLOBAL_SETTINGS);

        File f = userSettings != null ? resolveUserSettings(userSettings) : new File(userMavenConfigurationHome, SETTINGS_XML);
        USER_SETTINGS_FILE = f != null && f.exists() ? f : null;

        f = globalSettings != null ? resolveUserSettings(globalSettings)
                : new File(PropertyUtils.getProperty(MAVEN_DOT_HOME, envM2Home != null ? envM2Home : ""), "conf/settings.xml");
        GLOBAL_SETTINGS_FILE = f != null && f.exists() ? f : null;
    }

    private static File resolveUserSettings(String settingsArg) {
        File userSettings = new File(settingsArg);
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
        userSettings = new File(userHome, settingsArg);
        if (userSettings.exists()) {
            return userSettings;
        }
        return null;
    }

    private static Settings settings;

    private static final Logger log = Logger.getLogger(MavenRepoInitializer.class);

    public static RepositorySystem getRepositorySystem() {
        return getRepositorySystem(false, null);
    }

    /**
     * Parsed Maven command line options obtained from the value of MAVEN_CMD_LINE_ARGS environment
     * variable which is usually set by the Maven CLI.
     *
     * @return parsed Maven command line arguments
     */
    public static BootstrapMavenOptions getBootstrapMavenOptions() {
        return mvnArgs;
    }

    public static RepositorySystem getRepositorySystem(boolean offline, WorkspaceModelResolver wsModelResolver) {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        if (!offline) {
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, WagonTransporterFactory.class);
            locator.setServices(WagonProvider.class, new BootstrapWagonProvider());
        }
        locator.setServices(ModelBuilder.class, new MavenModelBuilder(wsModelResolver));
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                log.error("Failed to initialize " + impl.getName() + " as a service implementing " + type.getName(), exception);
            }
        });
        return locator.getService(RepositorySystem.class);
    }

    public static DefaultRepositorySystemSession newSession(RepositorySystem system) throws AppModelResolverException {
        return newSession(system, getSettings());
    }

    public static DefaultRepositorySystemSession newSession(RepositorySystem system, Settings settings) {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        final List<Mirror> mirrors = settings.getMirrors();
        if (mirrors != null && !mirrors.isEmpty()) {
            final DefaultMirrorSelector ms = new DefaultMirrorSelector();
            for (Mirror m : mirrors) {
                ms.add(m.getId(), m.getUrl(), m.getLayout(), false, m.getMirrorOf(), m.getMirrorOfLayouts());
            }
            session.setMirrorSelector(ms);
        }
        final String localRepoPath = getLocalRepo(settings);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, new LocalRepository(localRepoPath)));

        session.setOffline(settings.isOffline());

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

        final Map<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, settings.isInteractiveMode());
        configProps.putAll(System.getProperties());

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

        return session;
    }

    public static List<RemoteRepository> getRemoteRepos(RepositorySystem repoSystem, RepositorySystemSession repoSession)
            throws AppModelResolverException {
        return Collections.unmodifiableList(getRemoteRepos(getSettings(), repoSystem, repoSession));
    }

    public static List<RemoteRepository> getRemoteRepos(Settings settings) throws AppModelResolverException {
        final RepositorySystem system = getRepositorySystem();
        return getRemoteRepos(settings, system, newSession(system, settings));
    }

    public static List<RemoteRepository> getRemoteRepos(Settings settings, RepositorySystem repoSystem,
            RepositorySystemSession repoSession) throws AppModelResolverException {
        List<RemoteRepository> remotes = new ArrayList<>();

        final int profilesTotal = settings.getProfiles().size();
        if (profilesTotal > 0) {
            List<org.apache.maven.model.Profile> modelProfiles = new ArrayList<>(profilesTotal);
            for (Profile profile : settings.getProfiles()) {
                modelProfiles.add(SettingsUtils.convertFromSettingsProfile(profile));
            }

            final List<String> activeProfiles = mvnArgs.getActiveProfileIds();
            final List<String> inactiveProfiles = mvnArgs.getInactiveProfileIds();

            final String basedir = PropertyUtils.getProperty(BASEDIR);
            final DefaultProfileActivationContext context = new DefaultProfileActivationContext()
                    .setActiveProfileIds(activeProfiles)
                    .setInactiveProfileIds(inactiveProfiles)
                    .setSystemProperties(System.getProperties())
                    .setProjectDirectory(basedir == null ? new File("") : new File(basedir));
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
            for (org.apache.maven.model.Profile modelProfile : modelProfiles) {
                addProfileRepos(modelProfile, remotes);
            }
        }

        // then it's the ones under active profiles
        final List<String> activeProfiles = settings.getActiveProfiles();
        if (!activeProfiles.isEmpty()) {
            for (String profileName : activeProfiles) {
                final Profile profile = getProfile(profileName, settings);
                if (profile != null) {
                    addProfileRepos(profile, remotes);
                }
            }
        }
        // central must be there
        if (remotes.isEmpty() || !includesDefaultRepo(remotes)) {
            remotes.add(new RemoteRepository.Builder(DEFAULT_REMOTE_REPO_ID, "default", DEFAULT_REMOTE_REPO_URL)
                    .setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY,
                            RepositoryPolicy.CHECKSUM_POLICY_WARN))
                    .setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY,
                            RepositoryPolicy.CHECKSUM_POLICY_WARN))
                    .build());
        }
        return repoSystem.newResolutionRepositories(repoSession, remotes);
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

    private static void addProfileRepos(final Profile profile, final List<RemoteRepository> all) {
        final List<Repository> repositories = profile.getRepositories();
        for (Repository repo : repositories) {
            final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repo.getId(), repo.getLayout(),
                    repo.getUrl());
            org.apache.maven.settings.RepositoryPolicy policy = repo.getReleases();
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

    public static Settings getSettings() throws AppModelResolverException {
        if (settings != null) {
            return settings;
        }
        final Settings effectiveSettings;
        try {
            final SettingsBuildingResult result = new DefaultSettingsBuilderFactory()
                    .newInstance().build(new DefaultSettingsBuildingRequest()
                            .setSystemProperties(System.getProperties())
                            .setUserSettingsFile(USER_SETTINGS_FILE)
                            .setGlobalSettingsFile(GLOBAL_SETTINGS_FILE));
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

    public static String getLocalRepo(Settings settings) {
        String localRepo = System.getenv("QUARKUS_LOCAL_REPO");
        if (localRepo != null) {
            return localRepo;
        }
        localRepo = PropertyUtils.getProperty("maven.repo.local");
        if (localRepo != null) {
            return localRepo;
        }
        localRepo = settings.getLocalRepository();
        return localRepo == null ? getDefaultLocalRepo() : localRepo;
    }

    private static String getDefaultLocalRepo() {
        return new File(userMavenConfigurationHome, "repository").getAbsolutePath();
    }

    private static boolean includesDefaultRepo(List<RemoteRepository> repositories) {
        for (ArtifactRepository repository : repositories) {
            if (repository.getId().equals(DEFAULT_REMOTE_REPO_ID)) {
                return true;
            }
        }
        return false;
    }

    private static RepositoryPolicy toAetherRepoPolicy(org.apache.maven.model.RepositoryPolicy modelPolicy) {
        return new RepositoryPolicy(modelPolicy.isEnabled(),
                isEmpty(modelPolicy.getUpdatePolicy()) ? RepositoryPolicy.UPDATE_POLICY_DAILY : modelPolicy.getUpdatePolicy(),
                isEmpty(modelPolicy.getChecksumPolicy()) ? RepositoryPolicy.CHECKSUM_POLICY_WARN
                        : modelPolicy.getChecksumPolicy());
    }

    private static RepositoryPolicy toAetherRepoPolicy(org.apache.maven.settings.RepositoryPolicy settingsPolicy) {
        return new RepositoryPolicy(settingsPolicy.isEnabled(),
                isEmpty(settingsPolicy.getUpdatePolicy()) ? RepositoryPolicy.UPDATE_POLICY_DAILY
                        : settingsPolicy.getUpdatePolicy(),
                isEmpty(settingsPolicy.getChecksumPolicy()) ? RepositoryPolicy.CHECKSUM_POLICY_WARN
                        : settingsPolicy.getChecksumPolicy());
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static String getUserAgent() {
        return "Apache-Maven/" + getMavenVersion() + " (Java " + PropertyUtils.getProperty("java.version") + "; "
                + PropertyUtils.getProperty("os.name") + " " + PropertyUtils.getProperty("os.version") + ")";
    }

    private static String getMavenVersion() {
        final Properties props = new Properties();
        try (InputStream is = MavenRepoInitializer.class.getResourceAsStream(
                "/META-INF/maven/org.apache.maven/maven-core/pom.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            log.debug("Failed to read Maven version", e);
        }
        return props.getProperty("version", "unknown-version");
    }
}
