package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import org.apache.maven.settings.*;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_DAILY;
import static org.junit.Assert.*;

public class MavenRepoInitializerTest {
    private static Mirror mirrorA;
    private static Proxy localProxy;
    private static Settings baseSettings;
    private static Repository repository;

    @BeforeClass
    public static void init() {
        baseSettings = new Settings();

        baseSettings.setInteractiveMode(true);
        baseSettings.setUsePluginRegistry(false);
        baseSettings.setOffline(false);


        Profile profile = new Profile();
        profile.setId("foo-profile");

        repository = new Repository();
        repository.setId("foo-repo");
        repository.setUrl("https://foo.repo/artifact/content/groups/foo/");
        RepositoryPolicy releases = new RepositoryPolicy();
        releases.setEnabled(true);
        releases.setUpdatePolicy(null);
        releases.setChecksumPolicy(CHECKSUM_POLICY_WARN);
        repository.setReleases(releases);
        RepositoryPolicy snapshots = new RepositoryPolicy();
        releases.setEnabled(true);
        releases.setUpdatePolicy(UPDATE_POLICY_DAILY);
        releases.setChecksumPolicy(CHECKSUM_POLICY_WARN);
        repository.setSnapshots(snapshots);

        profile.addRepository(repository);

        baseSettings.addProfile(profile);
        baseSettings.addActiveProfile("foo-profile");


        localProxy = new Proxy();
        localProxy.setActive(true);
        localProxy.setProtocol("http");
        localProxy.setUsername(null);
        localProxy.setPassword(null);
        localProxy.setPort(8888);
        localProxy.setHost("localhost");
        localProxy.setNonProxyHosts("localhost|6V25RF2|192.168.*|10.111.*");
        localProxy.setId("local-proxy-http");

        mirrorA = new Mirror();
        mirrorA.setMirrorOf("central,repository.sonatype.org,jboss-public-repository");
        mirrorA.setUrl("https://mirror.com/artifact/content/groups/public/");
        mirrorA.setId("mirror-A");
    }

    @Test
    public void get_remote_repo_from_settings_without_proxy_neither_mmirror() throws AppModelResolverException {
        Settings settings = baseSettings.clone();

        MavenRepoInitializer mavenRepoInitializer = new MavenRepoInitializer.Builder()
                .settings(settings)
                .build();

        List<RemoteRepository> repos = mavenRepoInitializer.getRemoteRepos();
        assertEquals(2, repos.size());

        assertEquals("foo-repo", repos.get(0).getId());
        assertNull(repos.get(0).getProxy());
        assertTrue(repos.get(0).getMirroredRepositories().isEmpty());

        assertEquals("central", repos.get(1).getId());
        assertNull(repos.get(1).getProxy());
        assertTrue(repos.get(1).getMirroredRepositories().isEmpty());
    }

    @Test
    public void get_remote_repo_from_settings_with_proxy_without_mirror() throws AppModelResolverException {
        Settings settings = baseSettings.clone();
        settings.addProxy(localProxy);

        MavenRepoInitializer mavenRepoInitializer = new MavenRepoInitializer.Builder()
                .settings(settings)
                .build();


        List<RemoteRepository> repos = mavenRepoInitializer.getRemoteRepos();
        assertEquals(2, repos.size());

        assertEquals("foo-repo", repos.get(0).getId());
        assertNotNull(repos.get(0).getProxy());
        assertNotNull(repos.get(0).getMirroredRepositories());

        assertEquals("central repo must be added as default repository", "central", repos.get(1).getId());
        assertNotNull(repos.get(1).getProxy());
        assertTrue(repos.get(1).getMirroredRepositories().isEmpty());
    }

    @Test
    public void get_remote_repo_from_settings_with_proxy_and_mirror() throws AppModelResolverException {
        Settings settings = baseSettings.clone();
        settings.addProxy(localProxy);
        settings.addMirror(mirrorA);

        MavenRepoInitializer mavenRepoInitializer = new MavenRepoInitializer.Builder()
                .settings(settings)
                .build();

        List<RemoteRepository> repos = mavenRepoInitializer.getRemoteRepos();
        assertEquals(2, repos.size());

        assertEquals("foo-repo", repos.get(0).getId());
        assertNotNull(repos.get(0).getProxy());
        assertNotNull(repos.get(0).getMirroredRepositories());

        assertEquals("Central repo must be substitute by mirror", "mirror-A", repos.get(1).getId());
        assertNotNull(repos.get(1).getProxy());
        assertEquals(1, repos.get(1).getMirroredRepositories().size());
        assertEquals("central", repos.get(1).getMirroredRepositories().get(0).getId());
    }
}