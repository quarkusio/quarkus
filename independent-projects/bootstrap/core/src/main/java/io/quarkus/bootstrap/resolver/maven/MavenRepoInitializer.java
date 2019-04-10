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
import java.util.Map;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
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

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenRepoInitializer {

    private static final String DEFAULT_REMOTE_REPO_ID = "central";
    private static final String DEFAULT_REMOTE_REPO_URL = "https://repo.maven.apache.org/maven2";

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

    static {
        final String mvnCmd = System.getenv(MAVEN_CMD_LINE_ARGS);
        String userSettings = null;
        String globalSettings = null;
        if(mvnCmd != null) {
            userSettings = getMvnCmdArg(mvnCmd, " -s ");
            if(userSettings == null) {
                userSettings = getMvnCmdArg(mvnCmd, "--settings ");
            }
            globalSettings = getMvnCmdArg(mvnCmd, " -gs ");
            if(globalSettings == null) {
                globalSettings = getMvnCmdArg(mvnCmd, "--global-settings ");
            }
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
        base = PropertyUtils.getProperty("basedir"); // current module project base dir
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
                System.err.println("Service creation failed");
                exception.printStackTrace();
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
            Authentication auth = null;
            if(proxy.getUsername() != null) {
                auth = new AuthenticationBuilder()
                        .addUsername(proxy.getUsername())
                        .addPassword(proxy.getPassword())
                        .build();
            }
            session.setProxySelector(new DefaultProxySelector()
                    .add(new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth), proxy.getNonProxyHosts()));
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

        return session;
    }

    public static List<RemoteRepository> getRemoteRepos() throws AppModelResolverException {
        if(remoteRepos != null) {
            return remoteRepos;
        }
        return remoteRepos = Collections.unmodifiableList(getRemoteRepos(getSettings()));
    }

    public static List<RemoteRepository> getRemoteRepos(Settings settings) throws AppModelResolverException {
        final List<RemoteRepository> remotes = new ArrayList<>();
        for (Profile profile : settings.getProfiles()) {
            if (profile.getActivation() != null && profile.getActivation().isActiveByDefault()) {
                addProfileRepos(profile, remotes);
            }
        }
        final List<String> activeProfiles = settings.getActiveProfiles();
        if (!activeProfiles.isEmpty()) {
            final Map<String, Profile> profilesMap = settings.getProfilesAsMap();
            for (String profileName : activeProfiles) {
                addProfileRepos(profilesMap.get(profileName), remotes);
            }
        }
        if (remotes.isEmpty() || !includesDefaultRepo(remotes)) {
            remotes.add(new RemoteRepository.Builder(DEFAULT_REMOTE_REPO_ID, "default", DEFAULT_REMOTE_REPO_URL)
                    .setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                    .setSnapshotPolicy(new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                    .build());
        }
        return remotes;
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

    private static String getMvnCmdArg(final String mvnCmd, String argName) {
        final int argStart = mvnCmd.indexOf(argName);
        if(argStart > 0) {
            final StringBuilder buf = new StringBuilder();
            int i = argStart + argName.length();
            while(i < mvnCmd.length()) {
                final char c = mvnCmd.charAt(i++);
                if(Character.isWhitespace(c)) {
                    if(buf.length() > 0) {
                        return buf.toString();
                    }
                } else {
                    buf.append(c);
                }
            }
            return buf.length() == 0 ? null : buf.toString();
        }
        return null;
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

    private static RepositoryPolicy toAetherRepoPolicy(org.apache.maven.settings.RepositoryPolicy settingsPolicy) {
        return new RepositoryPolicy(settingsPolicy.isEnabled(),
                isEmpty(settingsPolicy.getUpdatePolicy()) ? RepositoryPolicy.UPDATE_POLICY_DAILY : settingsPolicy.getUpdatePolicy(),
                        isEmpty(settingsPolicy.getChecksumPolicy()) ? RepositoryPolicy.CHECKSUM_POLICY_WARN : settingsPolicy.getChecksumPolicy());
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
