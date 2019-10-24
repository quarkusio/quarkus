package io.quarkus.platform.descriptor.resolver.json.demo;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public class QuarkusPlatformConfigDemo {

    public static void main(String... args) throws Exception {

        final DefaultMessageWriter log = new DefaultMessageWriter();
        log.setDebugEnabled(true);

        final QuarkusPlatformDescriptor platform = QuarkusPlatformConfig.defaultConfigBuilder()
                .setMessageWriter(log)
                .build()
                .getPlatformDescriptor();

        log.info("Platform BOM: " + platform.getBomGroupId() + ":" + platform.getBomArtifactId() + ":" + platform.getBomVersion());
        log.info("Extensions total: " + platform.getExtensions().size());
        log.info("Managed deps total: " + platform.getManagedDependencies().size());

        log.info(platform.getTemplate("templates/basic-rest/java/resource-template.ftl"));
    }
}
