package io.quarkus.jaxb.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class PackageExcludeTest extends AbstractJaxbContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            io.quarkus.jaxb.deployment.one.Model.class,
                            io.quarkus.jaxb.deployment.two.Model.class)
                    .addPackage("io.quarkus.jaxb.deployment.info"))
            .overrideConfigKey("quarkus.jaxb.exclude-classes", "io.quarkus.jaxb.deployment.two.*");
}
