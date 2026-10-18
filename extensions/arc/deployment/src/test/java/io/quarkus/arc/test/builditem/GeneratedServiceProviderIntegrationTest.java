package io.quarkus.arc.test.builditem;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that a {@link GeneratedServiceProviderBuildItem} produced by a build step
 * results in a {@code META-INF/services/} entry that is discoverable via {@link ServiceLoader}.
 */
public class GeneratedServiceProviderIntegrationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(TestService.class, TestServiceImpl.class))
            .addBuildChainCustomizer(b -> b.addBuildStep(new BuildStep() {
                @Override
                public void execute(BuildContext context) {
                    context.produce(new GeneratedServiceProviderBuildItem(
                            TestService.class.getName(),
                            TestServiceImpl.class.getName()));
                }
            }).produces(GeneratedServiceProviderBuildItem.class).build());

    @Test
    void serviceProviderFileIsPresentOnClasspath() {
        String resourcePath = "META-INF/services/" + TestService.class.getName();
        URL resource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        assertThat(resource)
                .as("META-INF/services file for %s must be on the classpath", TestService.class.getName())
                .isNotNull();
    }

    @Test
    void serviceLoaderFindsRegisteredImplementation() {
        List<TestService> services = new ArrayList<>();
        ServiceLoader.load(TestService.class).forEach(services::add);
        assertThat(services)
                .as("ServiceLoader must find TestServiceImpl registered via GeneratedServiceProviderBuildItem")
                .hasAtLeastOneElementOfType(TestServiceImpl.class);
    }

    public interface TestService {
        String hello();
    }

    public static class TestServiceImpl implements TestService {
        @Override
        public String hello() {
            return "hello";
        }
    }
}
