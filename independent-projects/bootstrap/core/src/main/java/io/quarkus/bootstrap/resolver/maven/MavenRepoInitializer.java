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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.cli.CLIManager;
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
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jboss.logging.Logger;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.PropertyUtils;

import static java.util.stream.Collectors.toList;

/**
 * @author Alexey Loubyansky
 */
public class MavenRepoInitializer {

    private static final String DEFAULT_REMOTE_REPO_ID = "central";
    private static final String DEFAULT_REMOTE_REPO_URL = "https://repo.maven.apache.org/maven2";

    private static final String BASEDIR = "basedir";
    private static final String MAVEN_CMD_LINE_ARGS = "MAVEN_CMD_LINE_ARGS";
    private static final String DOT_M2 = ".m2";
    private static final String MAVEN_HOME = "maven.home";
    private static final String M2_HOME = "M2_HOME";
    private static final String SETTINGS_XML = "settings.xml";

    private static final String userHome = PropertyUtils.getUserHome();
    private static final File userMavenConfigurationHome = new File(userHome, DOT_M2);
    private static final String envM2Home = System.getenv(M2_HOME);
    private static final File USER_SETTINGS_FILE;
    private static final File GLOBAL_SETTINGS_FILE;

    private static final CommandLine mvnArgs;

    static {
        final String mvnCmd = System.getenv(MAVEN_CMD_LINE_ARGS);
        String userSettings = null;
        String globalSettings = null;
        if(mvnCmd != null) {
            final CLIManager mvnCli = new CLIManager();
            try {
                mvnArgs = mvnCli.parse(mvnCmd.split("\\s+"));
            } catch (ParseException e) {
                throw new IllegalStateException("Failed to parse Maven command line arguments", e);
            }
            userSettings = mvnArgs.getOptionValue(CLIManager.ALTERNATE_USER_SETTINGS);
            globalSettings = mvnArgs.getOptionValue(CLIManager.ALTERNATE_GLOBAL_SETTINGS);
        } else {
            mvnArgs = null;
        }

        File f = userSettings != null ? resolveUserSettings(userSettings) : new File(userMavenConfigurationHome, SETTINGS_XML);
        USER_SETTINGS_FILE = f != null && f.exists() ? f : null;

        f = globalSettings != null ? resolveUserSettings(globalSettings) : new File(PropertyUtils.getProperty(MAVEN_HOME, envM2Home != null ? envM2Home : ""), "conf/settings.xml");
        GLOBAL_SETTINGS_FILE = f != null && f.exists() ? f : null;
    }

    private static File resolveUserSettings(String settingsArg) {
        File userSettings = new File(settingsArg);
        if(userSettings.exists()) {
            return userSettings;
        }
        String base = System.getenv("MAVEN_PROJECTBASEDIR"); // Root project base dir
        if(base != null) {
            userSettings = new File(base, settingsArg);
            if(userSettings.exists()) {
                return userSettings;
            }
        }
        base = PropertyUtils.getProperty(BASEDIR); // current module project base dir
        if(base != null) {
            userSettings = new File(base, settingsArg);
            if(userSettings.exists()) {
                return userSettings;
            }
        }
        userSettings = new File(userHome, settingsArg);
        if(userSettings.exists()) {
            return userSettings;
        }
        return null;
    }

    private static List<RemoteRepository> remoteRepos;
    private static Settings settings;

    private static final Logger log = Logger.getLogger(MavenRepoInitializer.class);

    public static RepositorySystem getRepositorySystem() {
        return getRepositorySystem(false, null);
    }

    public static RepositorySystem getRepositorySystem(boolean offline, WorkspaceModelResolver wsModelResolver) {

        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        if(!offline) {
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
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

        final org.apache.maven.settings.Proxy proxy = settings.getActiveProxy();
        if (proxy != null) {
            session.setProxySelector(new DefaultProxySelector()
                    .add(toAetherProxy(proxy), proxy.getNonProxyHosts()));
        }

        final List<Mirror> mirrors = settings.getMirrors();
        if(mirrors != null && !mirrors.isEmpty()) {
            final DefaultMirrorSelector ms = new DefaultMirrorSelector();
            for(Mirror m : mirrors) {
                ms.add(m.getId(), m.getUrl(), m.getLayout(), false, m.getMirrorOf(), m.getMirrorOfLayouts());
            }
            session.setMirrorSelector(ms);
        }
        final String localRepoPath = getLocalRepo(settings);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, new LocalRepository(localRepoPath)));

        session.setOffline(settings.isOffline());

        if(mvnArgs != null) {
            if(!session.isOffline() && mvnArgs.hasOption(CLIManager.OFFLINE)) {
                session.setOffline(true);
            }
            if(mvnArgs.hasOption(CLIManager.SUPRESS_SNAPSHOT_UPDATES)) {
                session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
            } else if(mvnArgs.hasOption(CLIManager.UPDATE_SNAPSHOTS)) {
                session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
            }
            if(mvnArgs.hasOption(CLIManager.CHECKSUM_FAILURE_POLICY)) {
                session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
            } else if(mvnArgs.hasOption(CLIManager.CHECKSUM_WARNING_POLICY)) {
                session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
            }
        }

        return session;
    }

    public static List<RemoteRepository> getRemoteRepos() throws AppModelResolverException {
        if (remoteRepos != null) {
            return remoteRepos;
        }
        return remoteRepos = Collections.unmodifiableList(getRemoteRepos(getSettings()));
    }

    public static List<RemoteRepository> getRemoteRepos(Settings settings) throws AppModelResolverException {
        final List<RemoteRepository> remotes = new ArrayList<>();

        final int profilesTotal = settings.getProfiles().size();
        if(profilesTotal > 0) {
            List<org.apache.maven.model.Profile> modelProfiles = new ArrayList<>(profilesTotal);
            for (Profile profile : settings.getProfiles()) {
                modelProfiles.add(SettingsUtils.convertFromSettingsProfile(profile));
            }

            final List<String> activeProfiles = new ArrayList<>(0);
            final List<String> inactiveProfiles = new ArrayList<>(0);
            if(mvnArgs != null) {
                final String[] profileOptionValues = mvnArgs.getOptionValues(CLIManager.ACTIVATE_PROFILES);
                if (profileOptionValues != null && profileOptionValues.length > 0) {
                    for (String profileOptionValue : profileOptionValues) {
                        final StringTokenizer profileTokens = new StringTokenizer(profileOptionValue, ",");
                        while (profileTokens.hasMoreTokens()) {
                            final String profileAction = profileTokens.nextToken().trim();
                            if(profileAction.isEmpty()) {
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
                }
            }

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
            for(org.apache.maven.model.Profile modelProfile : modelProfiles) {
                addProfileRepos(modelProfile, remotes);
            }
        }

        // then it's the ones under active profiles
        final List<String> activeProfiles = settings.getActiveProfiles();
        if (!activeProfiles.isEmpty()) {
            for (String profileName : activeProfiles) {
                final Profile profile = getProfile(profileName, settings);
                if(profile != null) {
                    addProfileRepos(profile, remotes);
                }
            }
        }
        // central must be there
        if (remotes.isEmpty() || !includesDefaultRepo(remotes)) {
            remotes.add(new RemoteRepository.Builder(DEFAULT_REMOTE_REPO_ID, "default", DEFAULT_REMOTE_REPO_URL)
                    .setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                    .setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                    .build());
        }
        final ProxyAwareMirrorSelector proxyAwareMirrorSelector = new ProxyAwareMirrorSelector(
                settings.getMirrors(),
                getProxySelector(settings)
        );
        return remotes.stream()
                .map(proxyAwareMirrorSelector::getMirror)
                // remove duplicates
                .distinct()
                .collect(toList());
    }

    private static ProxySelector getProxySelector(Settings settings) {
        final org.apache.maven.settings.Proxy settingsProxy = settings.getActiveProxy();
        if (settingsProxy == null) {
            return null;
        }
        return new DefaultProxySelector().add(
                new Proxy(
                        settingsProxy.getProtocol(),
                        settingsProxy.getHost(),
                        settingsProxy.getPort(),
                        settingsProxy.getUsername() == null ? null : new AuthenticationBuilder()
                                .addUsername(settingsProxy.getUsername())
                                .addPassword(settingsProxy.getPassword())
                                .build()
                ),
                settingsProxy.getNonProxyHosts()
        );
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
        if(profile == null) {
            unrecognizedProfile(name, true);
        }
        return profile;
    }

    private static void unrecognizedProfile(String name, boolean activate) {
        final StringBuilder buf = new StringBuilder();
        buf.append("The requested Maven profile \"").append(name).append("\" could not be ");
        if(!activate) {
            buf.append("de");
        }
        buf.append("activated because it does not exist.");
        log.warn(buf.toString());
    }

    private static void addProfileRepos(final org.apache.maven.model.Profile profile, final List<RemoteRepository> all) {
        final List<org.apache.maven.model.Repository> repositories = profile.getRepositories();
        for (org.apache.maven.model.Repository repo : repositories) {
            final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repo.getId(), repo.getLayout(), repo.getUrl());
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
            final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repo.getId(), repo.getLayout(), repo.getUrl());
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
        if(settings != null) {
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
            if(!problems.isEmpty()) {
                for(SettingsProblem problem : problems) {
                    switch(problem.getSeverity()) {
                        case ERROR:
                        case FATAL:
                            throw new AppModelResolverException("Settings problem encountered at " + problem.getLocation(), problem.getException());
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
        final String localRepo = settings.getLocalRepository();
        return localRepo == null ? getDefaultLocalRepo() : localRepo;
    }

    private static String getDefaultLocalRepo() {
        return new File(userMavenConfigurationHome, "repository").getAbsolutePath();
    }

    private static boolean includesDefaultRepo(List<RemoteRepository> repositories) {
        for (ArtifactRepository repository : repositories) {
            if(repository.getId().equals(DEFAULT_REMOTE_REPO_ID)) {
                return true;
            }
        }
        return false;
    }

    private static RepositoryPolicy toAetherRepoPolicy(org.apache.maven.model.RepositoryPolicy modelPolicy) {
        return new RepositoryPolicy(modelPolicy.isEnabled(),
                isEmpty(modelPolicy.getUpdatePolicy()) ? RepositoryPolicy.UPDATE_POLICY_DAILY : modelPolicy.getUpdatePolicy(),
                        isEmpty(modelPolicy.getChecksumPolicy()) ? RepositoryPolicy.CHECKSUM_POLICY_WARN : modelPolicy.getChecksumPolicy());
    }

    private static RepositoryPolicy toAetherRepoPolicy(org.apache.maven.settings.RepositoryPolicy settingsPolicy) {
        return new RepositoryPolicy(settingsPolicy.isEnabled(),
                isEmpty(settingsPolicy.getUpdatePolicy()) ? RepositoryPolicy.UPDATE_POLICY_DAILY : settingsPolicy.getUpdatePolicy(),
                        isEmpty(settingsPolicy.getChecksumPolicy()) ? RepositoryPolicy.CHECKSUM_POLICY_WARN : settingsPolicy.getChecksumPolicy());
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
