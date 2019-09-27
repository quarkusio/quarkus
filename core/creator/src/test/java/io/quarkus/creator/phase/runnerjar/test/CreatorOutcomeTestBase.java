package io.quarkus.creator.phase.runnerjar.test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.config.reader.PropertiesConfigReader;
import io.quarkus.creator.config.reader.PropertiesHandler;

public abstract class CreatorOutcomeTestBase extends ResolverSetupCleanup {

    protected TsArtifact appJar;
    protected Path propsFile;

    @Test
    public void test() throws Exception {
        appJar = modelApp();
        appJar.install(repo);

        final Properties props = getProperties();
        propsFile = workDir.resolve("app-creator.properties");
        try (OutputStream out = Files.newOutputStream(propsFile)) {
            props.store(out, "Example AppCreator properties");
        }

        rebuild();
    }

    protected void rebuild() throws Exception {
        final PropertiesHandler<AppCreator> propsHandler = AppCreator.builder()
                .setModelResolver(resolver)
                .setWorkDir(workDir)
                .setAppJar(resolver.resolve(appJar.toAppArtifact()))
                .getPropertiesHandler();
        try (final AppCreator appCreator = PropertiesConfigReader.getInstance(propsHandler).read(propsFile)) {
            testCreator(appCreator);
        }
    }

    protected abstract TsArtifact modelApp() throws Exception;

    protected abstract void testCreator(AppCreator creator) throws Exception;

    private Properties getProperties() {
        final Properties props = new Properties();
        initProps(props);
        return props;
    }

    protected void initProps(Properties props) {
    }
}
