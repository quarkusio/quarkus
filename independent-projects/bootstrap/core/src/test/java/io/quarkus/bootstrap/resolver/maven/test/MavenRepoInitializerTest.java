package io.quarkus.bootstrap.resolver.maven.test;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenRepoInitializer;

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_DAILY;
import static org.junit.Assert.*;

public class MavenRepoInitializerTest {
    private static Mirror mirrorA;
    private static Proxy localProxy;
    private static Settings baseSettings;

    @BeforeClass
    public static void init() {
        baseSettings = new Settings();

        baseSettings.setInteractiveMode(true);
        baseSettings.setUsePluginRegistry(false);
        baseSettings.setOffline(false);

        final Profile profile = new Profile();
        profile.setId("foo-profile");

        final RepositoryPolicy releasesPolicy = new RepositoryPolicy();
        releasesPolicy.setEnabled(true);
        releasesPolicy.setUpdatePolicy(null);
        releasesPolicy.setChecksumPolicy(CHECKSUM_POLICY_WARN);

        final RepositoryPolicy snapshotsPolicy = new RepositoryPolicy();
        snapshotsPolicy.setEnabled(true);
        snapshotsPolicy.setUpdatePolicy(UPDATE_POLICY_DAILY);
        snapshotsPolicy.setChecksumPolicy(CHECKSUM_POLICY_WARN);

        final RepositoryPolicy noSnapshotsPolicy = new RepositoryPolicy();
        noSnapshotsPolicy.setEnabled(false);

        final Repository customRepository = new Repository();
        customRepository.setId("custom-repo");
        customRepository.setUrl("https://foo.repo/artifact/content/groups/foo/");
        customRepository.setReleases(releasesPolicy);
        customRepository.setSnapshots(snapshotsPolicy);
        profile.addRepository(customRepository);

        final Repository jbossRepository = new Repository();
        jbossRepository.setId("jboss-public-repository");
        jbossRepository.setUrl("https://repository.jboss.org/nexus/content/repositories/releases/");
        jbossRepository.setReleases(releasesPolicy);
        jbossRepository.setSnapshots(noSnapshotsPolicy);
        profile.addRepository(jbossRepository);

        final Repository springRepository = new Repository();
        springRepository.setId("spring-public-repository");
        springRepository.setUrl("http://repo.spring.io/libs-release/");
        springRepository.setReleases(releasesPolicy);
        springRepository.setSnapshots(noSnapshotsPolicy);
        profile.addRepository(springRepository);

        baseSettings.addProfile(profile);
        baseSettings.addActiveProfile("foo-profile");

        localProxy = new Proxy();
        localProxy.setActive(true);
        localProxy.setProtocol("http");
        localProxy.setUsername(null);
        localProxy.setPassword(null);
        localProxy.setPort(8888);
        localProxy.setHost("localhost");
        localProxy.setNonProxyHosts("localhost");
        localProxy.setId("local-proxy-http");

        mirrorA = new Mirror();
        mirrorA.setMirrorOf("central,jboss-public-repository,spring-public-repository");
        mirrorA.setUrl("https://mirror.com/artifact/content/groups/public/");
        mirrorA.setId("mirror-A");
    }

    @Test
    public void getRemoteRepoFromSettingsWithNeitherProxyNorMirror() throws AppModelResolverException {
        final Settings settings = baseSettings.clone();

        List<RemoteRepository> repos = MavenRepoInitializer.getRemoteRepos(settings);
        assertEquals(4, repos.size());

        assertEquals("custom-repo", repos.get(0).getId());
        assertNull(repos.get(0).getProxy());
        assertTrue(repos.get(0).getMirroredRepositories().isEmpty());

        final RemoteRepository centralRepo = repos.get(repos.size() - 1);
        assertEquals("central", centralRepo.getId());
        assertNull(centralRepo.getProxy());
        assertTrue(centralRepo.getMirroredRepositories().isEmpty());
    }

    @Test
    public void getRemoteRepoFromSettingsWithProxyButWithoutMirror() throws AppModelResolverException {
        final Settings settings = baseSettings.clone();
        settings.addProxy(localProxy);

        List<RemoteRepository> repos = MavenRepoInitializer.getRemoteRepos(settings);
        assertEquals(4, repos.size());

        assertEquals("custom-repo", repos.get(0).getId());
        assertNotNull(repos.get(0).getProxy());
        assertNotNull(repos.get(0).getMirroredRepositories());

        final RemoteRepository centralRepo = repos.get(repos.size() - 1);
        assertEquals("central repo must be added as default repository", "central", centralRepo.getId());
        assertNotNull(centralRepo.getProxy());
        assertTrue(centralRepo.getMirroredRepositories().isEmpty());
    }

    @Test
    public void getRemoteRepoFromSettingsWithProxyAndMirror() throws AppModelResolverException {
        final Settings settings = baseSettings.clone();
        settings.addProxy(localProxy);
        settings.addMirror(mirrorA);

        List<RemoteRepository> repos = MavenRepoInitializer.getRemoteRepos(settings);
        assertEquals(2, repos.size());

        assertEquals("custom-repo", repos.get(0).getId());
        assertNotNull(repos.get(0).getProxy());
        assertNotNull(repos.get(0).getMirroredRepositories());

        final RemoteRepository centralRepo = repos.get(repos.size() - 1);
        assertEquals("Central repo must be substitute by mirror", "mirror-A", centralRepo.getId());
        assertNotNull(centralRepo.getProxy());
        assertEquals(3, centralRepo.getMirroredRepositories().size());
        final List<String> mirrored = Arrays.asList("central", "jboss-public-repository", "spring-public-repository");
        for (RemoteRepository repo : centralRepo.getMirroredRepositories()) {
            assertTrue(mirrored.contains(repo.getId()));
        }
    }
}