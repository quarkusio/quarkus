package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContextConfig;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.AfterEach;

public class BootstrapMavenContextTestBase {

    private Map<String, String> cleanupProps = new HashMap<>();

    public BootstrapMavenContextTestBase() {
        super();
    }

    @AfterEach
    public void afterEach() {
        if (!cleanupProps.isEmpty()) {
            for (Map.Entry<String, String> entry : cleanupProps.entrySet()) {
                if (entry.getValue() == null) {
                    System.clearProperty(entry.getKey());
                } else {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected RemoteRepository newRepo(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }

    protected BootstrapMavenContext bootstrapMavenContextForProject(String projectOnCp) throws Exception {

        final BootstrapMavenContextConfig<?> config = BootstrapMavenContext.config();
        initBootstrapMavenContextConfig(config);

        final Path projectLocation = getProjectLocation(projectOnCp);
        config.setCurrentProject(projectLocation.toString());

        final Path projectSettingsXml = projectLocation.resolve("settings.xml");
        if (Files.exists(projectSettingsXml)) {
            config.setUserSettings(projectSettingsXml.toFile());
        }
        return new BootstrapMavenContext(config);
    }

    protected void initBootstrapMavenContextConfig(BootstrapMavenContextConfig<?> config) throws Exception {
    }

    protected BootstrapMavenContext bootstrapMavenContextWithSettings(String configDirOnCp) throws Exception {

        final BootstrapMavenContextConfig<?> config = initBootstrapMavenContextConfig();

        final Path projectLocation = getProjectLocation(configDirOnCp);
        final Path projectSettingsXml = projectLocation.resolve("settings.xml");
        if (Files.exists(projectSettingsXml)) {
            config.setUserSettings(projectSettingsXml.toFile());
        }
        return new BootstrapMavenContext(config);
    }

    protected BootstrapMavenContextConfig<?> initBootstrapMavenContextConfig() throws Exception {
        return BootstrapMavenContext.config().setWorkspaceDiscovery(false);
    }

    protected Path getProjectLocation(String projectOnCp) throws URISyntaxException {
        final URL basedirUrl = Thread.currentThread().getContextClassLoader().getResource(projectOnCp);
        assertNotNull(basedirUrl);
        return Paths.get(basedirUrl.toURI());
    }

    protected void setSystemProp(String name, String value) {
        cleanupProps.put(name, System.getProperty(name));
        System.setProperty(name, value);
    }

}
