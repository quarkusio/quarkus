/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.PropertyUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.model.Repository;
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
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.building.*;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.Collections.unmodifiableList;

/**
 * @author Alexey Loubyansky
 */
public class MavenRepoInitializer {
    private static final Logger LOG = Logger.getLogger(MavenRepoInitializer.class);

    /**
     * Default remote repository (Maven central)
     */
    private static final RemoteRepository MAVEN_CENTRAL_REPOSITORY =
            new RemoteRepository.Builder(
                    "central",
                    "default",
                    "https://repo.maven.apache.org/maven2")
                    .setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                    .setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                    .build();

    private static final ProxySelector NULL_PROXY_SELECTOR = new ProxySelector() {
        @Override
        public Proxy getProxy(RemoteRepository repository) {
            return null;
        }
    };


    private static final String BASEDIR = "basedir";
    private static final String MAVEN_CMD_LINE_ARGS = "MAVEN_CMD_LINE_ARGS";
    private static final String DOT_M2 = ".m2";
    private static final String SYS_MAVEN_HOME = "maven.home";

    /**
     * Environnment variable name where value indicate where maven is installed.
     *
     * @see "https://issues.apache.org/jira/browse/MNG-5607"
     */
    private static final String ENV_MAVEN_HOME = "MAVEN_HOME";

    private static final String SETTINGS_XML = "settings.xml";

    private static final String USER_HOME = PropertyUtils.getUserHome();
    private static final File USER_MAVEN_CONFIGURATION_HOME = new File(USER_HOME, DOT_M2);

    private static final String ENV_MAVEN_PROJECT_BASEDIR = "MAVEN_PROJECTBASEDIR";

    private static final DefaultProfileSelector DEFAULT_PROFILE_SELECTOR = new DefaultProfileSelector()
            .addProfileActivator(new PropertyProfileActivator())
            .addProfileActivator(new JdkVersionProfileActivator())
            .addProfileActivator(new OperatingSystemProfileActivator())
            .addProfileActivator(new FileProfileActivator().setPathTranslator(new DefaultPathTranslator()));

    private static final String DEFAULT_LOCAL_REPOSITORY_DIRECTORY_NAME = "repository";

    private static class Mapper {
        private static Function<? super org.apache.maven.model.RepositoryPolicy, RepositoryPolicy> MODEL_REPOSITORY_POLICY_TO_AETHER_REPOSITORY_POLICY = modelPolicy ->
                createAetherRepositoryPolicy(
                        modelPolicy.isEnabled(),
                        modelPolicy.getUpdatePolicy(),
                        modelPolicy.getChecksumPolicy()
                );

        private static Function<? super org.apache.maven.settings.RepositoryPolicy, RepositoryPolicy> SETTINGS_REPOSITORY_POLICY_TO_AETHER_REPOSITORY_POLICY = settingsPolicy ->
                createAetherRepositoryPolicy(
                        settingsPolicy.isEnabled(),
                        settingsPolicy.getUpdatePolicy(),
                        settingsPolicy.getChecksumPolicy()
                );

        static Function<Repository, RemoteRepository> MAVEN_REPOSITORY_TO_AETHER_REMOTE_REPOSITORY = mavenRepository -> {
            final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(
                    mavenRepository.getId(),
                    mavenRepository.getLayout(),
                    mavenRepository.getUrl()
            );

            // Set release policy if defined
            ofNullable(mavenRepository.getReleases())
                    .map(MODEL_REPOSITORY_POLICY_TO_AETHER_REPOSITORY_POLICY)
                    .ifPresent(repoBuilder::setReleasePolicy);

            // Set snapshot policy if defined
            ofNullable(mavenRepository.getSnapshots())
                    .map(MODEL_REPOSITORY_POLICY_TO_AETHER_REPOSITORY_POLICY)
                    .ifPresent(repoBuilder::setSnapshotPolicy);

            return repoBuilder.build();
        };

        static Function<org.apache.maven.settings.Repository, RemoteRepository> SETTINGS_REPOSITORY_TO_AETHER_REMOTE_REPOSITORY = settingsRepository -> {
            final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(
                    settingsRepository.getId(),
                    settingsRepository.getLayout(),
                    settingsRepository.getUrl()
            );

            // Set release policy if defined
            ofNullable(settingsRepository.getReleases())
                    .map(SETTINGS_REPOSITORY_POLICY_TO_AETHER_REPOSITORY_POLICY)
                    .ifPresent(repoBuilder::setReleasePolicy);

            // Set snapshot policy if defined
            ofNullable(settingsRepository.getSnapshots())
                    .map(SETTINGS_REPOSITORY_POLICY_TO_AETHER_REPOSITORY_POLICY)
                    .ifPresent(repoBuilder::setSnapshotPolicy);

            return repoBuilder.build();
        };


        private static Function<? super org.apache.maven.settings.Proxy, Authentication> EXTRACTOR_AUTHENTICATION = settingsProxy -> {
            if (settingsProxy.getUsername() == null) {
                return null;
            }
            return new AuthenticationBuilder()
                    .addUsername(settingsProxy.getUsername())
                    .addPassword(settingsProxy.getPassword())
                    .build();

        };

        /**
         * Convert a non-null {@link org.apache.maven.settings.Proxy} to a Aether {@link Proxy}.
         */
        private static Function<org.apache.maven.settings.Proxy, Proxy> SETTINGS_PROXY_TO_AETHER_PROXY = settingsProxy ->
                new Proxy(
                        settingsProxy.getProtocol(),
                        settingsProxy.getHost(),
                        settingsProxy.getPort(),
                        EXTRACTOR_AUTHENTICATION.apply(settingsProxy)
                );
        /**
         * Convert a {@link org.apache.maven.settings.Proxy} to a Aether {@link ProxySelector}. If <code>null</code>, a "Null Proxy Selector" is returned.
         */
        static final Function<org.apache.maven.settings.Proxy, ProxySelector> SETTINGS_PROXY_TO_AETHER_PROXY_SELECTOR = settingsProxy ->
                ofNullable(settingsProxy)
                        .map(SETTINGS_PROXY_TO_AETHER_PROXY)
                        .map(proxy -> (ProxySelector) new DefaultProxySelector().add(proxy, settingsProxy.getNonProxyHosts()))
                        .orElse(NULL_PROXY_SELECTOR);

        private static RepositoryPolicy createAetherRepositoryPolicy(boolean enabled, String updatePolicy, String checksumPolicy) {
            return new RepositoryPolicy(
                    enabled,
                    isEmpty(updatePolicy) ? RepositoryPolicy.UPDATE_POLICY_DAILY : updatePolicy,
                    isEmpty(checksumPolicy) ? RepositoryPolicy.CHECKSUM_POLICY_WARN : checksumPolicy
            );
        }
    }

    public static class Builder {
        private CommandLine mvnCmdLine;
        private boolean offline;
        private String updatePolicy;
        private Settings settings;
        private String checksumPolicy;
        private List<String> profileOptionValues = new ArrayList<>();

        public Builder() {
        }

        Builder offline(Boolean offline) {
            this.offline = offline != null && offline;
            return this;
        }

        /**
         * Configure builder from {@value #MAVEN_CMD_LINE_ARGS} env variable
         */
        Builder setupFrommavenCommandLine() {
            String mvnCmdLineStr = System.getenv(MAVEN_CMD_LINE_ARGS);
            if (mvnCmdLineStr == null || mvnCmdLineStr.isEmpty()) {
                LOG.warnf("Env var '%s' is not defined", MAVEN_CMD_LINE_ARGS);
                return this;
            }
            try {
                CommandLine mavenCmdLine = new CLIManager().parse(mvnCmdLineStr.split("\\s+"));
                return mavenCommandLine(mavenCmdLine);
            } catch (ParseException | AppModelResolverException e) {
                throw new IllegalStateException("Failed to parse Maven command line arguments", e);
            }
        }

        Builder mavenCommandLine(CommandLine mvnCmdLine) throws AppModelResolverException {
            this.mvnCmdLine = mvnCmdLine;
            this.offline = mvnCmdLine.hasOption(CLIManager.OFFLINE);

            final File userSettingsFile = ofNullable(mvnCmdLine.getOptionValue(CLIManager.ALTERNATE_USER_SETTINGS))
                    .map(MavenRepoInitializer::resolveSettings)
                    .filter(File::exists)
                    .orElse(new File(
                            USER_MAVEN_CONFIGURATION_HOME,
                            SETTINGS_XML
                    ));

            final File globalSettingsFile = ofNullable(mvnCmdLine.getOptionValue(CLIManager.ALTERNATE_GLOBAL_SETTINGS))
                    .map(MavenRepoInitializer::resolveSettings)
                    .filter(File::exists)
                    .orElse(new File(
                            PropertyUtils.getProperty(SYS_MAVEN_HOME, ofNullable(System.getenv(ENV_MAVEN_HOME)).orElse("")),
                            "conf/settings.xml"
                    ));


            if (mvnCmdLine.hasOption(CLIManager.SUPRESS_SNAPSHOT_UPDATES)) {
                updatePolicy = RepositoryPolicy.UPDATE_POLICY_NEVER;
            } else if (mvnCmdLine.hasOption(CLIManager.UPDATE_SNAPSHOTS)) {
                updatePolicy = RepositoryPolicy.UPDATE_POLICY_ALWAYS;
            }
            if (mvnCmdLine.hasOption(CLIManager.CHECKSUM_FAILURE_POLICY)) {
                checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL;
            } else if (mvnCmdLine.hasOption(CLIManager.CHECKSUM_WARNING_POLICY)) {
                checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
            }

            ofNullable(mvnCmdLine.getOptionValues(CLIManager.ACTIVATE_PROFILES))
                    .map(Arrays::asList)
                    .ifPresent(profileOptionValues::addAll);

            settings = createSettings(globalSettingsFile, userSettingsFile, offline);
            return this;
        }

        public MavenRepoInitializer build() {
            return new MavenRepoInitializer(this);
        }

        private static Settings createSettings(File globalSettingsFile, File userSettingsFile, boolean isOffline) throws AppModelResolverException {
            try {
                final DefaultSettingsBuildingRequest defaultSettingsBuildingRequest = new DefaultSettingsBuildingRequest()
                        .setSystemProperties(System.getProperties())
                        .setUserSettingsFile(userSettingsFile)
                        .setGlobalSettingsFile(globalSettingsFile);

                final SettingsBuildingResult result = new DefaultSettingsBuilderFactory()
                        .newInstance()
                        .build(defaultSettingsBuildingRequest);

                for (SettingsProblem problem : result.getProblems()) {
                    switch (problem.getSeverity()) {
                        case ERROR:
                        case FATAL:
                            throw new AppModelResolverException("Settings problem encountered at " + problem.getLocation(), problem.getException());
                        default:
                            LOG.warn("Settings problem encountered at " + problem.getLocation(), problem.getException());
                    }
                }
                Settings settings = result.getEffectiveSettings();
                if (isOffline) {
                    settings.setOffline(true);
                }
                return settings;
            } catch (SettingsBuildingException e) {
                throw new AppModelResolverException("Failed to initialize Maven repository settings", e);
            }
        }

        Builder settings(Settings settings) {
            this.settings = settings;
            return this;
        }

        ProxySelector getProxySelector() {
            return ofNullable(settings.getActiveProxy())
                    .map(Mapper.SETTINGS_PROXY_TO_AETHER_PROXY_SELECTOR)
                    .orElse(NULL_PROXY_SELECTOR);
        }

        MirrorSelector getMirrorSelector(ProxySelector proxySelector) {
            return new ProxyAwareMirrorSelector(
                    settings.getMirrors(),
                    proxySelector
            );
        }
    }

    private final String updatePolicy;
    private final String checksumPolicy;
    private final List<String> profileOptionValues;
    private final Settings settings;
    private final MirrorSelector mirrorSelector;
    private final ProxySelector proxySelector;

    private List<RemoteRepository> _remoteRepos;


    private MavenRepoInitializer(Builder builder) {
        this.updatePolicy = builder.updatePolicy;
        this.checksumPolicy = builder.checksumPolicy;
        this.profileOptionValues = unmodifiableList(builder.profileOptionValues);
        this.settings = builder.settings;
        this.proxySelector = builder.getProxySelector();
        this.mirrorSelector = builder.getMirrorSelector(this.proxySelector);
    }


    /**
     * Resolve XML Maven Settings file.
     * Resolution of relative path is made from (in order):
     * <ol>
     * <li>Root project base dir (from env {@value #ENV_MAVEN_PROJECT_BASEDIR}.</li>
     * <li>current module project base dir (from système property {@value #BASEDIR} )</li>
     * <li>user home directory</li>
     * </ol>
     *
     * @param settingsArg XML Maven Settings file absolute or relative path.
     * @return XML Maven Settings file, or <code>null</code>
     */
    private static File resolveSettings(String settingsArg) {
        File userSettings = new File(settingsArg);
        if (userSettings.exists()) {
            return userSettings;
        }
        return Stream
                .of(
                        // Root project base dir
                        System.getenv(ENV_MAVEN_PROJECT_BASEDIR),
                        // current module project base dir
                        PropertyUtils.getProperty(BASEDIR),
                        // user home directory
                        USER_HOME
                )
                // only if perperty if defined
                .filter(Objects::nonNull)
                // try to resolve relative settings file
                .map(base -> new File(base, settingsArg))
                // is settings file exists ?
                .filter(File::exists)
                // stop on first existing file found
                .findFirst()
                // return null if file not found with all bases
                .orElse(null);
    }


    RepositorySystem getRepositorySystem(WorkspaceModelResolver wsModelResolver) {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        if (!settings.isOffline()) {
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        }
        locator.setServices(ModelBuilder.class, new MavenModelBuilder(wsModelResolver));
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                LOG.error("Failed to initialize " + impl.getName() + " as a service implementing " + type.getName(), exception);
            }
        });

        return locator.getService(RepositorySystem.class);
    }


    DefaultRepositorySystemSession newSession(RepositorySystem system) {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        ofNullable(proxySelector)
                .ifPresent(session::setProxySelector);


        session.setMirrorSelector(mirrorSelector);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, getLocalRepo()));

        session.setOffline(settings.isOffline());
        session.setUpdatePolicy(updatePolicy);
        session.setChecksumPolicy(checksumPolicy);
        if (session.getCache() == null) {
            session.setCache(new DefaultRepositoryCache());
        }
        return session;
    }

    List<RemoteRepository> getRemoteRepos() throws AppModelResolverException {
        if (_remoteRepos != null) {
            return _remoteRepos;
        }
        return initRemoteRepos();
    }

    private synchronized List<RemoteRepository> initRemoteRepos() throws AppModelResolverException {
        if (_remoteRepos != null) {
            return _remoteRepos;
        }

        final List<RemoteRepository> remotes = new ArrayList<>();

        final int profilesTotal = settings.getProfiles().size();
        if (profilesTotal > 0) {
            List<org.apache.maven.model.Profile> modelProfiles = new ArrayList<>(profilesTotal);
            for (Profile profile : settings.getProfiles()) {
                modelProfiles.add(SettingsUtils.convertFromSettingsProfile(profile));
            }

            final List<String> activeProfiles = new ArrayList<>(0);
            final List<String> inactiveProfiles = new ArrayList<>(0);
            for (String profileOptionValue : profileOptionValues) {
                final StringTokenizer profileTokens = new StringTokenizer(profileOptionValue, ",");
                while (profileTokens.hasMoreTokens()) {
                    final String profileAction = profileTokens.nextToken().trim();
                    if (profileAction.isEmpty()) {
                        continue;
                    }
                    final char c = profileAction.charAt(0);
                    if (c == '-' || c == '!') {
                        inactiveProfiles.add(profileAction.substring(1));
                    } else if (c == '+') {
                        activeProfiles.add(profileAction.substring(1));
                    } else {
                        activeProfiles.add(profileAction);
                    }
                }
            }

            final DefaultProfileActivationContext context = new DefaultProfileActivationContext()
                    .setActiveProfileIds(activeProfiles)
                    .setInactiveProfileIds(inactiveProfiles)
                    .setSystemProperties(System.getProperties())
                    .setProjectDirectory(
                            ofNullable(PropertyUtils.getProperty(BASEDIR))
                                    .map(File::new)
                                    .orElse(new File(""))
                    );
            modelProfiles = DEFAULT_PROFILE_SELECTOR.getActiveProfiles(modelProfiles, context, new ModelProblemCollector() {
                public void add(ModelProblemCollectorRequest req) {
                    LOG.error("Failed to activate a Maven profile: " + req.getMessage());
                }
            });
            for (org.apache.maven.model.Profile modelProfile : modelProfiles) {
                remotes.addAll(extractRepositoryFromProfile(modelProfile));
            }
        }

        // then it's the ones under active profiles
        settings.getActiveProfiles()
                .stream()
                // Get Profile by name
                .map(this::getProfileByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                // Extract Remote repository defined in profile
                .map(MavenRepoInitializer::extractRepositoryFromSettingsProfile)
                .forEach(remotes::addAll);

        // central must be there
        if (!includesDefaultRepo(remotes)) {
            remotes.add(MAVEN_CENTRAL_REPOSITORY);
        }

        _remoteRepos = unmodifiableList(
                remotes.stream()
                        .map(this::enhanceRemoteRepository)
                        .collect(toList())
        );
        if (LOG.isDebugEnabled()) {
            LOG.debugf(
                    "Collected Remote Repoitories: %s",
                    _remoteRepos.stream()
                            .map(this::remoteRepositoryToStringInfo
                            )
                            .collect(Collectors.joining("\n- ", "\n- ", ""))
            );
        }

        return _remoteRepos;
    }

    /**
     * Replace repo with it's mirror if applicable, and ensure proxy is set-up if needed.
     *
     * @param remoteRepository Repository to enhance
     * @return Enhanced Repository
     */
    private RemoteRepository enhanceRemoteRepository(RemoteRepository remoteRepository) {
        return new RemoteRepository.Builder(
                // Get target repository (itself or mirror if applicable)
                ofNullable(mirrorSelector.getMirror(remoteRepository))
                        .orElse(remoteRepository)
        )
                // Setup proxy if needed
                .setProxy(proxySelector.getProxy(remoteRepository))
                .build();
    }

    private String remoteRepositoryToStringInfo(RemoteRepository remoteRepository) {
        return remoteRepository
                + ofNullable(remoteRepository.getProxy()).map(p -> " via proxy: " + p).orElse("")
                + of(remoteRepository.getMirroredRepositories())
                .filter(mirroredRepositories -> !mirroredRepositories.isEmpty())
                .map(mirroredRepositories -> " ; mirror of: " + mirroredRepositories)
                .orElse("");
    }


    private Optional<Profile> getProfileByName(String name) {
        Profile result = settings.getProfilesAsMap().get(name);
        if (result == null) {
            LOG.warn("The requested Maven profile \"" + name + "\" does not exist.");
        }
        return ofNullable(result);
    }

    private static List<RemoteRepository> extractRepositoryFromProfile(final org.apache.maven.model.Profile profile) {
        return profile.getRepositories().stream()
                .map(Mapper.MAVEN_REPOSITORY_TO_AETHER_REMOTE_REPOSITORY)
                .collect(toList());
    }

    private static List<RemoteRepository> extractRepositoryFromSettingsProfile(final Profile profile) {
        return profile.getRepositories().stream()
                .map(Mapper.SETTINGS_REPOSITORY_TO_AETHER_REMOTE_REPOSITORY)
                .collect(toList());
    }

    /**
     * Get local Maven repository. If not configured in settings, take the default repository (in user Maven
     * configuration home) named {@value #DEFAULT_LOCAL_REPOSITORY_DIRECTORY_NAME}
     *
     * @return
     */
    LocalRepository getLocalRepo() {
        return new LocalRepository(
                ofNullable(settings.getLocalRepository())
                        .orElse(getDefaultLocalRepo())
        );
    }

    private static String getDefaultLocalRepo() {
        return new File(USER_MAVEN_CONFIGURATION_HOME, DEFAULT_LOCAL_REPOSITORY_DIRECTORY_NAME).getAbsolutePath();
    }

    private static boolean includesDefaultRepo(List<RemoteRepository> repositories) {
        return repositories.stream()
                .map(RemoteRepository::getId)
                .anyMatch(id -> MAVEN_CENTRAL_REPOSITORY.getId().equals(id));
    }


    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
