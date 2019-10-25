package io.quarkus.platform.descriptor.resolver.json.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;
import io.quarkus.platform.descriptor.loader.legacy.QuarkusLegacyPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.legacy.QuarkusLegacyPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;

public class JsonDescriptorResolverDemo {

    private static final String IO_QUARKUS = "io.quarkus";

    private static final String LAUNCHER_GROUP_ID = IO_QUARKUS;
    private static final String LAUNCHER_ARTIFACT_ID = "quarkus-tools-launcher";
    private static final String LAUNCHER_POM_PROPS_PATH = "META-INF/maven/" + LAUNCHER_GROUP_ID + "/" + LAUNCHER_ARTIFACT_ID + "/pom.properties";

    public static void main(String... args) throws Exception {

        final DefaultMessageWriter log = new DefaultMessageWriter();
        log.setDebugEnabled(true);

        final String version = determineLauncherVersion(log);
        if(version == null) {
            log.info(errorFailedToDetermineLauncherVersion());
        } else {
            log.info("Tools Launcher Version: %s", version);
        }

        final QuarkusPlatformDescriptor platform = QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setMessageWriter(log)
                .setPlatformJsonArtifactId("quarkus-bom-descriptor-json")
                .resolve();

        log.info("Platform BOM: " + platform.getBomGroupId() + ":" + platform.getBomArtifactId() + ":" + platform.getBomVersion());
        log.info("Extensions total: " + platform.getExtensions().size());
        log.info("Managed deps total: " + platform.getManagedDependencies().size());

        log.info(platform.getTemplate("templates/basic-rest/java/resource-template.ftl"));

        final QuarkusLegacyPlatformDescriptorLoader legacyLoader = new QuarkusLegacyPlatformDescriptorLoader();
        final QuarkusLegacyPlatformDescriptor legacy = legacyLoader.load(new QuarkusPlatformDescriptorLoaderContext() {
            @Override
            public MessageWriter getMessageWriter() {
                return log;
            }});

        log.info("Legacy platform BOM: " + legacy.getBomGroupId() + ":" + legacy.getBomArtifactId() + ":" + legacy.getBomVersion());
        log.info("Extensions total: " + legacy.getExtensions().size());
        log.info("Managed deps total: " + legacy.getManagedDependencies().size());
        log.info(platform.getTemplate("/templates/basic-rest/java/pom.xml-template.ftl"));
    }

    private static String determineLauncherVersion(MessageWriter log) throws IOException {
        final InputStream launcherPomPropsIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(LAUNCHER_POM_PROPS_PATH);
        if(launcherPomPropsIs == null) {
            log.debug("Failed to locate %s on the classpath", LAUNCHER_POM_PROPS_PATH);
            return null;
        }
        try {
            final Properties props = new Properties();
            props.load(launcherPomPropsIs);
            return props.getProperty("version");
        } finally {
            launcherPomPropsIs.close();
        }
    }

    private static String errorFailedToDetermineLauncherVersion() {
        return "Failed to determine the version of Quarkus Tools Launcher";
    }
}
