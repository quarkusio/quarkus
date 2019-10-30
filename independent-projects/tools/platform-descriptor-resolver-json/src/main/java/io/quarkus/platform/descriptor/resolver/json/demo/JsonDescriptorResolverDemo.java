package io.quarkus.platform.descriptor.resolver.json.demo;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.DefaultMessageWriter;

public class JsonDescriptorResolverDemo {

    public static void main(String... args) throws Exception {

        final DefaultMessageWriter log = new DefaultMessageWriter();
        log.setDebugEnabled(true);

        final QuarkusPlatformDescriptor platform = QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setMessageWriter(log)
                .setJsonArtifactId("quarkus-bom-descriptor-json")
                .resolve();

        log.info("Platform BOM: " + platform.getBomGroupId() + ":" + platform.getBomArtifactId() + ":" + platform.getBomVersion());
        log.info("Extensions total: " + platform.getExtensions().size());

        log.info(platform.getTemplate("templates/basic-rest/java/resource-template.ftl"));
    }
}
