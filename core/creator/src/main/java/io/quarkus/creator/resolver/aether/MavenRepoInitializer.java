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
package io.quarkus.creator.resolver.aether;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
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
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jboss.logging.Logger;

import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenRepoInitializer {

    private static final String DOT_M2 = ".m2";
    private static final String MAVEN_HOME = "maven.home";
    private static final String M2_HOME = "M2_HOME";
    private static final String SETTINGS_XML = "settings.xml";

    public static final String userHome = PropertyUtils.getUserHome();
    public static final File userMavenConfigurationHome = new File(userHome, DOT_M2);
    public static final String envM2Home = System.getenv(M2_HOME);
    public static final File DEFAULT_USER_SETTINGS_FILE = new File(userMavenConfigurationHome, SETTINGS_XML);
    public static final File DEFAULT_GLOBAL_SETTINGS_FILE = new File(
            PropertyUtils.getProperty(MAVEN_HOME, envM2Home != null ? envM2Home : ""), "conf/settings.xml");

    private static RepositorySystem repoSystem;
    private static List<RemoteRepository> remoteRepos;
    private static Settings settings;

    private static final Logger log = Logger.getLogger(MavenRepoInitializer.class);

    public static RepositorySystem getRepositorySystem() {
        if (repoSystem != null) {
            return repoSystem;
        }

        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                System.err.println("Service creation failed");
                exception.printStackTrace();
            }
        });

        return repoSystem = locator.getService(RepositorySystem.class);
    }

    public static DefaultRepositorySystemSession newSession(RepositorySystem system) throws AppCreatorException {
        return newSession(system, getSettings());
    }

    public static DefaultRepositorySystemSession newSession(RepositorySystem system, Settings settings) {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        final org.apache.maven.settings.Proxy proxy = settings.getActiveProxy();
        if (proxy != null) {
            Authentication auth = null;
            if (proxy.getUsername() != null) {
                auth = new AuthenticationBuilder()
                        .addUsername(proxy.getUsername())
                        .addPassword(proxy.getPassword())
                        .build();
            }
            final Proxy aetherProxy = new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth);
            DefaultProxySelector proxySelector = new DefaultProxySelector();
            proxySelector.add(aetherProxy, proxy.getNonProxyHosts());
            session.setProxySelector(proxySelector);
        }

        final String localRepoPath = getLocalRepo(settings);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, new LocalRepository(localRepoPath)));

        session.setOffline(settings.isOffline());

        // uncomment to generate dirty trees
        //session.setDependencyGraphTransformer( null );

        return session;
    }

    public static List<RemoteRepository> getRemoteRepos() throws AppCreatorException {
        if (remoteRepos != null) {
            return remoteRepos;
        }
        remoteRepos = Collections.unmodifiableList(getRemoteRepos(getSettings()));
        return remoteRepos;
    }

    public static List<RemoteRepository> getRemoteRepos(Settings settings) throws AppCreatorException {

        final Map<String, Profile> profilesMap = settings.getProfilesAsMap();
        final List<RemoteRepository> remotes = new ArrayList<>();

        for (String profileName : settings.getActiveProfiles()) {
            final Profile profile = profilesMap.get(profileName);
            final List<Repository> repositories = profile.getRepositories();
            for (Repository repo : repositories) {
                final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repo.getId(), repo.getLayout(),
                        repo.getUrl());
                org.apache.maven.settings.RepositoryPolicy policy = repo.getReleases();
                if (policy != null) {
                    repoBuilder.setReleasePolicy(
                            new RepositoryPolicy(policy.isEnabled(), policy.getUpdatePolicy(), policy.getChecksumPolicy()));
                }
                policy = repo.getSnapshots();
                if (policy != null) {
                    repoBuilder.setSnapshotPolicy(
                            new RepositoryPolicy(policy.isEnabled(), policy.getUpdatePolicy(), policy.getChecksumPolicy()));
                }
                remotes.add(repoBuilder.build());
            }
        }
        return remotes;
    }

    public static Settings getSettings() throws AppCreatorException {
        if (settings != null) {
            return settings;
        }
        final SettingsBuildingRequest settingsBuildingRequest = new DefaultSettingsBuildingRequest();
        settingsBuildingRequest.setSystemProperties(System.getProperties());
        settingsBuildingRequest.setUserSettingsFile(DEFAULT_USER_SETTINGS_FILE);
        settingsBuildingRequest.setGlobalSettingsFile(DEFAULT_GLOBAL_SETTINGS_FILE);

        final Settings effectiveSettings;
        try {
            final SettingsBuildingResult result = new DefaultSettingsBuilderFactory().newInstance()
                    .build(settingsBuildingRequest);
            final List<SettingsProblem> problems = result.getProblems();
            if (!problems.isEmpty()) {
                for (SettingsProblem problem : problems) {
                    switch (problem.getSeverity()) {
                        case ERROR:
                        case FATAL:
                            throw new AppCreatorException("Settings problem encountered at " + problem.getLocation(),
                                    problem.getException());
                        default:
                            log.warn("Settings problem encountered at " + problem.getLocation(), problem.getException());
                    }
                }
            }
            effectiveSettings = result.getEffectiveSettings();
        } catch (SettingsBuildingException e) {
            throw new AppCreatorException("Failed to initialize Maven repository settings", e);
        }

        settings = effectiveSettings;
        return effectiveSettings;
    }

    public static String getLocalRepo(Settings settings) {
        final String localRepo = settings.getLocalRepository();
        return localRepo == null ? getDefaultLocalRepo() : localRepo;
    }

    private static String getDefaultLocalRepo() {
        return new File(userMavenConfigurationHome, "repository").getAbsolutePath();
    }
}
