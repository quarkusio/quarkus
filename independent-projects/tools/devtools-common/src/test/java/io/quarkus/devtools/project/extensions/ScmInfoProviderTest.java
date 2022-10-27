package io.quarkus.devtools.project.extensions;

import static io.quarkus.devtools.project.extensions.ScmInfoProvider.getSourceRepo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class ScmInfoProviderTest {

    @SystemStub
    private EnvironmentVariables environment;

    @BeforeEach
    public void setUp() {
        environment.set("GITHUB_REPOSITORY", null);
    }

    @Test
    public void shouldReturnNullWhenNoEnvironmentOrBuildConfigIsPresent() {
        Map scm = getSourceRepo();
        // We shouldn't throw an exception or get upset, we should just quietly return null
        assertNull(scm);
    }

    @Test
    void testGetSourceControlCoordinates() {
        String repoName = "org/place";
        environment.set("GITHUB_REPOSITORY", repoName);
        Map repo = getSourceRepo();
        assertNotNull(repo);
        assertEquals(repo.get("url").toString(), "https://github.com/org/place");
    }
}
