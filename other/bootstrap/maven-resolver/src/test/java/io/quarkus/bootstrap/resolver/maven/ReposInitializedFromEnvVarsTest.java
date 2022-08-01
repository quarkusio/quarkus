package io.quarkus.bootstrap.resolver.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

public class ReposInitializedFromEnvVarsTest {

    @Test
    void initFromEnvVars() throws Exception {

        final Map<String, String> env = new HashMap<>();
        env.put(BootstrapMavenContext.BOOTSTRAP_MAVEN_REPOS, "example-repo,other-snapshot");
        env.put(BootstrapMavenContext.BOOTSTRAP_MAVEN_REPO_PREFIX + "EXAMPLE_REPO_URL", "https://example-repo.org/maven");
        env.put(BootstrapMavenContext.BOOTSTRAP_MAVEN_REPO_PREFIX + "EXAMPLE_REPO_SNAPSHOT", "false");
        env.put(BootstrapMavenContext.BOOTSTRAP_MAVEN_REPO_PREFIX + "OTHER_SNAPSHOT_URL", "https://other.org/maven/snapshots");
        env.put(BootstrapMavenContext.BOOTSTRAP_MAVEN_REPO_PREFIX + "OTHER_SNAPSHOT_RELEASE", "false");

        final List<RemoteRepository> repos = new ArrayList<>();
        BootstrapMavenContext.readMavenReposFromEnv(repos, env);

        assertThat(repos.size()).isEqualTo(2);

        RemoteRepository r = repos.get(0);
        assertThat(r).isNotNull();
        assertThat(r.getId()).isEqualTo("example-repo");
        assertThat(r.getUrl()).isEqualTo("https://example-repo.org/maven");
        assertThat(r.getPolicy(false).isEnabled()).isTrue();
        assertThat(r.getPolicy(true).isEnabled()).isFalse();

        r = repos.get(1);
        assertThat(r).isNotNull();
        assertThat(r.getId()).isEqualTo("other-snapshot");
        assertThat(r.getUrl()).isEqualTo("https://other.org/maven/snapshots");
        assertThat(r.getPolicy(false).isEnabled()).isFalse();
        assertThat(r.getPolicy(true).isEnabled()).isTrue();
    }
}
